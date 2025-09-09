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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.MonthlyReturnsController
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.models.responses.{RDSDatacacheResponse, RDSMonthlyReturnDetails}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
class MonthlyReturnsControllerSpec extends SpecBase {

  "MonthlyReturnsController" - {
    "retrieveDirectDebits method" - {
      "return 200 and a successful response when the max number of records is supplied" in new SetUp {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(
          any[EmployerReference]
        )(any[HeaderCarrier])
        ).thenReturn(Future.successful(testDataCacheResponse))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testDataCacheResponse)
      }

      "return 200 and a successful response with 0 when the no value of max records is supplied" in new SetUp {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(
          any[EmployerReference]
        )(any[HeaderCarrier])
        ).thenReturn(Future.successful(testEmptyDataCacheResponse))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(testEmptyDataCacheResponse)
      }
    }
  }

  class SetUp {
    val mockMonthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]

    val testEmptyDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnList = Seq.empty)
    val testDataCacheResponse: RDSDatacacheResponse = RDSDatacacheResponse(monthlyReturnList = Seq(
        RDSMonthlyReturnDetails(monthlyReturnId = 66666L, taxYear = 2025, taxMonth = 1),
        RDSMonthlyReturnDetails(monthlyReturnId = 66667L, taxYear = 2025, taxMonth = 7)
      ))

    val controller = new MonthlyReturnsController(fakeAuthAction(), mockMonthlyReturnService, cc)
  }
}
