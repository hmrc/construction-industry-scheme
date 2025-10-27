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
import uk.gov.hmrc.constructionindustryscheme.models.FATAL_ERROR

import scala.xml.{Elem, XML}

final class ChrisConnectorIntegrationSpec
  extends Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with ApplicationWithWiremock {

  private lazy val connector = app.injector.instanceOf[ChrisConnector]
  private val path = "/submission/ChRIS/CISR/Filing/sync/CIS300MR"
  private val overridePath = "/non2xx-override-path"

  private lazy val xmlString: String = ItResources.read("chris/envelopes/nil_monthly_return.xml")
  private lazy val envelope: Elem = XML.loadString(xmlString)

  private def buildAppWith(extra: (String, Any)*): Application =
    new GuiceApplicationBuilder()
      .overrides(inject.bind[Router].toInstance(Router.empty))
      .in(Mode.Test)
      .configure(
        "microservice.services.chris.host" -> WireMockConstants.stubHost,
        "microservice.services.chris.port" -> WireMockConstants.stubPort,
        "microservice.services.chris.affix-url" -> path,
        "play.http.router" -> null
      )
      .configure(extra: _*)
      .build()

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

  "ChrisConnector non-2xx feature switch" should {

    "DISABLED: ignore override-url and hit base chris path" in {
      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withStatus(200).withBody("<Ack>OK</Ack>"))
      )
      stubFor(
        post(urlPathEqualTo(overridePath))
          .willReturn(aResponse().withStatus(404))
      )

      val app = buildAppWith(
        "chrisTest.enable-non-2xx" -> false,
        "chrisTest.non2xx.override-url" -> s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}$overridePath"
      )
      try {
        val connector2 = app.injector.instanceOf[ChrisConnector]
        val res = connector2.submitEnvelope(envelope, "cid-switch-off").futureValue

        res.status mustBe FATAL_ERROR
        res.meta.error.value.errorNumber mustBe "parse"
        verify(1, postRequestedFor(urlPathEqualTo(path)))
        verify(0, postRequestedFor(urlPathEqualTo(overridePath)))
      } finally {
        app.stop()
      }
    }

    "ENABLED with override pointing to WireMock (404): use override-url (not base)" in {
      stubFor(
        post(urlPathEqualTo(overridePath))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withStatus(404))
      )
      stubFor(
        post(urlPathEqualTo(path))
          .willReturn(aResponse().withStatus(200).withBody("<ShouldNotBeSeen/>"))
      )

      val app = buildAppWith(
        "chrisTest.enable-non-2xx" -> true,
        "chrisTest.non2xx.override-url" -> s"http://${WireMockConstants.stubHost}:${WireMockConstants.stubPort}$overridePath"
      )
      try {
        val connector2 = app.injector.instanceOf[ChrisConnector]
        val res = connector2.submitEnvelope(envelope, "cid-override-404").futureValue

        res.status mustBe FATAL_ERROR
        res.meta.error.value.errorNumber mustBe "http-404"
        verify(1, postRequestedFor(urlPathEqualTo(overridePath)))
        verify(0, postRequestedFor(urlPathEqualTo(path)))
      } finally {
        app.stop()
      }
    }

    "ENABLED with override to invalid host: map transport failure to conn" in {
      val app = buildAppWith(
        "chrisTest.enable-non-2xx" -> true,
        "chrisTest.non2xx.override-url" -> "https://definitely-not-a-real-host.invalid/cis-return"
      )
      try {
        val connector2 = app.injector.instanceOf[ChrisConnector]
        val res = connector2.submitEnvelope(envelope, "cid-override-dns").futureValue

        res.status mustBe FATAL_ERROR
        res.meta.error.value.errorNumber mustBe "conn"
        res.rawXml mustBe "<connection-error/>"
      } finally {
        app.stop()
      }
    }
  }
}