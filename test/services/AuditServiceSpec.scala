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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, GovTalkMeta, MonthlyReturnType, ResponseEndPoint, SUBMITTED, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.models.audit.{ClientListRetrievalFailedEvent, ClientListRetrievalInProgressEvent}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisSubmissionRequest, ChrisVerificationRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.services.AuditService
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {
  "AuditService" - {
    "call AuditConnector.sendExtendedEvent for MonthlyReturnRequestEvent with returnType Nil" in {
      val mockAuditConnector = mock[AuditConnector]
      val request            = ChrisSubmissionRequest(
        utr = "1234567890",
        aoReference = "123/AB456",
        monthYear = "2025-05",
        email = None,
        isAgent = false,
        isResubmission = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456",
        returnType = MonthlyReturnType.Nil,
        informationCorrect = "yes",
        inactivity = "no",
        standard = None
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.monthlyReturnRequestEvent(request, "CORR-456", "2025-05-09T10:30:00Z")
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "MonthlyReturnRequest"
      }
    }
    "call AuditConnector.sendExtendedEvent for MonthlyReturnResponseEvent with returnType Nil" in {
      val mockAuditConnector = mock[AuditConnector]
      val submissionResult   = SubmissionResult(
        status = ACCEPTED,
        rawXml = "<xml/>",
        meta = GovTalkMeta(
          qualifier = "response",
          function = "submit",
          className = "IR-CIS-CIS300MR",
          correlationId = "CORR-NIL-789",
          gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
          responseEndPoint = ResponseEndPoint("http://poll.example/", 5),
          error = None,
          acceptedTime = Some("2025-05-09T10:30:01Z")
        )
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.monthlyReturnResponseEvent(submissionResult, "Nil")
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "MonthlyReturnResponse"
      }
    }

    "call AuditConnector.sendExtendedEvent for MonthlyReturnRequestEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val request            = ChrisSubmissionRequest(
        utr = "1234567890",
        aoReference = "123/AB456",
        monthYear = "2025-05",
        email = None,
        isAgent = false,
        isResubmission = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "ABC456",
        returnType = MonthlyReturnType.Standard,
        informationCorrect = "yes",
        inactivity = "no",
        standard = None
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.monthlyReturnRequestEvent(request, "CORR-123", "2025-05-09T10:30:00Z")
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "MonthlyReturnRequest"
      }
    }
    "call AuditConnector.sendExtendedEvent for MonthlyReturnResponseEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val submissionResult   = SubmissionResult(
        status = ACCEPTED,
        rawXml = "<xml/>",
        meta = GovTalkMeta(
          qualifier = "response",
          function = "submit",
          className = "IR-CIS-CIS300MR",
          correlationId = "CORR-STD-123",
          gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
          responseEndPoint = ResponseEndPoint("http://poll.example/", 5),
          error = None,
          acceptedTime = Some("2025-05-09T10:30:01Z")
        )
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.monthlyReturnResponseEvent(submissionResult, "Standard")
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "MonthlyReturnResponse"
      }
    }

    "call AuditConnector.sendExtendedEvent for VerificationRequestEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val request            = ChrisVerificationRequest(
        instanceId = "inst-001",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456",
        contractorUTR = "1234567890",
        contractorAORef = "123/AB456",
        verificationBatchId = "BATCH-001",
        verificationBatchResourceRef = "ref-001",
        emailRecipient = None,
        subcontractors = Seq.empty,
        verifications = Seq.empty
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.verificationRequestEvent(request, "CORR-VER-123")
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "VerificationRequest"
      }
    }

    "call AuditConnector.sendExtendedEvent for VerificationResponseEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val submissionResult   = SubmissionResult(
        status = SUBMITTED,
        rawXml = "<xml/>",
        meta = GovTalkMeta(
          qualifier = "response",
          function = "submit",
          className = "IR-CIS-CISV",
          correlationId = "CORR-VER-789",
          gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
          responseEndPoint = ResponseEndPoint("http://poll.example/", 5),
          error = None,
          acceptedTime = None
        )
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.verificationResponseEvent(submissionResult)
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "VerificationResponse"
      }
    }

    "call AuditConnector.sendExtendedEvent for MonthlyReturnPollResponseEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val pollResponse       = ChrisPollResponse(
        status = ACCEPTED,
        correlationId = "CORR-POLL-123",
        pollUrl = Some("http://poll.example/123"),
        pollInterval = Some(10),
        error = None,
        irMarkReceived = None,
        lastMessageDate = None,
        acceptedTime = Some("2025-05-09T10:30:01Z")
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.monthlyReturnPollResponseEvent(pollResponse)
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "MonthlyReturnPollResponse"
      }
    }

    "call AuditConnector.sendExtendedEvent for VerificationPollResponseEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val pollResponse       = ChrisPollResponse(
        status = ACCEPTED,
        correlationId = "CORR-VPOLL-123",
        pollUrl = Some("http://poll.example/ver/123"),
        pollInterval = Some(10),
        error = None,
        irMarkReceived = None,
        lastMessageDate = None,
        acceptedTime = Some("2025-05-09T10:30:01Z")
      )
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service            = new AuditService(mockAuditConnector)
      val resultF            = service.verificationPollResponseEvent(pollResponse)
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor        = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe "VerificationPollResponse"
      }
    }

    "call AuditConnector.sendExtendedEvent for ClientListRetrievalFailedEvent" in {
      val mockAuditConnector = mock[AuditConnector]

      val credentialId = "cred-123"
      val phase        = "business#1"
      val reason       = Some("no-business-intervals")

      val expectedEvent =
        ClientListRetrievalFailedEvent(credentialId, phase, reason).extendedDataEvent

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val service = new AuditService(mockAuditConnector)
      val resultF = service.clientListRetrievalFailed(credentialId, phase, reason)

      resultF.map { result =>
        result shouldBe AuditResult.Success

        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail    shouldBe expectedEvent.detail
      }
    }

    "call AuditConnector.sendExtendedEvent for ClientListRetrievalInProgressEvent" in {
      val mockAuditConnector = mock[AuditConnector]

      val credentialId = "cred-456"
      val phase        = "browser"

      val expectedEvent =
        ClientListRetrievalInProgressEvent(credentialId, phase).extendedDataEvent

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val service = new AuditService(mockAuditConnector)
      val resultF = service.clientListRetrievalInProgress(credentialId, phase)

      resultF.map { result =>
        result shouldBe AuditResult.Success

        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail    shouldBe expectedEvent.detail
      }
    }
  }
}
