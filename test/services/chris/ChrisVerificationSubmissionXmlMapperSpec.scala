/*
 * Copyright 2026 HM Revenue & Customs
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
import org.scalatest.OptionValues.convertOptionToValuable
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisVerificationSubmissionXmlMapper

final class ChrisVerificationSubmissionXmlMapperSpec extends AnyFreeSpec with Matchers with EitherValues {

  private def headerXml(
    qualifier: String,
    function: String = "submit",
    clazz: String = "IR-CIS-VERIFY",
    correlationId: String = "ABCDEF123456",
    pollInterval: Option[Int] = Some(15),
    endpointUrl: Option[String] = Some("/poll")
  ): String = {
    val epAttr = pollInterval.map(i => s""" PollInterval="$i"""").getOrElse("")
    val epText = endpointUrl.getOrElse("")
    s"""
       |<Header>
       |  <MessageDetails>
       |    <Qualifier>$qualifier</Qualifier>
       |    <Function>$function</Function>
       |    <Class>$clazz</Class>
       |    <CorrelationID>$correlationId</CorrelationID>
       |    <GatewayTimestamp/>
       |    <ResponseEndPoint$epAttr>$epText</ResponseEndPoint>
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

  "ChrisVerificationSubmissionXmlMapper parse" - {

    "maps an acknowledgement to ACCEPTED, with poll interval and endpoint" in {
      val xml = envelope(
        headerXml(
          qualifier = "acknowledgement",
          pollInterval = Some(15),
          endpointUrl = Some("/poll")
        )
      )

      val res = ChrisVerificationSubmissionXmlMapper.parse(xml).value
      res.status mustBe ACCEPTED
      res.meta.qualifier mustBe "acknowledgement"
      res.meta.function mustBe "submit"
      res.meta.className mustBe "IR-CIS-VERIFY"
      res.meta.correlationId mustBe "ABCDEF123456"
      res.meta.gatewayTimestamp mustBe None
      res.meta.responseEndPoint mustBe ResponseEndPoint("/poll", 15)
      res.meta.error mustBe None
      res.rawXml.trim must include("<GovTalkMessage>")
    }

    "maps case-insensitive acknowledgement to ACCEPTED" in {
      val xml = envelope(
        headerXml(qualifier = "ACKNOWLEDGEMENT")
      )

      val res = ChrisVerificationSubmissionXmlMapper.parse(xml).value
      res.status mustBe ACCEPTED
    }

    "maps GovTalk error Type=fatal to FATAL_ERROR" in {
      val xml =
        """<GovTalkMessage>
          |  <Header>
          |    <MessageDetails>
          |      <Qualifier>error</Qualifier>
          |      <Function>submit</Function>
          |      <Class>IR-CIS-VERIFY</Class>
          |      <CorrelationID>ABCDEF1234567890ABCDEF1234567890</CorrelationID>
          |      <GatewayTimestamp>2025-02-01T12:00:00Z</GatewayTimestamp>
          |      <ResponseEndPoint PollInterval="30">/poll</ResponseEndPoint>
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

      val res = ChrisVerificationSubmissionXmlMapper.parse(xml).value
      res.status mustBe FATAL_ERROR

      val e = res.meta.error.value
      e.errorNumber mustBe "9001"
      e.errorType mustBe "systemError"
      e.errorText.toLowerCase must include("catastrophic")
    }

    "maps error number 3000 with fatal type to FATAL_ERROR" in {
      val xml =
        """<GovTalkMessage>
          |  <Header>
          |    <MessageDetails>
          |      <Qualifier>error</Qualifier>
          |      <Function>submit</Function>
          |      <Class>IR-CIS-VERIFY</Class>
          |      <CorrelationID>ABC123</CorrelationID>
          |      <ResponseEndPoint PollInterval="20">/poll</ResponseEndPoint>
          |    </MessageDetails>
          |  </Header>
          |  <GovTalkDetails>
          |    <GovTalkErrors>
          |      <Error>
          |        <Number>3000</Number>
          |        <Type>FaTaL</Type>
          |        <Text>Fatal submission error</Text>
          |      </Error>
          |    </GovTalkErrors>
          |  </GovTalkDetails>
          |</GovTalkMessage>
          |""".stripMargin

      val res = ChrisVerificationSubmissionXmlMapper.parse(xml).value
      res.status mustBe FATAL_ERROR

      val err = res.meta.error.value
      err.errorNumber mustBe "3001"
      err.errorType mustBe "departmentalError"
    }

    "maps unknown qualifier to FATAL_ERROR" in {
      val xml = envelope(
        headerXml(qualifier = "unknown")
      )

      val res = ChrisVerificationSubmissionXmlMapper.parse(xml).value
      res.status mustBe FATAL_ERROR
      res.meta.error mustBe None
    }

    "returns Left when mandatory fields are missing" - {
      "missing Qualifier" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Function>submit</Function>
            |      <Class>IR-CIS-VERIFY</Class>
            |      <CorrelationID>ABC</CorrelationID>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisVerificationSubmissionXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: Qualifier")
      }

      "missing CorrelationID" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>acknowledgement</Qualifier>
            |      <Function>submit</Function>
            |      <Class>IR-CIS-VERIFY</Class>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisVerificationSubmissionXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: CorrelationID")
      }

      "missing Function" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>acknowledgement</Qualifier>
            |      <Class>IR-CIS-VERIFY</Class>
            |      <CorrelationID>ABC</CorrelationID>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisVerificationSubmissionXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: Function")
      }

      "missing Class" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>acknowledgement</Qualifier>
            |      <Function>submit</Function>
            |      <CorrelationID>ABC</CorrelationID>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisVerificationSubmissionXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: Class")
      }
    }
  }
}
