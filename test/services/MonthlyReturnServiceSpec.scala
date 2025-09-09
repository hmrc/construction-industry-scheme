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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.constructionindustryscheme.connectors.MonthlyReturnConnector
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.models.responses.{RDSDatacacheResponse, RDSMonthlyReturnDetails}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class MonthlyReturnServiceSpec extends SpecBase {

  val mockConnector: MonthlyReturnConnector = mock[MonthlyReturnConnector]
  val testService: MonthlyReturnService = new MonthlyReturnService(connector = mockConnector)
  val testDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnList = Seq(
      RDSMonthlyReturnDetails(monthlyReturnId = 66666L, taxYear = 2025, taxMonth = 1),
      RDSMonthlyReturnDetails(monthlyReturnId = 66667L, taxYear = 2025, taxMonth = 7)
    ))

  "MonthlyReturnService" - {
    "retrieveMonthlyReturns method" - {
      "must return the response from the connector" in {
        val er = EmployerReference("123", "AB456")

        when(mockConnector.retrieveMonthlyReturns(
          any[EmployerReference]
        )(any[HeaderCarrier])
        ).thenReturn(Future.successful(testDataCacheResponse))

        val result = testService.retrieveMonthlyReturns(er).futureValue

        result mustBe testDataCacheResponse
      }
    }
  }

  "propagates failures from the connector" in {
    val boom = new RuntimeException("rds-datacache-proxy call failed")
    val er = EmployerReference("123", "AB456")

    when(
      mockConnector.retrieveMonthlyReturns(
        any[EmployerReference]
      )(any[HeaderCarrier])
    ).thenReturn(Future.failed(boom))

    val ex = testService.retrieveMonthlyReturns(er).failed.futureValue
    ex mustBe a [RuntimeException]
    ex.getMessage mustBe "rds-datacache-proxy call failed"
  }
}
