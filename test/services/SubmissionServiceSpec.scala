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
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, GetGovTalkStatusResponse}
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.constructionindustryscheme.services.{ChrisSubmissionSessionStore, MonthlyReturnService, SubmissionService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime}
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

      val updatedSession = session.copy(
        lastMessageDate = Instant.parse("2025-01-02T00:00:00Z"),
        numPolls = 1,
        pollInterval = 20,
        pollUrl = "/poll/999"
      )

      val govTalk = GetGovTalkStatusResponse(
        govtalk_status = Seq.empty
      )

      val pollResponse = ChrisPollResponse(
        status = SUBMITTED,
        correlationId = correlation,
        pollUrl = Some("/poll/999"),
        pollInterval = Some(20),
        lastMessageDate = Some("2025-01-02T00:00:00Z")
      )

      when(chrisSubmissionSessionStore.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))
        .thenReturn(Future.successful(Some(updatedSession)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(govTalk)))
        .thenReturn(Future.successful(Some(govTalk)))

      when(chrisSubmissionSessionStore.saveGovTalkStatus(eqTo(submissionId), eqTo(govTalk)))
        .thenReturn(Future.unit)

      when(chrisConnector.pollSubmission(eqTo(correlation), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(pollResponse))

      when(
        chrisSubmissionSessionStore.updateAfterPoll(
          eqTo(submissionId),
          eqTo(correlation),
          eqTo(Instant.parse("2025-01-02T00:00:00Z")),
          eqTo(20),
          eqTo("/poll/999")
        )
      ).thenReturn(Future.unit)

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
              protocolStatus = "dataPoll"
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      service.pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl).futureValue mustBe pollResponse
    }

    "fails when no session exists" in {
      val s = setup
      import s._

      when(chrisSubmissionSessionStore.get(eqTo("sub-123")))
        .thenReturn(Future.successful(None))

      service
        .pollSubmissionAndUpdateGovTalkStatus("sub-123", "/poll/123")
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

      val pollResponse = ChrisPollResponse(
        status = SUBMITTED,
        correlationId = "corr-actual",
        pollUrl = Some("/poll/999"),
        pollInterval = Some(20),
        lastMessageDate = Some("2025-01-02T00:00:00Z")
      )

      when(chrisSubmissionSessionStore.get(eqTo(submissionId)))
        .thenReturn(Future.successful(Some(session)))

      when(
        formpProxyConnector.getGovTalkStatus(
          eqTo(GetGovTalkStatusRequest(instanceId, submissionId))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(Some(govTalk)))

      when(chrisSubmissionSessionStore.saveGovTalkStatus(eqTo(submissionId), eqTo(govTalk)))
        .thenReturn(Future.unit)

      when(chrisConnector.pollSubmission(eqTo("corr-expected"), eqTo(pollUrl))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(pollResponse))

      service
        .pollSubmissionAndUpdateGovTalkStatus(submissionId, pollUrl)
        .failed
        .futureValue
        .getMessage must include("CorrelationId mismatch")
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

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      when(
        formpProxyConnector.getGovTalkStatus(eqTo(GetGovTalkStatusRequest("instance-123", submissionId)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(None))

      when(
        formpProxyConnector.createGovTalkStatusRecord(
          eqTo(CreateGovTalkStatusRecordRequest("instance-123", submissionId, expectedCorrelation, gatewayUrl))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        chrisSubmissionSessionStore.saveInitialAck(
          eqTo(submissionId),
          eqTo("instance-123"),
          eqTo(expectedCorrelation),
          eqTo(pollInterval),
          eqTo(pollUrl),
          eqTo(lastMessageDate)
        )
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusCorrelationId(
          eqTo(
            UpdateGovTalkStatusCorrelationIdRequest(
              "instance-123",
              submissionId,
              expectedCorrelation,
              pollInterval,
              gatewayUrl
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatusStatistics(
          eqTo(
            UpdateGovTalkStatusStatisticsRequest(
              "instance-123",
              submissionId,
              LocalDateTime.of(2025, 1, 1, 0, 0),
              0,
              pollInterval,
              gatewayUrl
            )
          )
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      when(
        formpProxyConnector.updateGovTalkStatus(
          eqTo(UpdateGovTalkStatusRequest("instance-123", submissionId, None, "dataPoll"))
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      service
        .processInitialChrisAck(
          employerRef,
          submissionId,
          expectedCorrelation,
          actualCorrelation,
          pollInterval,
          pollUrl,
          gatewayUrl,
          lastMessageDate
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
          "/gateway"
        )
        .failed
        .futureValue
        .getMessage must include("CorrelationId mismatch")
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
        formpProxyConnector.getGovTalkStatus(eqTo(GetGovTalkStatusRequest("instance-123", submissionId)))(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(None))

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
    val chrisConnector: ChrisConnector                           = mock[ChrisConnector]
    val formpProxyConnector: FormpProxyConnector                 = mock[FormpProxyConnector]
    val emailConnector: EmailConnector                           = mock[EmailConnector]
    val monthlyReturnService: MonthlyReturnService               = mock[MonthlyReturnService]
    val chrisSubmissionSessionStore: ChrisSubmissionSessionStore = mock[ChrisSubmissionSessionStore]

    val service = new SubmissionService(
      chrisConnector,
      formpProxyConnector,
      emailConnector,
      monthlyReturnService,
      chrisSubmissionSessionStore
    )

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
