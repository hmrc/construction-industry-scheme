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
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisPollXmlMapper

import java.time.Instant

final class ChrisPollXmlMapperSpec extends AnyFreeSpec with Matchers with EitherValues {

  private val corrId    = "CORR-123"
  private val gatewayTs = "2025-01-01T00:00:00"

  private def headerXml(
    qualifier: String,
    correlationId: String = corrId,
    gatewayTimestamp: Option[String] = Some(gatewayTs),
    endpointUrl: Option[String] = None,
    pollInterval: Option[Int] = None
  ): String = {
    val epText           = endpointUrl.getOrElse("")
    val pollIntervalAttr = pollInterval.map(pi => s""" PollInterval="$pi"""").getOrElse("")
    val gatewayTsXml     = gatewayTimestamp.map(ts => s"<GatewayTimestamp>$ts</GatewayTimestamp>").getOrElse("")

    s"""
       |<Header>
       |  <MessageDetails>
       |    <Qualifier>$qualifier</Qualifier>
       |    <CorrelationID>$correlationId</CorrelationID>
       |    $gatewayTsXml
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

  "ChrisPollXmlMapper parse" - {

    "maps an acknowledgement to ACCEPTED" - {
      "with endpoint URL and poll interval" in {
        val xml = envelope(
          headerXml(
            qualifier = "acknowledgement",
            endpointUrl = Some("/poll/next"),
            pollInterval = Some(10)
          )
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/poll/next")
        res.pollInterval mustBe Some(10)
        res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
      }

      "with endpoint URL but no poll interval" in {
        val xml = envelope(
          headerXml(
            qualifier = "acknowledgement",
            endpointUrl = Some("/poll/next")
          )
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/poll/next")
        res.pollInterval mustBe None
        res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
      }

      "without endpoint URL" in {
        val xml = envelope(
          headerXml(qualifier = "acknowledgement")
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.correlationId mustBe corrId
        res.pollUrl mustBe None
        res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
      }

      "case insensitive matching" in {
        val xml = envelope(
          headerXml(
            qualifier = "ACKNOWLEDGEMENT",
            endpointUrl = Some("/poll")
          )
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe ACCEPTED
        res.correlationId mustBe corrId
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

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe SUBMITTED
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/response/endpoint")
        res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
      }

      "without endpoint URL" in {
        val xml = envelope(
          headerXml(qualifier = "response")
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe SUBMITTED
        res.correlationId mustBe corrId
        res.pollUrl mustBe None
      }

      "case insensitive matching" in {
        val xml = envelope(
          headerXml(qualifier = "RESPONSE")
        )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe SUBMITTED
        res.correlationId mustBe corrId
      }
    }

    "maps error qualifier to error statuses" - {
      "fatal error type with non-special number maps to to FATAL_ERROR" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
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

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe FATAL_ERROR
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/error/endpoint")
      }

      "3001 with business error type to DEPARTMENTAL_ERROR" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
             |      <ResponseEndPoint>/business/error</ResponseEndPoint>
             |    </MessageDetails>
             |  </Header>
             |  <GovTalkDetails>
             |    <GovTalkErrors>
             |      <Error>
             |        <Number>3001</Number>
             |        <Type>business</Type>
             |        <Text>Invalid data supplied</Text>
             |      </Error>
             |    </GovTalkErrors>
             |  </GovTalkDetails>
             |</GovTalkMessage>
             |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe DEPARTMENTAL_ERROR
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/business/error")
      }

      "3001 with business type is case insensitive error type matching" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>ERROR</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
             |    </MessageDetails>
             |  </Header>
             |  <GovTalkDetails>
             |    <GovTalkErrors>
             |      <Error>
             |        <Number>3001</Number>
             |        <Type>BUSINESS</Type>
             |        <Text>Error text</Text>
             |      </Error>
             |    </GovTalkErrors>
             |  </GovTalkDetails>
             |</GovTalkMessage>
             |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe DEPARTMENTAL_ERROR
      }

      "business error type & (body error Type=business & body error number=2021) to SUBMITTED_NO_RECEIPT" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
             |      <ResponseEndPoint>/business/error</ResponseEndPoint>
             |    </MessageDetails>
             |  </Header>
             |  <GovTalkDetails>
             |    <GovTalkErrors>
             |      <Error>
             |        <Number>3001</Number>
             |        <Type>business</Type>
             |        <Text>Your submission failed due to business validation errors. Please see below for details.</Text>
             |      </Error>
             |    </GovTalkErrors>
             |  </GovTalkDetails>
             |   <Body>
             |     <ErrorResponse SchemaVersion="2.0">
             |       <Application>
             |         <MessageCount>1</MessageCount>
             |       </Application>
             |       <Error>
             |         <RaisedBy>ChRIS</RaisedBy>
             |         <Number>2021</Number>
             |         <Type>business</Type>
             |         <Text>The supplied IRmark is incorrect.</Text>
             |         <Location>IRmark</Location>
             |       </Error>
             |     </ErrorResponse>
             |   </Body>
             |</GovTalkMessage>
             |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe SUBMITTED_NO_RECEIPT
        res.correlationId mustBe corrId
        res.pollUrl mustBe Some("/business/error")
      }

      "unknown error type defaults to FATAL_ERROR" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
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

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe FATAL_ERROR
      }

      "error number 3000 with fatal type maps to STARTED" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
             |      <ResponseEndPoint>/fatal/3000</ResponseEndPoint>
             |    </MessageDetails>
             |  </Header>
             |  <GovTalkDetails>
             |    <GovTalkErrors>
             |      <Error>
             |        <Number>3000</Number>
             |        <Type>fatal</Type>
             |        <Text>Fatal processing error</Text>
             |      </Error>
             |    </GovTalkErrors>
             |  </GovTalkDetails>
             |</GovTalkMessage>
             |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe STARTED
        res.pollUrl mustBe Some("/fatal/3000")
      }

      "2005 maps to STARTED" in {
        val xml =
          envelope(
            s"""
               |${headerXml(
                qualifier = "error",
                endpointUrl = Some("/recoverable/2005")
              )}
               |<GovTalkDetails>
               |  <GovTalkErrors>
               |    <Error>
               |      <Number>2005</Number>
               |      <Type>fatal</Type>
               |      <Text>2005 recoverable error</Text>
               |    </Error>
               |  </GovTalkErrors>
               |</GovTalkDetails>
               |""".stripMargin
          )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe STARTED
        res.pollUrl mustBe Some("/recoverable/2005")
      }

      "1000 maps to STARTED" in {
        val xml =
          envelope(
            s"""
               |${headerXml(
                qualifier = "error",
                endpointUrl = Some("/recoverable/1000")
              )}
               |<GovTalkDetails>
               |  <GovTalkErrors>
               |    <Error>
               |      <Number>1000</Number>
               |      <Type>fatal</Type>
               |      <Text>1000 recoverable error</Text>
               |    </Error>
               |  </GovTalkErrors>
               |</GovTalkDetails>
               |""".stripMargin
          )

        val res = ChrisPollXmlMapper.parse(xml).value
        res.status mustBe STARTED
        res.pollUrl mustBe Some("/recoverable/1000")
      }
    }

    "maps unknown qualifier to FATAL_ERROR" in {
      val xml = envelope(
        headerXml(
          qualifier = "unknown",
          endpointUrl = Some("/unknown")
        )
      )

      val res = ChrisPollXmlMapper.parse(xml).value
      res.status mustBe FATAL_ERROR
      res.correlationId mustBe corrId
      res.pollUrl mustBe Some("/unknown")
    }

    "returns Left when mandatory fields are missing" - {
      "missing Qualifier field" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <CorrelationID>$corrId</CorrelationID>
             |      <ResponseEndPoint>/poll</ResponseEndPoint>
             |    </MessageDetails>
             |  </Header>
             |</GovTalkMessage>
             |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: Qualifier")
      }

      "missing CorrelationID field" in {
        val xml =
          """<GovTalkMessage>
            |  <Header>
            |    <MessageDetails>
            |      <Qualifier>response</Qualifier>
            |      <ResponseEndPoint>/poll</ResponseEndPoint>
            |    </MessageDetails>
            |  </Header>
            |</GovTalkMessage>
            |""".stripMargin

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field: CorrelationID")
      }

      "error qualifier missing error details" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
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

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("Missing mandatory field")
      }

      "error qualifier missing error number" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
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

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Number")
      }

      "error qualifier missing error type" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
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

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Type")
      }

      "error qualifier missing error text" in {
        val xml =
          s"""<GovTalkMessage>
             |  <Header>
             |    <MessageDetails>
             |      <Qualifier>error</Qualifier>
             |      <CorrelationID>$corrId</CorrelationID>
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

        val res = ChrisPollXmlMapper.parse(xml)
        res.isLeft mustBe true
        res.left.value must include("GovTalkErrors/Error/Text")
      }
    }

    "handles whitespace in XML elements" in {
      val xml =
        s"""<GovTalkMessage>
           |  <Header>
           |    <MessageDetails>
           |      <Qualifier>  acknowledgement  </Qualifier>
           |      <CorrelationID>  $corrId  </CorrelationID>
           |      <GatewayTimestamp>  $gatewayTs  </GatewayTimestamp>
           |      <ResponseEndPoint>  /poll/endpoint  </ResponseEndPoint>
           |    </MessageDetails>
           |  </Header>
           |</GovTalkMessage>
           |""".stripMargin

      val res = ChrisPollXmlMapper.parse(xml).value
      res.status mustBe ACCEPTED
      res.correlationId mustBe corrId
      res.pollUrl mustBe Some("/poll/endpoint")
      res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
    }

    "handles empty ResponseEndPoint element" in {
      val xml =
        s"""<GovTalkMessage>
           |  <Header>
           |    <MessageDetails>
           |      <Qualifier>response</Qualifier>
           |      <CorrelationID>$corrId</CorrelationID>
           |      <GatewayTimestamp>$gatewayTs</GatewayTimestamp>
           |      <ResponseEndPoint></ResponseEndPoint>
           |    </MessageDetails>
           |  </Header>
           |</GovTalkMessage>
           |""".stripMargin

      val res = ChrisPollXmlMapper.parse(xml).value
      res.status mustBe SUBMITTED
      res.correlationId mustBe corrId
      res.pollUrl mustBe None
    }

    "uses current time when GatewayTimestamp is missing" in {
      val fixedNow = Instant.parse("2026-03-23T12:00:00Z")

      val xml = envelope(
        headerXml(
          qualifier = "response",
          gatewayTimestamp = None,
          endpointUrl = Some("/poll")
        )
      )

      val res = ChrisPollXmlMapper.parse(xml, fixedNow).value
      res.status mustBe SUBMITTED
      res.correlationId mustBe corrId
      res.pollUrl mustBe Some("/poll")
      res.lastMessageDate mustBe Some("2026-03-23T12:00:00Z")
    }

    "normalises GatewayTimestamp when it is a valid LocalDateTime without zone" in {
      val xml = envelope(
        headerXml(
          qualifier = "response",
          gatewayTimestamp = Some("2025-01-01T00:00:00"),
          endpointUrl = Some("/poll")
        )
      )

      val res = ChrisPollXmlMapper.parse(xml).value
      res.status mustBe SUBMITTED
      res.correlationId mustBe corrId
      res.pollUrl mustBe Some("/poll")
      res.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
    }

    "returns Left when GatewayTimestamp cannot be parsed" in {
      val xml = envelope(
        headerXml(
          qualifier = "response",
          gatewayTimestamp = Some("not-a-timestamp"),
          endpointUrl = Some("/poll")
        )
      )

      val res = ChrisPollXmlMapper.parse(xml)
      res.isLeft mustBe true
      res.left.value must include("Failed to parse GatewayTimestamp 'not-a-timestamp'")
    }
  }
}
