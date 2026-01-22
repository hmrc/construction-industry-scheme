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

import actions.FakeAuthAction
import base.SpecBase
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK}
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.MonthlyReturnsController
import uk.gov.hmrc.constructionindustryscheme.models.response.{CreateNilMonthlyReturnResponse, UnsubmittedMonthlyReturnsResponse, UnsubmittedMonthlyReturnsRow}
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.ClientListService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class MonthlyReturnsControllerSpec extends SpecBase {

  "MonthlyReturnsController" - {

    "GET /cis/client/taxpayer/:taxOfficeNumber/:taxOfficeReference (getCisClientTaxpayer)" - {

      val taxOfficeNumber    = "123"
      val taxOfficeReference = "AB456"
      val irAgentId          = "SA123456"
      val credId             = "cred-123"

      "return 200 with taxpayer when client exists and service resolves it (happy path)" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]
        val taxpayer                 = mkTaxpayer()

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.successful(true))

        when(
          mockMonthlyReturnService.getCisTaxpayer(eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference)))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.successful(taxpayer))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(taxpayer)
        verify(mockClientListService, times(1)).hasClient(
          eqTo(taxOfficeNumber),
          eqTo(taxOfficeReference),
          eqTo(irAgentId),
          eqTo(credId),
          any[scala.concurrent.duration.FiniteDuration]
        )(using any[HeaderCarrier])
        verify(mockMonthlyReturnService, times(1)).getCisTaxpayer(
          eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
        )(any[HeaderCarrier])
      }

      "return 403 Forbidden when client does not exist" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.successful(false))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj("error" -> "Client not found")
        verify(mockClientListService, times(1)).hasClient(
          eqTo(taxOfficeNumber),
          eqTo(taxOfficeReference),
          eqTo(irAgentId),
          eqTo(credId),
          any[scala.concurrent.duration.FiniteDuration]
        )(using any[HeaderCarrier])
        verify(mockMonthlyReturnService, never()).getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier])
      }

      "return 404 when client exists but datacache says taxpayer not found" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.successful(true))

        when(
          mockMonthlyReturnService.getCisTaxpayer(eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference)))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe NOT_FOUND
        (contentAsJson(result) \ "message").as[String].toLowerCase must include("not found")
      }

      "map other UpstreamErrorResponse to same status with message when client exists" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.successful(true))

        when(
          mockMonthlyReturnService.getCisTaxpayer(eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference)))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception when client exists" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.successful(true))

        when(
          mockMonthlyReturnService.getCisTaxpayer(eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference)))(
            any[HeaderCarrier]
          )
        )
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }

      "return 403 Forbidden when credentialId is missing" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        val authAction = FakeAuthAction.withEnrolments(
          Set(
            uk.gov.hmrc.auth.core.Enrolment(
              key = "IR-PAYE-AGENT",
              identifiers = Seq(uk.gov.hmrc.auth.core.EnrolmentIdentifier("IRAgentReference", irAgentId)),
              state = "Activated"
            )
          ),
          bodyParsers,
          credId = None
        )
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj("error" -> "credentialId is missing from session")
        verify(mockClientListService, never()).hasClient(
          any[String],
          any[String],
          any[String],
          any[String],
          any[scala.concurrent.duration.FiniteDuration]
        )(using any[HeaderCarrier])
      }

      "return 403 Forbidden when IR-PAYE-AGENT enrolment is missing" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        val authAction = FakeAuthAction.withEnrolments(Set.empty, bodyParsers, Some(credId))
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe FORBIDDEN
        contentAsJson(result) mustBe Json.obj("error" -> "IR-PAYE-AGENT enrolment with IRAgentReference is missing")
        verify(mockClientListService, never()).hasClient(
          any[String],
          any[String],
          any[String],
          any[String],
          any[scala.concurrent.duration.FiniteDuration]
        )(using any[HeaderCarrier])
      }

      "return 500 InternalServerError when hasClient service fails" in {
        val mockMonthlyReturnService = mock[MonthlyReturnService]
        val mockClientListService    = mock[ClientListService]

        when(
          mockClientListService.hasClient(
            eqTo(taxOfficeNumber),
            eqTo(taxOfficeReference),
            eqTo(irAgentId),
            eqTo(credId),
            any[scala.concurrent.duration.FiniteDuration]
          )(using any[HeaderCarrier])
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("Service error", 500, 500)))

        val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
        val controller = new MonthlyReturnsController(authAction, mockMonthlyReturnService, mockClientListService, cc)

        val result = controller.getCisClientTaxpayer(taxOfficeNumber, taxOfficeReference)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj("error" -> "Failed to check client")
      }
    }

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
        (contentAsJson(result) \ "message").as[String].toLowerCase must include("not found")
      }

      "map other UpstreamErrorResponse to same status with message" in new SetupWithEnrolmentReference {
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result = controller.getCisTaxpayer(fakeRequest)
        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupWithEnrolmentReference {
        when(mockMonthlyReturnService.getCisTaxpayer(any[EmployerReference])(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val result = controller.getCisTaxpayer(fakeRequest)
        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
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
        (contentAsJson(result) \ "message").as[String].toLowerCase must include("missing 'cisid'")
        verifyNoInteractions(mockMonthlyReturnService)
      }

      "map UpstreamErrorResponse to same status with message" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getAllMonthlyReturnsByCisId(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val result: Future[Result] = controller.getAllMonthlyReturns(Some("CIS-123"))(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }
    }

    "GET /cis/scheme/email/:instanceId (getSchemeEmail)" - {

      "return 200 with email when service returns Some" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Some("x@y.com")))

        val result: Future[Result] = controller.getSchemeEmail("CIS-123")(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "email").asOpt[String] mustBe Some("x@y.com")
        verify(mockMonthlyReturnService).getSchemeEmail(eqTo("CIS-123"))(any[HeaderCarrier])
      }

      "return 200 with null when service returns None" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.successful(None))

        val result: Future[Result] = controller.getSchemeEmail("CIS-123")(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "email").toOption.flatMap(_.asOpt[String]) mustBe None
      }

      "map UpstreamErrorResponse to same status with message" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result: Future[Result] = controller.getSchemeEmail("CIS-123")(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return 500 Unexpected error on unknown exception" in new SetupAuthOnly {
        when(mockMonthlyReturnService.getSchemeEmail(eqTo("CIS-123"))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("unexpected-exception")))

        val result: Future[Result] = controller.getSchemeEmail("CIS-123")(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] must equal("Unexpected error")
      }
    }

    "GET /cis/monthly-returns/unsubmitted/:instanceId (getUnsubmittedMonthlyReturns)" - {

      "return 200 with wrapper when service succeeds (happy path)" in new SetupAuthOnly {
        val instanceId = "INSTANCE-123"

        val payload = UnsubmittedMonthlyReturnsResponse(
          unsubmittedCisReturns = Seq(
            UnsubmittedMonthlyReturnsRow(
              taxYear = 2025,
              taxMonth = 1,
              returnType = "Nil",
              status = "STARTED",
              lastUpdate = None
            )
          )
        )

        when(mockMonthlyReturnService.getUnsubmittedMonthlyReturns(eqTo(instanceId))(any[HeaderCarrier]))
          .thenReturn(Future.successful(payload))

        val result = controller.getUnsubmittedMonthlyReturns(instanceId)(fakeRequest)

        status(result) mustBe OK
        contentAsJson(result) mustBe Json.toJson(payload)
        verify(mockMonthlyReturnService).getUnsubmittedMonthlyReturns(eqTo(instanceId))(any[HeaderCarrier])
      }

      "return 400 when instanceId is blank" in new SetupAuthOnly {
        val result = controller.getUnsubmittedMonthlyReturns("   ")(fakeRequest)

        status(result) mustBe BAD_REQUEST
        (contentAsJson(result) \ "message").as[String] mustBe "Missing 'cisId'"
        verifyNoInteractions(mockMonthlyReturnService)
      }

      "map UpstreamErrorResponse to same status with message" in new SetupAuthOnly {
        val instanceId = "abc-123"

        when(mockMonthlyReturnService.getUnsubmittedMonthlyReturns(eqTo(instanceId))(any[HeaderCarrier]))
          .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

        val result = controller.getUnsubmittedMonthlyReturns(instanceId)(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "return 500 with 'Unexpected error' when a NonFatal exception occurs" in new SetupAuthOnly {
        val instanceId = "abc-123"

        when(mockMonthlyReturnService.getUnsubmittedMonthlyReturns(eqTo(instanceId))(any[HeaderCarrier]))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val result = controller.getUnsubmittedMonthlyReturns(instanceId)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "GET /cis/monthly-returns/details/:instanceId/:taxMonth/:taxYear (getAllDetails)" - {

      "return 200 with all monthly return details" in new SetupAuthOnly {
        val instanceId = "INSTANCE-123"
        val taxMonth   = 1
        val taxYear    = 2025

        val result = controller.getAllDetails(instanceId, taxMonth, taxYear)(fakeRequest)

        status(result) mustBe OK
        val json = contentAsJson(result)
        (json \ "scheme").as[Seq[play.api.libs.json.JsValue]]             must not be empty
        (json \ "monthlyReturn").as[Seq[play.api.libs.json.JsValue]]      must not be empty
        (json \ "subcontractors").as[Seq[play.api.libs.json.JsValue]]     must not be empty
        (json \ "monthlyReturnItems").as[Seq[play.api.libs.json.JsValue]] must not be empty
        (json \ "submission").as[Seq[play.api.libs.json.JsValue]]         must not be empty
      }
    }
  }

  "POST /cis/monthly-returns/nil (createNil)" - {

    "return 200 with monthly return when service succeeds" in new SetupAuthOnly {
      val expectedResponse = CreateNilMonthlyReturnResponse("STARTED")
      when(mockMonthlyReturnService.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(expectedResponse))

      val payload                = NilMonthlyReturnRequest("CIS-123", 2024, 3, "Y", "Y")
      val result: Future[Result] = controller.createNil()(fakeRequest.withBody(Json.toJson(payload)))

      status(result) mustBe CREATED
      contentAsJson(result) mustBe Json.toJson(expectedResponse)
      verify(mockMonthlyReturnService).createNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
    }

    "return 400 on invalid json" in new SetupAuthOnly {
      val result: Future[Result] = controller.createNil()(fakeRequest.withBody(Json.obj("bad" -> "json")))
      status(result) mustBe BAD_REQUEST
    }

    "propagate UpstreamErrorResponse status" in new SetupAuthOnly {
      when(mockMonthlyReturnService.createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("boom", BAD_GATEWAY)))

      val payload                = NilMonthlyReturnRequest("CIS-123", 2024, 3, "Y", "Y")
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
    val mockClientListService: ClientListService       = mock[ClientListService]
    implicit val ec: ExecutionContext                  = cc.executionContext
    implicit val hc: HeaderCarrier                     = HeaderCarrier()
  }

  private trait SetupWithEnrolmentReference extends BaseSetup {
    private val auth: AuthAction = fakeAuthAction(ton = "123", tor = "AB456")
    val controller               = new MonthlyReturnsController(auth, mockMonthlyReturnService, mockClientListService, cc)
  }

  private trait SetupAuthOnly extends BaseSetup {
    private val auth: AuthAction = noEnrolmentReferenceAuthAction
    val controller               = new MonthlyReturnsController(auth, mockMonthlyReturnService, mockClientListService, cc)
  }
}
