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
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.UserMonthlyReturns
import uk.gov.hmrc.http.HeaderCarrier

class FormpProxyConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = app.injector.instanceOf[FormpProxyConnector]

  private val instanceId = "abc-123"
  private val instanceReqJson = Json.obj("instanceId" -> instanceId)

  "FormpProxyConnector getMonthlyReturns" should {

    "POST instanceId to /formp-proxy/monthly-returns and return wrapper (200)" in {
      val responseJson = Json.parse(
        """{
          |  "monthlyReturnList": [
          |    { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
          |    { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
          |  ]
          |}""".stripMargin) 

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "return empty wrapper when upstream returns an empty list" in {
      val responseJson = Json.parse("""{ "monthlyReturnList": [] }""")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(responseJson.toString()))
      )

      val out = connector.getMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = intercept[Throwable](connector.getMonthlyReturns(instanceId).futureValue)
      ex.getMessage must include ("500")
    }
  }
}