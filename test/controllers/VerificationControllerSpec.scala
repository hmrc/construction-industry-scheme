/*
 * Copyright 2026 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, JSON, POST, contentAsJson, contentType, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.VerificationController
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.services.{SubmissionService, VerificationService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import scala.concurrent.Future

class VerificationControllerSpec extends SpecBase with EitherValues {

  private def mockController(
    verificationService: VerificationService,
    submissionService: SubmissionService = mock[SubmissionService],
    auth: AuthAction = fakeAuthAction()
  ): VerificationController =
    new VerificationController(auth, verificationService, submissionService, cc)

  "getNewestVerificationBatch" - {

    val instanceId = "abc-123"
    val url        = s"/cis/verification-batch/newest/$instanceId"

    "returns 200 OK with JSON body when service succeeds (full payload)" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val response = GetNewestVerificationBatchResponse(
        scheme = Some(
          ContractorSchemeNewVerification(
            accountsOfficeReference = Some("123PA00123456"),
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com")
          )
        ),
        subcontractors = Seq(
          SubcontractorNewVerification(
            subcontractorId = 1L,
            firstName = Some("John"),
            secondName = Some("Q"),
            surname = Some("Smith"),
            tradingName = Some("ACME"),
            partnershipTradingName = Some("ACME trading"),
            verified = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationDate = Some(LocalDateTime.of(2026, 1, 3, 10, 0, 0)),
            lastMonthlyReturnDate = Some(LocalDateTime.of(2026, 1, 4, 10, 0, 0)),
            createDate = Some(LocalDateTime.of(2026, 1, 4, 10, 0, 0)),
            subcontractorType = Some("soletrader"),
            subbieResourceRef = Some(10L),
            utr = Some("1111111111"),
            partnerUtr = None,
            crn = None,
            nino = Some("AA123456A")
          )
        ),
        verificationBatch = Some(
          VerificationBatch(
            verificationBatchId = 99L,
            status = Some("STARTED"),
            verificationNumber = Some("VB0001")
          )
        ),
        verifications = Seq(
          Verification(
            verificationId = 1001L,
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationBatchId = Some(99L),
            subcontractorId = Some(1L)
          )
        ),
        submission = Some(
          SubmissionNewVerification(
            submissionId = 555L,
            activeObjectId = Some(99L),
            status = Some("ACCEPTED"),
            submissionRequestDate = Some(LocalDateTime.of(2026, 1, 1, 13, 5, 0))
          )
        ),
        monthlyReturn = Some(
          MonthlyReturnNewVerification(
            monthlyReturnId = 777L,
            decNoMoreSubPayments = Some("N")
          )
        ),
        monthlyReturnSubmission = Some(
          MonthlyReturnSubmissionNewVerification(
            submissionId = 888L,
            submissionRequestDate = Some(LocalDateTime.of(2026, 2, 12, 11, 59, 0))
          )
        )
      )

      when(verificationService.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getNewestVerificationBatch(instanceId)(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)

      val json = contentAsJson(result)

      (json \ "scheme").\("name").as[String] mustBe "ABC Construction Ltd"

      (json \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L

      (json \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
      (json \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      (json \ "submission").\("submissionId").as[Long] mustBe 555L
      (json \ "monthlyReturn").\("monthlyReturnId").as[Long] mustBe 777L
      (json \ "monthlyReturnSubmission").\("submissionId").as[Long] mustBe 888L

      json mustBe Json.toJson(response)

      verify(verificationService).getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }

    "returns 502 BadGateway with error body when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getNewestVerificationBatch(instanceId)(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "get-newest-verification-batch-failed")

      verify(verificationService).getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }
  }

  "getCurrentVerificationBatch" - {

    val instanceId = "abc-123"
    val url        = s"/cis/verification-batch/current/$instanceId"

    "returns 200 OK with JSON body when service succeeds (full payload)" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val response = GetCurrentVerificationBatchResponse(
        subcontractors = Seq(
          SubcontractorCurrentVerification(
            subcontractorId = 1L,
            subbieResourceRef = Some(10L),
            firstName = Some("John"),
            secondName = Some("Q"),
            surname = Some("Smith"),
            tradingName = Some("ACME"),
            utr = Some("1111111111"),
            nino = Some("AA123456A"),
            crn = Some("AC012345"),
            partnerUtr = Some("5860920998"),
            partnershipTradingName = Some("ACME trading"),
            subcontractorType = Some("soletrader"),
            addressLine1 = Some("Line 1"),
            addressLine2 = Some("Line 2"),
            addressLine3 = Some("Line 3"),
            addressLine4 = Some("Line 4"),
            country = Some("UK"),
            postcode = Some("NE1 1AA"),
            worksReferenceNumber = Some("WRN123")
          )
        ),
        verificationBatch = Some(
          VerificationBatchCurrentVerification(
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(999L)
          )
        ),
        verifications = Seq(
          VerificationCurrentVerification(
            verificationId = 1001L,
            verificationBatchId = Some(99L),
            subcontractorId = Some(1L),
            verificationResourceRef = Some(1L)
          )
        )
      )

      when(verificationService.getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getCurrentVerificationBatch(instanceId)(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)

      val json = contentAsJson(result)

      (json \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L
      (json \ "subcontractors")(0).\("utr").as[String] mustBe "1111111111"
      (json \ "subcontractors")(0).\("subcontractorType").as[String] mustBe "soletrader"

      (json \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
      (json \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      json mustBe Json.toJson(response)

      verify(verificationService).getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }

    "returns 502 BadGateway with error body when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getCurrentVerificationBatch(instanceId)(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "get-current-verification-batch-failed")

      verify(verificationService).getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }
  }

  "createVerificationBatchAndVerifications" - {

    val url = "/cis/verification-batch/create"

    val validRequest: CreateVerificationBatchAndVerificationsRequest =
      CreateVerificationBatchAndVerificationsRequest(
        instanceId = "abc-123",
        verificationResourceReferences = Seq(1L, 2L),
        actionIndicator = Some("A")
      )

    val validJson: JsValue = Json.toJson(validRequest)

    val response: CreateVerificationBatchAndVerificationsResponse =
      CreateVerificationBatchAndVerificationsResponse(
        verificationBatchResourceReference = 10L
      )

    "returns 201 Created with JSON body when service succeeds" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe CREATED
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(verificationService).createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val badJson = Json.obj("bad" -> "json")

      val req = FakeRequest(POST, url)
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(verificationService)
    }

    "returns 502 BadGateway with error body when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "create-verification-batch-and-verifications-failed")

      verify(verificationService).createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }
  }

  "modifyVerifications" - {

    val url = "/cis/verification-batch/modify"

    val validRequest: ModifyVerificationsRequest =
      ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

    val validJson: JsValue = Json.toJson(validRequest)

    "returns 204 NoContent when service succeeds" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.modifyVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.modifyVerifications()(req)

      status(result) mustBe NO_CONTENT
      verify(verificationService).modifyVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val badJson = Json.obj("bad" -> "json")

      val req = FakeRequest(POST, url)
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.modifyVerifications()(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(verificationService)
    }

    "returns 502 BadGateway with error body when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.modifyVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.modifyVerifications()(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "modify-verifications-failed")

      verify(verificationService).modifyVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }
  }

  "sendEmailForVerification" - {

    "returns 202 when service succeeds" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val body = Json.obj(
        "email" -> "test@test.com"
      )

      when(submissionService.sendEmailForVerification(any[SubcontractorVerificationEmailRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req =
        FakeRequest(POST, "/verification-batch/send-email")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendEmailForVerification(req)

      status(result) mustBe 202

      verify(submissionService, times(1)).sendEmailForVerification(any[SubcontractorVerificationEmailRequest])(
        any[HeaderCarrier]
      )
    }

    "returns 400 when request JSON is invalid" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val badJson = Json.obj("notEmail" -> "test@test.com")

      val req =
        FakeRequest(POST, "/verification-batch/send-email")
          .withBody(badJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendEmailForVerification(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(submissionService)
    }

    "returns 502 when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val body = Json.obj(
        "email" -> "test@test.com"
      )

      when(submissionService.sendEmailForVerification(any[SubcontractorVerificationEmailRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req =
        FakeRequest(POST, "/verification-batch/send-email")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendEmailForVerification(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "send-email-for-verification-failed"
    }
  }

  "createSubmissionForVerification" - {

    val url = "/cis/verification-batch/submission/create"

    val validRequest = CreateSubmissionAndUpdateVerificationsRequest(
      instanceId = "abc-123",
      verificationBatchId = 99L,
      verificationBatchResourceRef = 10L,
      emailRecipient = "ops@example.com",
      irMarkGenerated = Some("IR_MARK"),
      verifications = Seq(
        VerificationToUpdate("ACME", 111L, "Y"),
        VerificationToUpdate("BETA", 222L, "N")
      ),
      agentId = None
    )

    val validJson: JsValue = Json.toJson(validRequest)

    val response = CreateSubmissionAndUpdateVerificationsResponse(submissionId = 555L)

    "returns 201 Created with JSON body when service succeeds" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.createSubmissionAndUpdateVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmissionAndUpdateVerifications()(req)

      status(result) mustBe CREATED
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(verificationService).createSubmissionAndUpdateVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      val badJson = Json.obj("instanceId" -> "abc-123")

      val req = FakeRequest(POST, url)
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmissionAndUpdateVerifications()(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(verificationService)
    }

    "returns 502 BadGateway with error body when service fails" in {
      val verificationService = mock[VerificationService]
      val submissionService   = mock[SubmissionService]
      val controller          = mockController(verificationService, submissionService)

      when(verificationService.createSubmissionAndUpdateVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmissionAndUpdateVerifications()(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "create-submission-for-verification-failed")

      verify(verificationService).createSubmissionAndUpdateVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }
  }

  "updateVerificationSubmission" - {

    val url = "/cis/verification/submission/update"

    val validRequest = UpdateVerificationSubmissionRequest(
      instanceId = "1",
      verificationBatchResourceRef = 2001L,
      submittableStatus = "SUBMITTED",
      submissionRequestDate = None,
      hmrcMarkGenerated = None
    )

    val validJson = Json.obj(
      "instanceId"                   -> "1",
      "verificationBatchId"          -> 1001L,
      "verificationBatchResourceRef" -> 2001L,
      "submittableStatus"            -> "SUBMITTED"
    )

    "returns 204 NoContent when service succeeds" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      when(service.updateVerificationSubmission(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(validJson)

      val result = controller.updateVerificationSubmission()(req)

      status(result) mustBe NO_CONTENT

      verify(service).updateVerificationSubmission(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      val invalidJson = Json.obj(
        "instanceId" -> "1"
      )

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(invalidJson)

      val result = controller.updateVerificationSubmission()(req)

      status(result) mustBe BAD_REQUEST
      contentType(result) mustBe Some(JSON)

      verify(service, never()).updateVerificationSubmission(any[UpdateVerificationSubmissionRequest])(
        any[HeaderCarrier]
      )
    }

    "returns 502 BadGateway when service fails" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      when(service.updateVerificationSubmission(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withHeaders(CONTENT_TYPE -> JSON)
        .withBody(validJson)

      val result = controller.updateVerificationSubmission()(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "update-verification-submission-failed")

      verify(service).updateVerificationSubmission(eqTo(validRequest))(any[HeaderCarrier])
    }
  }
}
