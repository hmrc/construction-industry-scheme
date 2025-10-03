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
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, ItResources}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.xml.{Elem, XML}

class ChrisConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private lazy val connector = app.injector.instanceOf[ChrisConnector]
  private val path      = "/submission/ChRIS/CISR/Filing/sync/CIS300MR"

  private lazy val xmlString: String = ItResources.read("chris/envelopes/nil_monthly_return.xml")
  private lazy val envelope: Elem = XML.loadString(xmlString)

  "ChrisConnector.submitEnvelope" should {

    "POST XML to /submission/... and return HttpResponse on 200" in {
      stubFor(
        post(urlPathEqualTo(path))
          .withHeader("Content-Type", equalTo("application/xml"))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/xml")
              .withBody("<Ack>OK</Ack>")
          )
      )

      val result: HttpResponse = connector.submitEnvelope(envelope).futureValue
      result.status mustBe 200
      result.body   mustBe "<Ack>OK</Ack>"

      verify(
        postRequestedFor(urlPathEqualTo(path))
          .withHeader("Content-Type", equalTo("application/xml"))
      )
    }

    "fail the future with UpstreamErrorResponse on 500" in {
      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withStatus(500).withBody("boom"))
      )

      val response = connector.submitEnvelope(envelope).futureValue
      response.status mustBe 500
      response.body must include("boom")
    }

    "fail the future with UpstreamErrorResponse on 404" in {
      stubFor(
        post(urlPathEqualTo(path))
          .withRequestBody(equalToXml(xmlString))
          .willReturn(aResponse().withStatus(404))
      )

      val response = connector.submitEnvelope(envelope).futureValue
      response.status mustBe 404
    }
  }
}
