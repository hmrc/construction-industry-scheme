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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, get, stubFor, urlPathEqualTo}
import itutil.ApplicationWithWiremock
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.constructionindustryscheme.connectors.MonthlyReturnConnector
import uk.gov.hmrc.constructionindustryscheme.models.responses.{RDSDatacacheResponse, RDSMonthlyReturnDetails}

class MonthlyReturnConnectorSpec extends ApplicationWithWiremock
  with Matchers
  with ScalaFutures
  with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val connector: MonthlyReturnConnector = app.injector.instanceOf[MonthlyReturnConnector]

  val testEmptyDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnCount = 0, monthlyReturnList = Seq.empty)
  val testDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnCount = 2,
    monthlyReturnList = Seq(
      RDSMonthlyReturnDetails(monthlyReturnId = "testRef1", taxYear = 2025, taxMonth = 1),
      RDSMonthlyReturnDetails(monthlyReturnId = "testRef2", taxYear = 2025, taxMonth = 7)
    ))

  "MonthlyReturnConnector" should {
    "retrieveMonthlyReturns" should {
      "successfully retrieve monthly returns" in {
        stubFor(
          get(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withQueryParam("maxRecords", equalTo("2"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(testDataCacheResponse).toString)
            )
        )

        val result = connector.retrieveMonthlyReturns(2).futureValue

        result mustBe testDataCacheResponse
      }

      "successfully retrieve empty direct debits" in {
        stubFor(
          get(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withQueryParam("maxRecords", equalTo("0"))
            .willReturn(
              aResponse()
                .withStatus(OK)
                .withBody(Json.toJson(testEmptyDataCacheResponse).toString)
            )
        )

        val result = connector.retrieveMonthlyReturns(0).futureValue

        result mustBe testEmptyDataCacheResponse
      }

      "must fail when the result is parsed as an UpstreamErrorResponse" in {
        stubFor(
          get(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withQueryParam("maxRecords", equalTo("2"))
            .willReturn(
              aResponse()
                .withStatus(INTERNAL_SERVER_ERROR)
                .withBody("test error")
            )
        )

        val result = intercept[Exception](connector.retrieveMonthlyReturns(2).futureValue)

        result.getMessage must include("returned 500. Response body: 'test error'")
      }

      "must fail when the result is a failed future" in {
        stubFor(
          get(urlPathEqualTo("/rds-datacache-proxy/monthly-returns"))
            .withQueryParam("maxRecords", equalTo("2"))
            .willReturn(
              aResponse()
                .withStatus(0)
            )
        )

        val result = intercept[Exception](connector.retrieveMonthlyReturns(2).futureValue)

        result.getMessage must include("The future returned an exception")
      }
    }
  }
}

