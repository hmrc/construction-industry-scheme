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
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, ItResources}
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
      val longBody = ("boom" * 100)
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
}
