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

package uk.gov.hmrc.constructionindustryscheme.services

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.models.UserMonthlyReturns
import uk.gov.hmrc.http.HeaderCarrier

class MonthlyReturnServiceSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val service = app.injector.instanceOf[MonthlyReturnService]

  private val er = EmployerReference("111", "test111")
  private val erJson = Json.toJson(er)
  private val instanceId = "abc-123"

  private val instanceIdResponseJson = Json.obj("instanceId" -> instanceId)   
  private val instanceIdRequestJson  = Json.obj("instanceId" -> instanceId)   

  "MonthlyReturnService#retrieveMonthlyReturns" should {

    "call SP1 (datacache) then SP2 (formp) and return wrapper (happy path)" in {
      val returnsJson = Json.parse(
        """{
          |  "monthlyReturnList": [
          |    { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
          |    { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
          |  ]
          |}""".stripMargin) 

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(instanceIdResponseJson.toString()))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceIdRequestJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(returnsJson.toString()))
      )

      val out = service.retrieveMonthlyReturns(er).futureValue
      Json.toJson(out) mustBe returnsJson
    }

    "return empty wrapper when SP2 returns empty list" in {
      val emptyJson = Json.parse("""{ "monthlyReturnList": [] }""")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(instanceIdResponseJson.toString()))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceIdRequestJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(emptyJson.toString()))
      )

      val out = service.retrieveMonthlyReturns(er).futureValue
      Json.toJson(out) mustBe emptyJson
    }

    "fail when SP1 (datacache) fails" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("rds datacache error"))
      )

      val ex = intercept[Throwable](service.retrieveMonthlyReturns(er).futureValue)
      ex.getMessage must include ("500")
    }

    "fail when SP2 (formp) fails" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(instanceIdResponseJson.toString()))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceIdRequestJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = intercept[Throwable](service.retrieveMonthlyReturns(er).futureValue)
      ex.getMessage must include ("500")
    }
  }
}
