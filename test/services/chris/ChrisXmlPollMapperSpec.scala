/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services.chris

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.EitherValues
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisXmlPollMapper

final class ChrisXmlPollMapperSpec extends AnyFreeSpec with Matchers with EitherValues {

  private def headerXml(
    qualifier: String,
    endpointUrl: Option[String] = None,
    pollInterval: Option[Int] = None
  ): String = {
    val epText = endpointUrl.getOrElse("")
    val pollIntervalAttr = pollInterval.map(pi => s""" PollInterval="$pi"""").getOrElse("")
    s"""
       |<Header>
       |  <MessageDetails>
       |    <Qualifier>$qualifier</Qualifier>
       |    <ResponseEndPoint$pollIntervalAttr>$epText</ResponseEndPoint>
       |  </MessageDetails>
       |</Header>
       |""".stripMargin
  }

  private def envelope(bodyInsideGovTalkMessage: String): String =
    s"""
       |<GovTalkMessage>
       |  $bodyInsideGovTalkMessage
       |</GovTalkMessage>
       |""".stripMargin

  "ChrisXmlPollMapper parse" - {

    "maps an acknowledgement to ACCEPTED" - {
      "with endpoint URL and poll interval" in {
        val xml = envelope(
          headerXml(
            qualifier = "acknowledgement",
            endpointUrl = Some("/poll/next"),
            pollInterval = Some(10)
          )
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.pollUrl mustBe Some("/poll/next")
        res.pollInterval mustBe Some(10)
      }

      "with endpoint URL but no poll interval" in {
        val xml = envelope(
          headerXml(
            qualifier = "acknowledgement",
            endpointUrl = Some("/poll/next")
          )
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.pollUrl mustBe Some("/poll/next")
        res.pollInterval mustBe None
      }

      "without endpoint URL" in {
        val xml = envelope(
          headerXml(qualifier = "acknowledgement")
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.pollUrl mustBe None
      }

      "case insensitive matching" in {
        val xml = envelope(
          headerXml(
            qualifier = "ACKNOWLEDGEMENT",
            endpointUrl = Some("/poll")
          )
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.pollUrl mustBe Some("/poll")
      }
    }

    "maps a response to SUBMITTED" - {
      "with endpoint URL" in {
        val xml = envelope(
          headerXml(
            qualifier = "response",
            endpointUrl = Some("/response/endpoint")
          )
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe SUBMITTED
        res.pollUrl mustBe Some("/response/endpoint")
      }

      "without endpoint URL" in {
        val xml = envelope(
          headerXml(qualifier = "response")
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe SUBMITTED
        res.pollUrl mustBe None
      }

      "case insensitive matching" in {
        val xml = envelope(
          headerXml(qualifier = "RESPONSE")
        )

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe SUBMITTED
      }
    }

    "maps error qualifier to error statuses" - {
      "fatal error type to FATAL_ERROR" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |      <ResponseEndPoint>/error/endpoint</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>9001</Number>
            |        <Type>fatal</Type>
            |        <Text>Catastrophic failure</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe FATAL_ERROR
        res.pollUrl mustBe Some("/error/endpoint")
      }

      "business error type to DEPARTMENTAL_ERROR" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |      <ResponseEndPoint>/business/error</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>4001</Number>
            |        <Type>business</Type>
            |        <Text>Invalid data supplied</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe DEPARTMENTAL_ERROR
        res.pollUrl mustBe Some("/business/error")
      }

      "case insensitive error type matching" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>ERROR</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>4001</Number>
            |        <Type>BUSINESS</Type>
            |        <Text>Error text</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe DEPARTMENTAL_ERROR
      }

      "unknown error type defaults to FATAL_ERROR" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>5001</Number>
            |        <Type>unknown</Type>
            |        <Text>Unknown error type</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml).value
        res.status mustBe FATAL_ERROR
      }
    }

    "maps unknown qualifier to FATAL_ERROR" in {
      val xml = envelope(
        headerXml(
          qualifier = "unknown",
          endpointUrl = Some("/unknown")
        )
      )

      val res = ChrisXmlPollMapper.parse(xml).value
      res.status mustBe FATAL_ERROR
      res.pollUrl mustBe Some("/unknown")
    }

    "returns Left when mandatory fields are missing" - {
      "missing Qualifier field" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: Qualifier")
      }

      "error qualifier missing error details" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field")
      }

      "error qualifier missing error number" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Type>fatal</Type>
            |        <Text>Error text</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Number")
      }

      "error qualifier missing error type" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>9001</Number>
            |        <Text>Error text</Text>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Type")
      }

      "error qualifier missing error text" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>error</Qualifier>
            |    </MessageDetails>
            |  </Header>
            |  <GovTalkDetails>
            |    <GovTalkErrors>
            |      <Error>
            |        <Number>9001</Number>
            |        <Type>fatal</Type>
            |      </Error>
            |    </GovTalkErrors>
            |  </GovTalkDetails>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisXmlPollMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Text")
      }
    }

    "handles whitespace in XML elements" in {
      val xml =
        """<GovTalkMessage>
          |  <Header>
          |    <MessageDetails>
          |      <Qualifier>  acknowledgement  </Qualifier>
          |      <ResponseEndPoint>  /poll/endpoint  </ResponseEndPoint>
          |    </MessageDetails>
          |  </Header>
          |</GovTalkMessage>
          |""".stripMargin

      val res = ChrisXmlPollMapper.parse(xml).value
      res.status mustBe ACCEPTED
      res.pollUrl mustBe Some("/poll/endpoint")
    }

    "handles empty ResponseEndPoint element" in {
      val xml =
        """<GovTalkMessage>
          |  <Header>
          |    <MessageDetails>
          |      <Qualifier>response</Qualifier>
          |      <ResponseEndPoint></ResponseEndPoint>
          |    </MessageDetails>
          |  </Header>
          |</GovTalkMessage>
          |""".stripMargin

      val res = ChrisXmlPollMapper.parse(xml).value
      res.status mustBe SUBMITTED
      res.pollUrl mustBe None
    }
  }
}