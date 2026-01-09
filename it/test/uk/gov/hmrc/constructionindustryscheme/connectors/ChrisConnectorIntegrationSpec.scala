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

package uk.gov.hmrc.constructionindustryscheme.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import play.api.{Application, Mode, inject}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.routing.Router
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, ItResources, WireMockConstants}
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, DEPARTMENTAL_ERROR, FATAL_ERROR, SUBMITTED}
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisPollRequest

import scala.xml.{Elem, XML}

final class ChrisConnectorIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with ApplicationWithWiremock {

  private lazy val connector = app.injector.instanceOf[ChrisConnector]
  private val path = "/submission/ChRIS/CISR/Filing/sync/CIS300MR"

  private lazy val xmlString: String = ItResources.read("chris/envelopes/nil_monthly_return.xml")
  private lazy val envelope: Elem = XML.loadString(xmlString)

  "ChrisConnector.submitEnvelope" should {

    "on 2xx but unparsable XML -> returns FATAL_ERROR(parse) and preserves correlationId" in {
      val correlationId = "cid-123"

      stubFor(
        post(urlPathEqualTo(path))
          .withHeader("Content-Type", equalTo("application/xml"))
          .withHeader("Accept", equalTo("application/xml"))
          .withHeader("CorrelationId", equalTo(correlationId))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody("<Ack>OK</Ack>")
          )
      )

      val result = connector.submitEnvelope(envelope, correlationId).futureValue

      result.status mustBe FATAL_ERROR
      result.rawXml mustBe "<Ack>OK</Ack>"
      result.meta.correlationId mustBe correlationId
      result.meta.error.value.errorNumber mustBe "parse"

      verify(postRequestedFor(urlPathEqualTo(path))
        .withHeader("CorrelationId", equalTo(correlationId))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("Accept", equalTo("application/xml"))
      )
    }

    "on 500 -> returns FATAL_ERROR(http-500) with truncated body (255 chars)" in {
      val correlationId = "cid-500"
      val longBody = "boom" * 100
      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse()
            .withStatus(500)
            .withHeader("Content-Type", "application/xml")
            .withBody(longBody)
          )
      )

      val result = connector.submitEnvelope(envelope, correlationId).futureValue

      result.status mustBe FATAL_ERROR
      result.meta.error.value.errorNumber mustBe "http-500"

      val errText = result.meta.error.value.errorText
      errText.length must be <= 256
      errText must endWith("…")
    }

    "on 404 -> returns FATAL_ERROR(http-404)" in {
      val correlationId = "cid-404"

      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withStatus(404))
      )

      val result = connector.submitEnvelope(envelope, correlationId).futureValue

      result.status mustBe FATAL_ERROR
      result.meta.error.value.errorNumber mustBe "http-404"
    }

    "on connection fault -> returns FATAL_ERROR(conn)" in {
      val correlationId = "cid-conn"

      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
      )

      val result = connector.submitEnvelope(envelope, correlationId).futureValue

      result.status mustBe FATAL_ERROR
      result.meta.error.value.errorNumber mustBe "conn"
      result.rawXml mustBe "<connection-error/>"
    }
  }

  "ChrisConnector.pollSubmission" should {

    "successfully parse acknowledgement response and return ACCEPTED" in {
      val correlationId = "poll-cid-ack"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/endpoint"
      val ackXml = ItResources.read("chris/responses/poll_acknowledgement.xml")

      val expectedRequestXml = ChrisPollRequest(correlationId).paylaod.toString

      stubFor(
        post(urlPathEqualTo("/poll/endpoint"))
          .withHeader("Content-Type", equalTo("application/xml"))
          .withHeader("Accept", equalTo("application/xml"))
          .withHeader("CorrelationId", equalTo(correlationId))
          .withRequestBody(equalToXml(expectedRequestXml))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(ackXml)
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe ACCEPTED
      result.pollUrl mustBe Some("/poll/next-endpoint")
      result.pollInterval mustBe Some(10)

      verify(postRequestedFor(urlPathEqualTo("/poll/endpoint"))
        .withHeader("CorrelationId", equalTo(correlationId))
        .withHeader("Content-Type", equalTo("application/xml"))
        .withHeader("Accept", equalTo("application/xml"))
      )
    }

    "successfully parse response and return SUBMITTED" in {
      val correlationId = "poll-cid-resp"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/response"
      val responseXml = ItResources.read("chris/responses/poll_response.xml")

      stubFor(
        post(urlPathEqualTo("/poll/response"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(responseXml)
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe SUBMITTED
      result.pollUrl mustBe Some("/final/response")
      result.pollInterval mustBe None
    }

    "successfully parse fatal error response and return FATAL_ERROR" in {
      val correlationId = "poll-cid-fatal"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/fatal"
      val errorXml = ItResources.read("chris/responses/poll_fatal_error.xml")

      stubFor(
        post(urlPathEqualTo("/poll/fatal"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(errorXml)
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe FATAL_ERROR
      result.pollUrl mustBe Some("/error/endpoint")
      result.pollInterval mustBe None
    }

    "successfully parse business error response and return DEPARTMENTAL_ERROR" in {
      val correlationId = "poll-cid-biz"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/business"
      val errorXml = ItResources.read("chris/responses/poll_business_error.xml")

      stubFor(
        post(urlPathEqualTo("/poll/business"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(errorXml)
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe DEPARTMENTAL_ERROR
      result.pollUrl mustBe Some("/business/error")
      result.pollInterval mustBe None
    }

    "return FATAL_ERROR when response is unparsable XML" in {
      val correlationId = "poll-cid-parse-err"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/bad"

      stubFor(
        post(urlPathEqualTo("/poll/bad"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody("<Invalid>Unparsable</Invalid>")
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe FATAL_ERROR
      result.pollUrl mustBe None
      result.pollInterval mustBe None
    }

    "return FATAL_ERROR when 500 error is returned" in {
      val correlationId = "poll-cid-500"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/500"

      stubFor(
        post(urlPathEqualTo("/poll/500"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(500)
            .withBody("Internal Server Error")
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe FATAL_ERROR
      result.pollUrl mustBe None
      result.pollInterval mustBe None
    }

    "return FATAL_ERROR when 404 error is returned" in {
      val correlationId = "poll-cid-404"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/404"

      stubFor(
        post(urlPathEqualTo("/poll/404"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(404)
            .withBody("Not Found")
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe FATAL_ERROR
      result.pollUrl mustBe None
      result.pollInterval mustBe None
    }

    "return FATAL_ERROR on connection fault" in {
      val correlationId = "poll-cid-conn"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/conn"

      stubFor(
        post(urlPathEqualTo("/poll/conn"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe FATAL_ERROR
      result.pollUrl mustBe None
      result.pollInterval mustBe None
    }

    "handle response without pollUrl endpoint" in {
      val correlationId = "poll-cid-no-url"
      val pollUrl = s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}/poll/no-url"
      val responseXml = """<GovTalkMessage>
        |  <Header>
        |    <MessageDetails>
        |      <Qualifier>response</Qualifier>
        |      <ResponseEndPoint></ResponseEndPoint>
        |    </MessageDetails>
        |  </Header>
        |</GovTalkMessage>""".stripMargin

      stubFor(
        post(urlPathEqualTo("/poll/no-url"))
          .withRequestBody(equalToXml(ChrisPollRequest(correlationId).paylaod.toString))
          .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(responseXml)
          )
      )

      val result = connector.pollSubmission(correlationId, pollUrl).futureValue

      result.status mustBe SUBMITTED
      result.pollUrl mustBe None
      result.pollInterval mustBe None
    }
  }
}