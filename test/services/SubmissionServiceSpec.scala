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
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, EmailConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{ChrisPollJourney, *}
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.ChrisSubmissionPhase.{Initial, Polling}
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, ChrisSubmissionSessionRepository}
import uk.gov.hmrc.constructionindustryscheme.services.*
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future
import scala.xml.Elem

final class SubmissionServiceSpec extends SpecBase {

  private def setup: Setup = new Setup {}

  private def existingGovTalkStatus(
    userIdentifier: String,
    formResultID: String
  ): GovTalkStatusRecord =
    GovTalkStatusRecord(
      userIdentifier = userIdentifier,
      formResultID = formResultID,
      correlationID = "123456",
      formLock = "Y",
      createDate = Some(LocalDateTime.of(2025, 1, 1, 0, 0)),
      endStateDate = None,
      lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
      numPolls = 0,
      pollInterval = 5,
      protocolStatus = "dataRequest",
      gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
    )

  "createSubmission" - {

    "delegates to FormpProxyConnector and returns submissionId" in {
      val s = setup; import s._

      val req = CreateSubmissionRequest("123", 2024, 4, "N")

      when(formpProxyConnector.createSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful("sub-123"))

      service.createSubmission(req).futureValue mustBe "sub-123"
      verify(formpProxyConnector).createSubmission(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = CreateSubmissionRequest("123", 2024, 4, "N")

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
        amendment = "N",
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
        amendment = "N",
        submittableStatus = "REJECTED"
      )

      when(formpProxyConnector.updateSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new IllegalStateException("nope")))

      service.updateSubmission(req).failed.futureValue.getMessage must include("nope")
    }
  }

  "resetGovTalkStatus" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup; import s._

      val req = ResetGovTalkStatusRequest(
        userIdentifier = "123",
        formResultID = "sub-123",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      when(formpProxyConnector.resetGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.resetGovTalkStatus(req).futureValue
      verify(formpProxyConnector).resetGovTalkStatus(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = ResetGovTalkStatusRequest(
        userIdentifier = "123",
        formResultID = "sub-123",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      when(formpProxyConnector.resetGovTalkStatus(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("reset failed")))

      service.resetGovTalkStatus(req).failed.futureValue.getMessage must include("reset failed")
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

  "pollSubmissionAndUpdateGovTalkStatus" - {

    "polls Chris, updates session and GovTalk status, and returns poll response" in {
      val s = setup
      import s._

      val submissionId = "sub-123"
      val instanceId   = "instance-123"
      val correlation  = "corr-123"
      val pollUrl      = "/poll/123"

      val session = ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = instanceId,
        correlationId = correlation,
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = pollUrl,
        govTalkStatus = None
      )

      val govTalk = GetGovTalkStatusResponse(
        govtalk_status = Seq.empty
      )

      val sessionWithGovTalk = session.copy(
        govTalkStatus = Some(govTalk)
      )

      val updatedSession = sessionWithGovTalk.copy(
        lastMessageDate = Instant.parse("2025-01-02T00:00:00Z"),
        numPolls = 1,
        pollInterval = 20,
        pollUrl = "/poll/999"
      )

      val updatedSessionWithGovTalk = updatedSession.copy(
        govTalkStatus = Some(govTalk)
      )

      val pollResponse = ChrisPollResponse(
        status = SUBMITTED,
        correlationId = correlation,
        pollUrl = Some("/poll/999"),
        pollInterval = Some(20),
        error = None,
        irMarkReceived = None,
        lastMessageDate = Some("2025-01-02T00:00:00Z"),
        acceptedTime = Some("2025-01-02T00:00:00Z")
      )

      when(chrisSubmissionSessionRepository.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))
        .thenReturn(Future.successful(Some(session)))
        .thenReturn(Future.successful(Some(sessionWithGovTalk)))
        .thenReturn(Future.successful(Some(updatedSession)))
        .thenReturn(Future.successful(Some(updatedSession)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId)),
          eqTo(Polling)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(govTalk)))
        .thenReturn(Future.successful(Some(govTalk)))

      when(chrisSubmissionSessionRepository.upsert(eqTo(sessionWithGovTalk)))
        .thenReturn(Future.unit)

      when(
        chrisConnector.pollSubmission(eqTo(correlation), eqTo(pollUrl), eqTo(ChrisPollJourney.MonthlyReturn))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(pollResponse))

      when(
        chrisConnector.deleteSubmission(
          eqTo(correlation),
          eqTo(pollUrl)
        )(using any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(chrisSubmissionSessionRepository.upsert(eqTo(updatedSession)))
        .thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusCorrelationId(
          eqTo(
            UpdateGovTalkStatusCorrelationIdRequest(
              userIdentifier = instanceId,
              formResultID = submissionId,
              correlationID = correlation,
              pollInterval = 20,
              gatewayURL = "/poll/999"
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusStatistics(
          eqTo(
            UpdateGovTalkStatusStatisticsRequest(
              userIdentifier = instanceId,
              formResultID = submissionId,
              lastMessageDate = LocalDateTime.of(2025, 1, 2, 0, 0),
              numPolls = 1,
              pollInterval = 20,
              gatewayURL = "/poll/999"
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatus(
          eqTo(
            UpdateGovTalkStatusRequest(
              userIdentifier = instanceId,
              formResultID = submissionId,
              endStateDate = None,
              protocolStatus = "dataPoll"
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(chrisSubmissionSessionRepository.upsert(eqTo(updatedSessionWithGovTalk)))
        .thenReturn(Future.unit)

      when(formPSubmissionUpdateProcessorRegistry.processorFor(eqTo(ChrisPollJourney.MonthlyReturn)))
        .thenReturn(formPSubmissionUpdateProcessor)

      when(
        formPSubmissionUpdateProcessor.handlePollResponse(any[ChrisSubmissionSessionData], any[ChrisPollResponse])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.unit)

      service
        .pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl, ChrisPollJourney.MonthlyReturn)
        .futureValue mustBe pollResponse
    }

    "passes Verification journey to Chris connector" in {
      val s = setup
      import s._

      val submissionId = "sub-123"
      val instanceId   = "instance-123"
      val correlation  = "corr-expected"
      val pollUrl      = "/poll/123"

      val session = ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = instanceId,
        correlationId = correlation,
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = pollUrl,
        govTalkStatus = None
      )

      val govTalk = GetGovTalkStatusResponse(Seq.empty)

      when(chrisSubmissionSessionRepository.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId)),
          eqTo(Polling)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(govTalk)))

      when(chrisSubmissionSessionRepository.upsert(eqTo(session.copy(govTalkStatus = Some(govTalk)))))
        .thenReturn(Future.unit)

      when(
        chrisConnector.pollSubmission(
          eqTo(correlation),
          eqTo(pollUrl),
          eqTo(ChrisPollJourney.Verification)
        )(using any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          ChrisPollResponse(
            status = SUBMITTED,
            correlationId = "different-correlation",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = None,
            lastMessageDate = None,
            acceptedTime = None
          )
        )
      )

      service
        .pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl, ChrisPollJourney.Verification)
        .failed
        .futureValue
        .getMessage must include("CorrelationId mismatch")

      verify(chrisConnector).pollSubmission(
        eqTo(correlation),
        eqTo(pollUrl),
        eqTo(ChrisPollJourney.Verification)
      )(using any[HeaderCarrier])
    }

    "fails when no session exists" in {
      val s = setup
      import s._

      when(chrisSubmissionSessionRepository.get(eqTo("sub-123")))
        .thenReturn(Future.successful(None))

      service
        .pollSubmissionAndUpdateGovTalkStatus("sub-123", "/poll/123", ChrisPollJourney.MonthlyReturn)
        .failed
        .futureValue
        .getMessage mustBe "No session found for submissionId: sub-123"
    }

    "fails when polled correlationId does not match existing session correlationId" in {
      val s = setup
      import s._

      val submissionId = "sub-123"
      val instanceId   = "instance-123"
      val pollUrl      = "/poll/123"

      val session = ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = instanceId,
        correlationId = "corr-expected",
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = pollUrl,
        govTalkStatus = None
      )

      val govTalk = GetGovTalkStatusResponse(
        govtalk_status = Seq.empty
      )

      val sessionWithGovTalk = session.copy(govTalkStatus = Some(govTalk))

      val pollResponse = ChrisPollResponse(
        status = SUBMITTED,
        correlationId = "corr-actual",
        pollUrl = Some("/poll/999"),
        pollInterval = Some(20),
        error = None,
        irMarkReceived = None,
        lastMessageDate = Some("2025-01-02T00:00:00Z"),
        acceptedTime = Some("2025-01-02T00:00:00Z")
      )

      when(chrisSubmissionSessionRepository.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))
        .thenReturn(Future.successful(Some(session)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId)),
          eqTo(Polling)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(govTalk)))

      when(chrisSubmissionSessionRepository.upsert(eqTo(sessionWithGovTalk)))
        .thenReturn(Future.unit)

      when(
        chrisConnector.pollSubmission(eqTo("corr-expected"), eqTo(pollUrl), eqTo(ChrisPollJourney.MonthlyReturn))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(pollResponse))

      service
        .pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl, ChrisPollJourney.MonthlyReturn)
        .failed
        .futureValue
        .getMessage must include("CorrelationId mismatch")
    }

    "fails when no GovTalk status is found after polling" in {
      val s = setup
      import s._

      val submissionId = "sub-123"
      val instanceId   = "instance-123"
      val correlation  = "corr-123"
      val pollUrl      = "/poll/123"

      val session = ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = instanceId,
        correlationId = correlation,
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = pollUrl,
        govTalkStatus = None
      )

      when(chrisSubmissionSessionRepository.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId)),
          eqTo(Polling)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      val ex =
        service
          .pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl, ChrisPollJourney.MonthlyReturn)
          .failed
          .futureValue

      ex.getMessage mustBe s"No GovTalk status found for instanceId: $instanceId, submissionId: $submissionId"

      verify(chrisSubmissionSessionRepository, never()).upsert(any[ChrisSubmissionSessionData])
      verifyNoInteractions(chrisConnector)
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

      when(emailConnector.sendEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      service.sendSuccessfulEmail(submissionId, req).futureValue mustBe ()

      verify(emailConnector).sendEmail(eqTo(expectedPayload))(any[HeaderCarrier])
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

      when(emailConnector.sendEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.sendSuccessfulEmail(submissionId, req).failed.futureValue.getMessage must include("boom")

      verify(emailConnector).sendEmail(eqTo(expectedPayload))(any[HeaderCarrier])
    }
  }

  "sendEmailForVerification" - {

    "builds SubcontractorVerificationEmail and calls EmailConnector, returning Unit" in {
      val s = setup
      import s._

      val req = SubcontractorVerificationEmailRequest("test@test.com")

      val expectedPayload =
        SubcontractorVerificationEmail(
          to = List("test@test.com"),
          templateId = "dtr_subcontractor_verification",
          parameters = Map.empty[String, String]
        )

      when(emailConnector.sendEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      service.sendEmailForVerification(req).futureValue mustBe ()

      verify(emailConnector).sendEmail(eqTo(expectedPayload))(any[HeaderCarrier])
      verifyNoMoreInteractions(emailConnector)
    }

    "propagates failures from EmailConnector" in {
      val s = setup
      import s._

      val req = SubcontractorVerificationEmailRequest("test@test.com")

      val expectedPayload =
        SubcontractorVerificationEmail(
          to = List("test@test.com"),
          templateId = "dtr_subcontractor_verification",
          parameters = Map.empty[String, String]
        )

      when(emailConnector.sendEmail(eqTo(expectedPayload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.sendEmailForVerification(req).failed.futureValue.getMessage must include("boom")

      verify(emailConnector).sendEmail(eqTo(expectedPayload))(any[HeaderCarrier])
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

  "updateGovTalkStatusCorrelationId" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        correlationID = "corr-123",
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      when(formpProxyConnector.updateGovTalkStatusCorrelationId(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateGovTalkStatusCorrelationId(req).futureValue

      verify(formpProxyConnector).updateGovTalkStatusCorrelationId(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        correlationID = "corr-123",
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      when(formpProxyConnector.updateGovTalkStatusCorrelationId(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateGovTalkStatusCorrelationId(req).failed.futureValue.getMessage must include("boom")
    }
  }

  "updateGovTalkStatusStatistics" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusStatisticsRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        numPolls = 3,
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      when(formpProxyConnector.updateGovTalkStatusStatistics(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateGovTalkStatusStatistics(req).futureValue

      verify(formpProxyConnector).updateGovTalkStatusStatistics(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup
      import s._

      val req = UpdateGovTalkStatusStatisticsRequest(
        userIdentifier = "instance-123",
        formResultID = "sub-123",
        lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        numPolls = 3,
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      when(formpProxyConnector.updateGovTalkStatusStatistics(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateGovTalkStatusStatistics(req).failed.futureValue.getMessage must include("boom")
    }
  }

  "processInitialChrisAck" - {

    "initialises govtalk, saves session, and runs govtalk update steps" in {
      val s = setup
      import s._

      val employerRef         = EmployerReference("123", "AB456")
      val submissionId        = "sub-123"
      val expectedCorrelation = "corr-123"
      val actualCorrelation   = "corr-123"
      val pollInterval        = 10
      val pollUrl             = "/poll/123"
      val gatewayUrl          = "/gateway"
      val lastMessageDate     = Instant.parse("2025-01-01T00:00:00Z")
      val taxpayer            = mkTaxpayer("instance-123")
      val journey             = ChrisPollJourney.MonthlyReturn
      val context             = MonthlyReturnSubmissionContext(
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = LocalDateTime.of(2025, 1, 1, 0, 0)
      )
      val response            = mkResult(ACCEPTED, corrId = expectedCorrelation)
      val expectedSessionData = ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = "instance-123",
        correlationId = expectedCorrelation,
        lastMessageDate = lastMessageDate,
        numPolls = 0,
        pollInterval = pollInterval,
        pollUrl = pollUrl,
        govTalkStatus = None,
        monthlyReturnContext = context.monthlyReturnContext,
        verificationContext = context.verificationContext
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest("instance-123", submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      when(
        formpProxyConnector.createGovTalkStatusRecord(
          eqTo(CreateGovTalkStatusRecordRequest("instance-123", submissionId, expectedCorrelation, gatewayUrl))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        chrisSubmissionSessionRepository.upsert(eqTo(expectedSessionData))
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusCorrelationId(
          eqTo(
            UpdateGovTalkStatusCorrelationIdRequest(
              userIdentifier = "instance-123",
              formResultID = submissionId,
              correlationID = expectedCorrelation,
              pollInterval = pollInterval,
              gatewayURL = gatewayUrl
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusStatistics(
          eqTo(
            UpdateGovTalkStatusStatisticsRequest(
              userIdentifier = "instance-123",
              formResultID = submissionId,
              lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
              numPolls = 0,
              pollInterval = pollInterval,
              gatewayURL = gatewayUrl
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatus(
          eqTo(UpdateGovTalkStatusRequest("instance-123", submissionId, None, "dataPoll"))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(formPSubmissionUpdateProcessorRegistry.processorFor(eqTo(journey)))
        .thenReturn(formPSubmissionUpdateProcessor)

      when(
        formPSubmissionUpdateProcessor.handleInitialAccepted(eqTo(expectedSessionData), eqTo(response))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.unit)

      service
        .processInitialChrisAck(
          employerRef,
          submissionId,
          expectedCorrelation,
          actualCorrelation,
          pollInterval,
          pollUrl,
          gatewayUrl,
          lastMessageDate,
          journey,
          context,
          response,
          isResubmission = false
        )
        .futureValue mustBe ()
    }

    "fails when correlationId does not match" in {
      val s = setup
      import s._

      service
        .processInitialChrisAck(
          EmployerReference("123", "AB456"),
          "sub-123",
          "corr-expected",
          "corr-actual",
          10,
          "/poll/123",
          "/gateway",
          Instant.parse("2025-01-01T00:00:00Z"),
          ChrisPollJourney.MonthlyReturn,
          MonthlyReturnSubmissionContext(
            hmrcMarkGenerated = "hmrc-mark",
            submissionRequestDate = LocalDateTime.of(2025, 1, 1, 0, 0)
          ),
          mkResult(ACCEPTED, corrId = "corr-actual"),
          isResubmission = false
        )
        .failed
        .futureValue
        .getMessage must include("CorrelationId mismatch")

      verifyNoInteractions(formpProxyConnector)
      verifyNoInteractions(chrisSubmissionSessionRepository)
    }
  }

  "processInitialChrisFailure" - {

    "initialises govtalk and updates status to dataRequest" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("123", "AB456")
      val submissionId  = "sub-123"
      val correlationId = "corr-123"
      val gatewayUrl    = "/gateway"
      val taxpayer      = mkTaxpayer("instance-123")

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest("instance-123", submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      when(
        formpProxyConnector.createGovTalkStatusRecord(
          eqTo(CreateGovTalkStatusRecordRequest("instance-123", submissionId, correlationId, gatewayUrl))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatus(
          eqTo(UpdateGovTalkStatusRequest("instance-123", submissionId, None, "dataRequest"))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      service.processInitialChrisFailure(employerRef, submissionId, correlationId, gatewayUrl).futureValue mustBe ()
    }
  }

  "initialiseGovTalkStatus" - {

    "returns instanceId when taxpayer exists and no GovTalk status record exists" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer()

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
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      when(formpProxyConnector.createGovTalkStatusRecord(eqTo(expectedCreateReq))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl
        )
        .futureValue mustBe taxpayer.uniqueId

      verify(monthlyReturnService).getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier])

      verify(formpProxyConnector).getGovTalkStatus(
        eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
        eqTo(Initial)
      )(any[HeaderCarrier])

      verify(formpProxyConnector).createGovTalkStatusRecord(eqTo(expectedCreateReq))(any[HeaderCarrier])

      verify(formpProxyConnector, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(
        any[HeaderCarrier]
      )

      verifyNoInteractions(chrisConnector)
      verifyNoInteractions(emailConnector)
    }

    "fails when GovTalk status record already exists and this is not a resubmission" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer()

      val existing = GetGovTalkStatusResponse(
        govtalk_status = Seq(
          existingGovTalkStatus(
            userIdentifier = taxpayer.uniqueId,
            formResultID = submissionId
          )
        )
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(existing)))

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl
        )
        .failed
        .futureValue
        .getMessage mustBe "govtalk status already exists"

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )

      verify(formpProxyConnector, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(
        any[HeaderCarrier]
      )
    }

    "fails when FormP returns an empty GovTalk status response and this is not a resubmission" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer("instance-123")

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          Some(GetGovTalkStatusResponse(govtalk_status = Seq.empty))
        )
      )

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl
        )
        .failed
        .futureValue
        .getMessage mustBe "govtalk status already exists"

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )

      verify(formpProxyConnector, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(
        any[HeaderCarrier]
      )
    }

    "resets existing GovTalk status when this is a resubmission" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer("instance-123")

      val existing = GetGovTalkStatusResponse(
        govtalk_status = Seq(
          existingGovTalkStatus(
            userIdentifier = taxpayer.uniqueId,
            formResultID = submissionId
          )
        )
      )

      val expectedResetReq = ResetGovTalkStatusRequest(
        userIdentifier = taxpayer.uniqueId,
        formResultID = submissionId,
        oldProtocolStatus = "dataRequest",
        gatewayURL = gatewayUrl
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(existing)))

      when(formpProxyConnector.resetGovTalkStatus(eqTo(expectedResetReq))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl,
          isResubmission = true
        )
        .futureValue mustBe taxpayer.uniqueId

      verify(formpProxyConnector).resetGovTalkStatus(eqTo(expectedResetReq))(any[HeaderCarrier])

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )
    }

    "fails when this is a resubmission but no GovTalk status record exists" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer("instance-123")

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(None))

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl,
          isResubmission = true
        )
        .failed
        .futureValue
        .getMessage mustBe "Expected govTalkStatus record for resubmission but none found"

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )

      verify(formpProxyConnector, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(
        any[HeaderCarrier]
      )
    }

    "fails when FormP returns an empty GovTalk status response for resubmission" in {
      val s = setup
      import s._

      val employerRef   = EmployerReference("", "")
      val submissionId  = "sub-123"
      val correlationId = "CORR-123"
      val gatewayUrl    = chrisGatewayUrl

      val taxpayer = mkTaxpayer("instance-123")

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(taxpayer.uniqueId, submissionId)),
          eqTo(Initial)
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(
          Some(GetGovTalkStatusResponse(govtalk_status = Seq.empty))
        )
      )

      service
        .initialiseGovTalkStatus(
          employerRef,
          submissionId,
          correlationId,
          gatewayUrl,
          isResubmission = true
        )
        .failed
        .futureValue
        .getMessage mustBe "Expected govTalkStatus record for resubmission but none found"

      verify(formpProxyConnector, never()).createGovTalkStatusRecord(any[CreateGovTalkStatusRecordRequest])(
        any[HeaderCarrier]
      )

      verify(formpProxyConnector, never()).resetGovTalkStatus(any[ResetGovTalkStatusRequest])(
        any[HeaderCarrier]
      )
    }
  }

  "getSubmissionsToPoll" - {

    "must return submissions from FormpProxyConnector" in new Setup {
      val response =
        GetBatchPollSubmissionsResponse(
          verificationSubmissions = Seq.empty,
          monthlyReturnSubmissions = Seq.empty
        )

      when(formpProxyConnector.getBatchPollSubmissions())
        .thenReturn(Future.successful(response))

      service.getSubmissionsToPoll().futureValue mustBe response

      verify(formpProxyConnector).getBatchPollSubmissions()
    }

    "must propagate error from FormpProxyConnector" in new Setup {
      val exception = new RuntimeException("formp-proxy failed")

      when(formpProxyConnector.getBatchPollSubmissions())
        .thenReturn(Future.failed(exception))

      service.getSubmissionsToPoll().failed.futureValue mustBe exception

      verify(formpProxyConnector).getBatchPollSubmissions()
    }
  }

  trait Setup {
    val chrisConnector: ChrisConnector                                     = mock[ChrisConnector]
    val formpProxyConnector: FormpProxyConnector                           = mock[FormpProxyConnector]
    val emailConnector: EmailConnector                                     = mock[EmailConnector]
    val monthlyReturnService: MonthlyReturnService                         = mock[MonthlyReturnService]
    val chrisSubmissionSessionRepository: ChrisSubmissionSessionRepository = mock[ChrisSubmissionSessionRepository]
    val appConfig: AppConfig                                               = mock[AppConfig]

    val chrisGatewayUrl                                                                = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
    val formPSubmissionUpdateProcessorRegistry: FormPSubmissionUpdateProcessorRegistry =
      mock[FormPSubmissionUpdateProcessorRegistry]
    val formPSubmissionUpdateProcessor: FormPSubmissionUpdateProcessor                 =
      mock[FormPSubmissionUpdateProcessor]

    val service = new SubmissionService(
      chrisConnector,
      formpProxyConnector,
      emailConnector,
      monthlyReturnService,
      chrisSubmissionSessionRepository,
      formPSubmissionUpdateProcessorRegistry,
      appConfig
    )

    when(appConfig.chrisGatewayUrl)
      .thenReturn(chrisGatewayUrl)

    def mkPayload(
      corrId: String = "CID-123",
      envelope: Elem = <GovTalkMessage/>
    ): ChRISSubmission =
      ChRISSubmission(envelope = envelope, correlationId = corrId, irMark = "IRMK", irEnvelope = envelope)

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

  "getSubmissionWithVerificationBatch" - {

    "must call FormpProxyConnector with instanceId and verificationBatchResourceRef" in new Setup {
      val submission =
        VerificationSubmissionToPoll(
          submissionId = 90001L,
          submissionType = "CISVERIFY",
          agentId = Some("A123456"),
          taxOfficeNumber = "123",
          taxOfficeReference = "ABC123",
          instanceId = "instance-verification-001",
          status = "SUBMITTED",
          verificationBatchResourceRef = 70001L
        )

      val response =
        GetSubmissionWithVerificationBatchResponse(
          scheme = None,
          subcontractors = Seq.empty,
          verifications = Seq.empty,
          verificationBatch = None,
          submission = None
        )

      when(
        formpProxyConnector.getSubmissionWithVerificationBatch(
          submission.instanceId,
          submission.verificationBatchResourceRef
        )
      ).thenReturn(Future.successful(response))

      service.getSubmissionWithVerificationBatch(submission).futureValue mustBe response

      verify(formpProxyConnector).getSubmissionWithVerificationBatch(
        submission.instanceId,
        submission.verificationBatchResourceRef
      )
    }
  }

}
