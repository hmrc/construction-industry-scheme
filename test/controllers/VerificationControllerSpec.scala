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
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, JSON, POST, contentAsJson, contentType, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.VerificationController
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.VerificationService
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class VerificationControllerSpec extends SpecBase with EitherValues {

  private def mockController(
    verificationService: VerificationService,
    auth: AuthAction = fakeAuthAction()
  ): VerificationController =
    new VerificationController(auth, verificationService, cc)

  "getNewestVerificationBatch" - {

    val instanceId = "abc-123"
    val url        = s"/cis/verification-batch/newest/$instanceId"

    "returns 200 OK with JSON body when service succeeds (full payload)" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      val response = GetNewestVerificationBatchResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 123,
            instanceId = instanceId,
            accountsOfficeReference = "123PA00123456",
            taxOfficeNumber = "163",
            taxOfficeReference = "AB0063",
            utr = Some("1234567890"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("test@example.com"),
            displayWelcomePage = Some("Y"),
            prePopCount = Some(1),
            prePopSuccessful = Some("Y"),
            subcontractorCounter = Some(10),
            verificationBatchCounter = Some(2),
            lastUpdate = Some(Instant.parse("2026-01-01T10:00:00Z")),
            version = Some(3)
          )
        ),
        subcontractors = Seq(
          Subcontractor(
            subcontractorId = 1L,
            utr = Some("1111111111"),
            pageVisited = Some(2),
            partnerUtr = None,
            crn = None,
            firstName = Some("John"),
            nino = Some("AA123456A"),
            secondName = Some("Q"),
            surname = Some("Smith"),
            partnershipTradingName = None,
            tradingName = Some("ACME"),
            subcontractorType = Some("soletrader"),
            addressLine1 = Some("1 Main Street"),
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = Some("United Kingdom"),
            postcode = Some("AA1 1AA"),
            emailAddress = Some("john@acme.test"),
            phoneNumber = Some("02000000000"),
            mobilePhoneNumber = Some("07123456789"),
            worksReferenceNumber = Some("WRN-001"),
            createDate = Some(LocalDateTime.of(2026, 1, 1, 10, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 2, 10, 0, 0)),
            subbieResourceRef = Some(10L),
            matched = Some("Y"),
            autoVerified = Some("N"),
            verified = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationDate = Some(LocalDateTime.of(2026, 1, 3, 10, 0, 0)),
            version = Some(1),
            updatedTaxTreatment = Some("0"),
            lastMonthlyReturnDate = Some(LocalDateTime.of(2026, 1, 4, 10, 0, 0)),
            pendingVerifications = Some(0)
          )
        ),
        verificationBatch = Seq(
          VerificationBatch(
            verificationBatchId = 99L,
            schemeId = 123L,
            verificationsCounter = Some(1L),
            verifBatchResourceRef = Some(999L),
            proceedSession = Some("Y"),
            confirmArrangement = Some("Y"),
            confirmCorrect = Some("Y"),
            status = Some("STARTED"),
            verificationNumber = Some("VB0001"),
            createDate = Some(LocalDateTime.of(2026, 1, 1, 11, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 2, 11, 0, 0)),
            version = Some(1)
          )
        ),
        verifications = Seq(
          Verification(
            verificationId = 1001L,
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            actionIndicator = Some("A"),
            verificationBatchId = Some(99L),
            schemeId = Some(123L),
            subcontractorId = Some(1L),
            subcontractorName = Some("ACME"),
            verificationResourceRef = Some(1L),
            proceed = Some("Y"),
            createDate = Some(LocalDateTime.of(2026, 1, 1, 12, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 2, 12, 0, 0)),
            version = Some(1)
          )
        ),
        submission = Seq(
          Submission(
            submissionId = 555L,
            submissionType = "VERIFICATIONS",
            activeObjectId = Some(99L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("hmrcMarkGen"),
            hmrcMarkGgis = Some("hmrcMarkGgis"),
            emailRecipient = Some("ops@example.com"),
            acceptedTime = Some("12:00"),
            createDate = Some(LocalDateTime.of(2026, 1, 1, 13, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 2, 13, 0, 0)),
            schemeId = 123L,
            agentId = Some("agent-1"),
            l_Migrated = Some(0L),
            submissionRequestDate = Some(LocalDateTime.of(2026, 1, 1, 13, 5, 0)),
            govTalkErrorCode = Some("E001"),
            govTalkErrorType = Some("TYPE"),
            govTalkErrorMessage = Some("error message")
          )
        ),
        monthlyReturn = Seq(
          MonthlyReturn(
            monthlyReturnId = 777L,
            taxYear = 2025,
            taxMonth = 1,
            nilReturnIndicator = Some("N"),
            decEmpStatusConsidered = Some("Y"),
            decAllSubsVerified = Some("Y"),
            decInformationCorrect = Some("Y"),
            decNoMoreSubPayments = Some("N"),
            decNilReturnNoPayments = Some("N"),
            status = Some("SUBMITTED"),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 5, 10, 0, 0)),
            amendment = Some("N"),
            supersededBy = None
          )
        ),
        monthlyReturnSubmission = Seq(
          Submission(
            submissionId = 556L,
            submissionType = "MONTHLY_RETURN",
            activeObjectId = Some(777L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("hmrcMrMarkGen"),
            hmrcMarkGgis = Some("hmrcMrMarkGgis"),
            emailRecipient = Some("mr@example.com"),
            acceptedTime = Some("13:00"),
            createDate = Some(LocalDateTime.of(2026, 1, 5, 11, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 5, 12, 0, 0)),
            schemeId = 123L,
            agentId = Some("agent-2"),
            l_Migrated = Some(0L),
            submissionRequestDate = Some(LocalDateTime.of(2026, 1, 5, 11, 5, 0)),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      when(service.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getNewestVerificationBatch(instanceId)(req)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)

      val json = contentAsJson(result)

      (json \ "scheme")(0).\("schemeId").as[Int] mustBe 123
      (json \ "scheme")(0).\("instanceId").as[String] mustBe "abc-123"

      (json \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L
      (json \ "subcontractors")(0).\("utr").as[String] mustBe "1111111111"

      (json \ "verificationBatch")(0).\("verificationBatchId").as[Long] mustBe 99L
      (json \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      (json \ "submission")(0).\("submissionId").as[Long] mustBe 555L
      (json \ "monthlyReturn")(0).\("monthlyReturnId").as[Long] mustBe 777L
      (json \ "monthlyReturnSubmission")(0).\("submissionId").as[Long] mustBe 556L

      json mustBe Json.toJson(response)

      verify(service).getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }

    "returns 502 BadGateway with error body when service fails" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      when(service.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, url)
      val result                                   = controller.getNewestVerificationBatch(instanceId)(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "get-newest-verification-batch-failed")

      verify(service).getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
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
      val service    = mock[VerificationService]
      val controller = mockController(service)

      when(service.createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe CREATED
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(response)

      verify(service).createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      val badJson = Json.obj("bad" -> "json")

      val req = FakeRequest(POST, url)
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(service)
    }

    "returns 502 BadGateway with error body when service fails" in {
      val service    = mock[VerificationService]
      val controller = mockController(service)

      when(service.createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, url)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createVerificationBatchAndVerifications()(req)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.obj("message" -> "create-verification-batch-and-verifications-failed")

      verify(service).createVerificationBatchAndVerifications(eqTo(validRequest))(any[HeaderCarrier])
    }
  }
}
