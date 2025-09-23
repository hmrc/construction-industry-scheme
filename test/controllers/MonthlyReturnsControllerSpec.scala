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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any,eq  => eqTo}
import org.mockito.Mockito.*
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.MonthlyReturnsController
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, MonthlyReturn, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}
class MonthlyReturnsControllerSpec extends SpecBase {

  "MonthlyReturnsController" - {

    "GET /cis/monthly-returns (retrieveMonthlyReturns)" - {

      "return 200 with wrapper when service succeeds (happy path)" in new SetupWithCis {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.successful(sampleWrapper))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(sampleWrapper)
        verify(mockMonthlyReturnService).retrieveMonthlyReturns(eqTo(EmployerReference("123","AB456")))(any[HeaderCarrier])
      }

      "return 200 with empty wrapper when service returns empty" in new SetupWithCis {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.successful(UserMonthlyReturns(Seq.empty)))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(UserMonthlyReturns(Seq.empty))
      }

      "return 400 when CIS enrolment identifiers are missing" in new SetupWithoutCis {
        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String].toLowerCase must include ("missing cis enrolment")
        verifyNoInteractions(mockMonthlyReturnService)
      }

      "map UpstreamErrorResponse to same status with message" in new SetupWithCis {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", 502)))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe 502
        (contentAsJson(result) \ "message").as[String] must include ("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupWithCis {
        when(mockMonthlyReturnService.retrieveMonthlyReturns(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("kaboom")))

        val result: Future[Result] = controller.retrieveMonthlyReturns(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal ("Unexpected error")
      }
    }
  }


  private lazy val sampleWrapper: UserMonthlyReturns = UserMonthlyReturns(
    Seq(
      MonthlyReturn(
        monthlyReturnId = 66666L,
        taxYear = 2025,
        taxMonth = 1
      ),
      MonthlyReturn(
        monthlyReturnId = 66667L,
        taxYear = 2025,
        taxMonth = 7
      )
    )
  )

  private trait BaseSetup {
    val mockMonthlyReturnService: MonthlyReturnService = mock(classOf[MonthlyReturnService])
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier   = HeaderCarrier()
  }

  private trait SetupWithCis extends BaseSetup {
    private val auth: AuthAction = fakeAuthAction(ton = "123", tor = "AB456")
    val controller = new MonthlyReturnsController(auth, mockMonthlyReturnService, cc)
  }

  private trait SetupWithoutCis extends BaseSetup {
    private val auth: AuthAction = noCisAuthAction
    val controller = new MonthlyReturnsController(auth, mockMonthlyReturnService, cc)
  }
}
