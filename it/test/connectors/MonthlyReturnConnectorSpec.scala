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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, stubFor, urlPathEqualTo}
import itutil.ApplicationWithWiremock
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.constructionindustryscheme.connectors.MonthlyReturnConnector
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.models.responses.{RDSDatacacheResponse, RDSMonthlyReturnDetails}

class MonthlyReturnConnectorSpec extends ApplicationWithWiremock
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: MonthlyReturnConnector = app.injector.instanceOf[MonthlyReturnConnector]

  val testEmptyDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnList = Seq.empty)
  val testDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(
    monthlyReturnList = Seq(
      RDSMonthlyReturnDetails(monthlyReturnId = 66666L, taxYear = 2025, taxMonth = 1),
      RDSMonthlyReturnDetails(monthlyReturnId = 66667L, taxYear = 2025, taxMonth = 7)
    ))

  val er: EmployerReference = EmployerReference("111", "test111")

  "MonthlyReturnConnector" should {
    "retrieveMonthlyReturns" should {
      "successfully retrieve monthly returns" in {
        stubFor(
          post(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(Json.toJson(er).toString(), true, true))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(testDataCacheResponse).toString)
            )
        )

        val result = connector.retrieveMonthlyReturns(er).futureValue

        result mustBe testDataCacheResponse
      }

      "successfully retrieve empty monthly return" in {
        stubFor(
          post(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(Json.toJson(er).toString(), true, true))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(testEmptyDataCacheResponse).toString)
            )
        )

        val result = connector.retrieveMonthlyReturns(er).futureValue

        result mustBe testEmptyDataCacheResponse
      }

      "must fail when the result is parsed as an UpstreamErrorResponse" in {
        stubFor(
          post(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(Json.toJson(er).toString(), true, true))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withBody("test error")
            )
        )

        val result = intercept[Exception](connector.retrieveMonthlyReturns(er).futureValue)

        result.getMessage must include("returned 500. Response body: 'test error'")
      }

      "must fail when the result is a failed future" in {
        stubFor(
          post(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withHeader("Content-Type", equalTo("application/json"))
            .withRequestBody(equalToJson(Json.toJson(er).toString(), true, true))
            .willReturn(
              aResponse()
                .withStatus(0)
            )
        )

        val result = intercept[Exception](connector.retrieveMonthlyReturns(er).futureValue)

        result.getMessage must include("The future returned an exception")
      }
    }
  }
}

