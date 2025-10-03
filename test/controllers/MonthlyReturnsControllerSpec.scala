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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.MonthlyReturnsController
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class MonthlyReturnsControllerSpec extends SpecBase {

  "MonthlyReturnsController" - {

    "GET /cis/taxpayer (getCisTaxpayer)" - {

      "return 200 with {cisId} when service resolves it (happy path)" in new SetupWithEnrolmentReference {
        val taxpayer = mkTaxpayer()
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.successful(taxpayer))

        val result: Future[Result] = controller.getCisTaxpayer(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(taxpayer)
        verify(mockMonthlyReturnService).getCisTaxpayer(eqTo(EmployerReference("123", "AB456")))(any[HeaderCarrier])
      }

      "return 400 when datacache says not found" in new SetupAuthOnly {
        val result = controller.getCisTaxpayer(fakeRequest)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Missing CIS enrolment identifiers"

        verifyNoInteractions(mockMonthlyReturnService)
      }

        "return 404 when datacache says not found" in new SetupWithEnrolmentReference {
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result = controller.getCisTaxpayer(fakeRequest)
        status(result) mustBe NOT_FOUND
        (contentAsJson(result) \ "message").as[String].toLowerCase must include ("not found")
      }

      "map other UpstreamErrorResponse to same status with message" in new SetupWithEnrolmentReference {
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result = controller.getCisTaxpayer(fakeRequest)
        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include ("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupWithEnrolmentReference {
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val result = controller.getCisTaxpayer(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal ("Unexpected error")
      }
    }

    "GET /cis/monthly-returns (getAllMonthlyReturns)" - {

      "return 200 with wrapper when service succeeds (happy path)" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(sampleWrapper))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(sampleWrapper)
        verify(mockMonthlyReturnService).getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier])
      }

      "return 200 with empty wrapper when service returns empty" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(UserMonthlyReturns(Seq.empty)))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(UserMonthlyReturns(Seq.empty))
      }

      "return 400 when cisId query param is missing" in new SetupAuthOnly {
        val result: Future[Result] = controller.getAllMonthlyReturns(None)(fakeRequest)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String].toLowerCase must include ("missing 'cisid'")
        verifyNoInteractions(mockMonthlyReturnService)
      }

      "map UpstreamErrorResponse to same status with message" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include ("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal ("Unexpected error")
      }
    }
  }

    "POST /cis/monthly-returns/nil (createNil)" - {

      "return 204 when service succeeds" in new SetupAuthOnly {
        when(mockMonthlyReturnService.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.successful(()))

        val payload = NilMonthlyReturnRequest("CIS-123", 2024, 3, Some("option1"), Some("confirmed"))
        val result: Future[Result] = controller.createNil()(fakeRequest.withBody(Json.toJson(payload)))

        status(result) mustBe NO_CONTENT
      }

      "return 400 on invalid json" in new SetupAuthOnly {
        val result: Future[Result] = controller.createNil()(fakeRequest.withBody(Json.obj("bad" -> "json")))
        status(result) mustBe BAD_REQUEST
      }

      "propagate UpstreamErrorResponse status" in new SetupAuthOnly {
        when(mockMonthlyReturnService.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom", BAD_GATEWAY)))

        val payload = NilMonthlyReturnRequest("CIS-123", 2024, 3, None, None)
        val result: Future[Result] = controller.createNil()(fakeRequest.withBody(Json.toJson(payload)))

        status(result) mustBe BAD_GATEWAY
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
    val mockMonthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier   = HeaderCarrier()
  }

  private trait SetupWithEnrolmentReference extends BaseSetup {
    private val auth: AuthAction = fakeAuthAction(ton = "123", tor = "AB456")
    val controller = new MonthlyReturnsController(auth, mockMonthlyReturnService, cc)
  }

  private trait SetupAuthOnly extends BaseSetup {
    private val auth: AuthAction = noEnrolmentReferenceAuthAction
    val controller = new MonthlyReturnsController(auth, mockMonthlyReturnService, cc)
  }
}
