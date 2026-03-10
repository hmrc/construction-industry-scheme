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
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.eq as eqTo
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, EmailConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, BuiltSubmissionPayload, DEPARTMENTAL_ERROR, EmployerReference, FATAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SUBMITTED, SubmissionResult, SubmissionStatus}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateGovTalkStatusRecordRequest, CreateSubmissionRequest, GetGovTalkStatusRequest, NilMonthlyReturnOrgSuccessEmail, SendSuccessEmailRequest, UpdateGovTalkStatusRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, GetGovTalkStatusResponse}
import uk.gov.hmrc.constructionindustryscheme.services.{MonthlyReturnService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.xml.Elem

final class SubmissionServiceSpec extends SpecBase {

  private def setup: Setup = new Setup {}

  "createSubmission" - {

    "delegates to FormpProxyConnector and returns submissionId" in {
      val s = setup; import s._

      val req = CreateSubmissionRequest("123", 2024, 4)

      when(formpProxyConnector.createSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful("sub-123"))

      service.createSubmission(req).futureValue mustBe "sub-123"
      verify(formpProxyConnector).createSubmission(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = CreateSubmissionRequest("123", 2024, 4)

      when(formpProxyConnector.createSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createSubmission(req).failed.futureValue.getMessage must include("boom")
    }
  }

  "updateSubmission" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        submittableStatus = "ACCEPTED"
      )

      when(formpProxyConnector.updateSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateSubmission(req).futureValue
      verify(formpProxyConnector).updateSubmission(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123",
        taxYear = 2024,
        taxMonth = 4,
        submittableStatus = "REJECTED"
      )

      when(formpProxyConnector.updateSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new IllegalStateException("nope")))

      service.updateSubmission(req).failed.futureValue.getMessage must include("nope")
    }
  }

  "submitToChris" - {

    "passes envelope + correlationId to ChrisConnector and returns SubmissionResult" in {
      val s = setup
      import s._

      val payload  = mkPayload(corrId = "ABCDEF1234567890ABCDEF1234567890")
      val expected = mkResult(SUBMITTED, corrId = payload.correlationId)

      when(chrisConnector.submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.submitToChris(payload).futureValue mustBe expected

      verify(chrisConnector).submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
      verifyNoInteractions(emailConnector)
    }

    "propagates failures from ChrisConnector" in {
      val s = setup
      import s._

      val payload = mkPayload()

      when(chrisConnector.submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("downstream boom")))

      service.submitToChris(payload).failed.futureValue.getMessage must include("downstream boom")

      verifyNoInteractions(formpProxyConnector)
      verifyNoInteractions(emailConnector)
    }
  }

  "pollSubmission" - {

    "delegates to ChrisConnector and returns ChrisPollResponse with SUBMITTED status" in {
      val s = setup; import s._

      val correlationId = "CORR-123"
      val pollUrl       = "http://example.com/poll"
      val expected      = ChrisPollResponse(SUBMITTED, None, None)

      when(chrisConnector.pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.pollSubmission(correlationId, pollUrl).futureValue mustBe expected

      verify(chrisConnector).pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }

    "delegates to ChrisConnector and returns ChrisPollResponse with ACCEPTED status and pollUrl" in {
      val s = setup; import s._

      val correlationId = "CORR-456"
      val pollUrl       = "http://example.com/poll"
      val expected      = ChrisPollResponse(ACCEPTED, Some(pollUrl), Some(10))

      when(chrisConnector.pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.pollSubmission(correlationId, pollUrl).futureValue mustBe expected

      verify(chrisConnector).pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }

    "delegates to ChrisConnector and returns ChrisPollResponse with FATAL_ERROR status" in {
      val s = setup; import s._

      val correlationId = "CORR-789"
      val pollUrl       = "http://example.com/poll"
      val expected      = ChrisPollResponse(FATAL_ERROR, None, None)

      when(chrisConnector.pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.pollSubmission(correlationId, pollUrl).futureValue mustBe expected

      verify(chrisConnector).pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }

    "delegates to ChrisConnector and returns ChrisPollResponse with DEPARTMENTAL_ERROR status" in {
      val s = setup; import s._

      val correlationId = "CORR-ABC"
      val pollUrl       = "http://example.com/poll"
      val expected      = ChrisPollResponse(DEPARTMENTAL_ERROR, None, None)

      when(chrisConnector.pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.pollSubmission(correlationId, pollUrl).futureValue mustBe expected

      verify(chrisConnector).pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }

    "propagates failures from ChrisConnector" in {
      val s = setup; import s._

      val correlationId = "CORR-FAIL"
      val pollUrl       = "http://example.com/poll"

      when(chrisConnector.pollSubmission(eqTo(correlationId), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("polling failed")))

      service.pollSubmission(correlationId, pollUrl).failed.futureValue.getMessage must include("polling failed")
    }
  }

  "sendSuccessfulEmail" - {

    "builds NilMonthlyReturnOrgSuccessEmail and calls EmailConnector, returning Unit" in {
      val s = setup
      import s._

      val submissionId = "90001"
      val req          = SendSuccessEmailRequest(
        email = "test@test.com",
        month = "September",
        year = "2025"
      )

      val expectedPayload =
        NilMonthlyReturnOrgSuccessEmail(
          to = List("test@test.com"),
          templateId = "dtr_cis_nil_monthly_return_org_success",
          parameters = Map("month" -> "September", "year" -> "2025")
        )

      when(emailConnector.sendSuccessfulEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      service.sendSuccessfulEmail(submissionId, req).futureValue mustBe ()

      verify(emailConnector).sendSuccessfulEmail(eqTo(expectedPayload))(any[HeaderCarrier])
      verifyNoMoreInteractions(emailConnector)
    }

    "propagates failures from EmailConnector" in {
      val s = setup
      import s._

      val submissionId = "90001"
      val req          = SendSuccessEmailRequest("test@test.com", "September", "2025")

      val expectedPayload =
        NilMonthlyReturnOrgSuccessEmail(
          to = List("test@test.com"),
          templateId = "dtr_cis_nil_monthly_return_org_success",
          parameters = Map("month" -> "September", "year" -> "2025")
        )

      when(emailConnector.sendSuccessfulEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.sendSuccessfulEmail(submissionId, req).failed.futureValue.getMessage must include("boom")

      verify(emailConnector).sendSuccessfulEmail(eqTo(expectedPayload))(any[HeaderCarrier])
    }
  }

  "getGovTalkStatus" - {

    "delegates to FormpProxyConnector and returns the response when present" in {
      val s = setup
      import s._

      val req = GetGovTalkStatusRequest(
        userIdentifier = "123",
        formResultID = "1234567890"
      )

      val resp = GetGovTalkStatusResponse(
        govtalk_status = Seq.empty
      )

      when(formpProxyConnector.getGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(resp)))

      service.getGovTalkStatus(req).futureValue mustBe Some(resp)

      verify(formpProxyConnector).getGovTalkStatus(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "delegates to FormpProxyConnector and returns None when no record exists" in {
      val s = setup
      import s._

      val req = GetGovTalkStatusRequest(
        userIdentifier = "123",
        formResultID = "1234567890"
      )

      when(formpProxyConnector.getGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      service.getGovTalkStatus(req).futureValue mustBe None

      verify(formpProxyConnector).getGovTalkStatus(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }
  }

  "createGovTalkStatusRecord" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup
      import s._

      val req = CreateGovTalkStatusRecordRequest(
        userIdentifier = "123",
        formResultID = "1234567890",
        correlationID = "CORR-123",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      when(formpProxyConnector.createGovTalkStatusRecord(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.createGovTalkStatusRecord(req).futureValue

      verify(formpProxyConnector).createGovTalkStatusRecord(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup
      import s._

      val req = CreateGovTalkStatusRecordRequest(
        userIdentifier = "123",
        formResultID = "1234567890",
        correlationID = "CORR-123",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      when(formpProxyConnector.createGovTalkStatusRecord(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createGovTalkStatusRecord(req).failed.futureValue.getMessage must include("boom")

      verify(formpProxyConnector).createGovTalkStatusRecord(eqTo(req))(any[HeaderCarrier])
    }
  }

  "updateGovTalkStatus" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        protocolStatus = "dataRequest"
      )

      when(formpProxyConnector.updateGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateGovTalkStatus(req).futureValue

      verify(formpProxyConnector).updateGovTalkStatus(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        protocolStatus = "dataRequest"
      )

      when(formpProxyConnector.updateGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateGovTalkStatus(req).failed.futureValue.getMessage must include("boom")

      verify(formpProxyConnector).updateGovTalkStatus(eqTo(req))(any[HeaderCarrier])
    }
  }

  "initialiseGovTalkStatus" - {

    "returns instanceId when taxpayer exists and no GovTalk status record exists" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"

      val taxpayer          = mkTaxpayer()
      val expectedCreateReq = CreateGovTalkStatusRecordRequest(
        userIdentifier = taxpayer.uniqueId,
        formResultID = submissionId,
        correlationID = correlationId,
        gatewayURL = gatewayUrl
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))
      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))
      when(formpProxyConnector.createGovTalkStatusRecord(eqTo(expectedCreateReq))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service
        .initialiseGovTalkStatus(employerRef, submissionId, correlationId, gatewayUrl)
        .futureValue mustBe taxpayer.uniqueId

      verify(monthlyReturnService).getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier])
      verify(formpProxyConnector)
        .getGovTalkStatus(eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)))(any[HeaderCarrier])
      verify(formpProxyConnector).createGovTalkStatusRecord(eqTo(expectedCreateReq))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "fails when GovTalk status record already exists" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"

      val taxpayer = mkTaxpayer()
      val existing = GetGovTalkStatusResponse(govtalk_status = Seq.empty)

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))
      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(existing)))

      service
        .initialiseGovTalkStatus(employerRef, submissionId, correlationId, gatewayUrl)
        .failed
        .futureValue
        .getMessage mustBe "govtalk status already exists"

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )
    }
  }

  trait Setup {
    val chrisConnector: ChrisConnector             = mock[ChrisConnector]
    val formpProxyConnector: FormpProxyConnector   = mock[FormpProxyConnector]
    val emailConnector: EmailConnector             = mock[EmailConnector]
    val monthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]

    val service = new SubmissionService(chrisConnector, formpProxyConnector, emailConnector, monthlyReturnService)

    def mkPayload(
      corrId: String = "CID-123",
      envelope: Elem = <GovTalkMessage/>
    ): BuiltSubmissionPayload =
      BuiltSubmissionPayload(envelope = envelope, correlationId = corrId, irMark = "IRMK", irEnvelope = envelope)

    def mkResult(
      status: SubmissionStatus,
      corrId: String = "CID-123",
      pollSecs: Int = 15,
      ts: Option[String] = None,
      err: Option[GovTalkError] = None
    ): SubmissionResult =
      SubmissionResult(
        status = status,
        rawXml = "<ack/>",
        meta = GovTalkMeta(
          qualifier = "response",
          function = "submit",
          className = "CIS300MR",
          correlationId = corrId,
          gatewayTimestamp = ts,
          responseEndPoint = ResponseEndPoint("/poll", pollSecs),
          error = err
        )
      )
  }

}
