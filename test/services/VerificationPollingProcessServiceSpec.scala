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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.Verification
import uk.gov.hmrc.constructionindustryscheme.models.{BatchChRISPollResult, PollReportContent, SUBMITTED, SubmissionStatus}
import uk.gov.hmrc.constructionindustryscheme.models.requests.SubcontractorVerificationEmailRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, VerificationSubmissionToPoll}
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.constructionindustryscheme.services.{SubmissionService, VerificationPollingProcessService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class VerificationPollingProcessServiceSpec extends SpecBase {

  "VerificationPollingProcessService process" - {

    "must return PollReportContent and send email for verification submissions" in new Setup {
      val submissions =
        Seq(verificationSubmission)

      when(
        mockSubmissionService
          .syncVerificationSessionForPolling(verificationSubmission)
      ).thenReturn(Future.successful(syncedSession))

      when(
        mockSubmissionService.pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )
      ).thenReturn(
        Future.successful(
          BatchChRISPollResult.Completed(
            pollResponse
          )
        )
      )

      when(
        mockSubmissionService.sendEmailForVerification(
          SubcontractorVerificationEmailRequest(emailRecipient)
        )
      ).thenReturn(Future.unit)

      service.process(submissions).futureValue mustBe Seq(
        expectedReportContent
      )

      verify(mockSubmissionService)
        .syncVerificationSessionForPolling(verificationSubmission)

      verify(mockSubmissionService)
        .pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )

      verify(mockSubmissionService)
        .sendEmailForVerification(
          SubcontractorVerificationEmailRequest(emailRecipient)
        )

      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must not send an email when submission status is not eligible" in new Setup {
      val ineligibleStatus       = mock[SubmissionStatus]
      val ineligiblePollResponse = pollResponse.copy(status = ineligibleStatus)

      when(
        mockSubmissionService
          .syncVerificationSessionForPolling(verificationSubmission)
      ).thenReturn(Future.successful(syncedSession))

      when(
        mockSubmissionService.pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )
      ).thenReturn(
        Future.successful(
          BatchChRISPollResult.Completed(ineligiblePollResponse)
        )
      )

      service.process(Seq(verificationSubmission)).futureValue

      verify(mockSubmissionService)
        .syncVerificationSessionForPolling(verificationSubmission)

      verify(mockSubmissionService)
        .pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )

      verify(mockSubmissionService, never)
        .sendEmailForVerification(any[SubcontractorVerificationEmailRequest])(
          any[HeaderCarrier]
        )

      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must complete successfully when email recipient is missing" in new Setup {
      val sessionWithoutEmail =
        syncedSession.copy(emailRecipient = None)

      when(
        mockSubmissionService
          .syncVerificationSessionForPolling(verificationSubmission)
      ).thenReturn(Future.successful(sessionWithoutEmail))

      when(
        mockSubmissionService.pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )
      ).thenReturn(
        Future.successful(
          BatchChRISPollResult.Completed(pollResponse)
        )
      )

      service.process(Seq(verificationSubmission)).futureValue

      verify(mockSubmissionService)
        .syncVerificationSessionForPolling(verificationSubmission)

      verify(mockSubmissionService)
        .pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )

      verify(mockSubmissionService, never)
        .sendEmailForVerification(any[SubcontractorVerificationEmailRequest])(
          any[HeaderCarrier]
        )

      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must return empty report content for empty verification submissions" in new Setup {
      service.process(Seq.empty).futureValue mustBe Seq.empty

      verifyNoInteractions(mockSubmissionService)
    }

    "must return failed report content when verification polling fails" in new Setup {
      when(
        mockSubmissionService
          .syncVerificationSessionForPolling(verificationSubmission)
      ).thenReturn(
        Future.failed(
          new RuntimeException("verification sync failed")
        )
      )

      val result =
        service
          .process(Seq(verificationSubmission))
          .futureValue

      result mustBe Seq(
        PollReportContent(
          user = verificationSubmission.instanceId,
          submissionType = verificationSubmission.submissionType,
          submissionId = verificationSubmission.submissionId.toString,
          govTalkRequestStatus = verificationSubmission.status,
          currentReturnStatus = "-",
          employerReference = s"${verificationSubmission.taxOfficeNumber}/${verificationSubmission.taxOfficeReference}",
          correlationId = "(not polled)",
          agentId = "A123456"
        )
      )

      verify(mockSubmissionService)
        .syncVerificationSessionForPolling(verificationSubmission)

      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must return poll report content when post-poll processing fails" in new Setup {
      when(
        mockSubmissionService
          .syncVerificationSessionForPolling(verificationSubmission)
      ).thenReturn(Future.successful(syncedSession))

      when(
        mockSubmissionService.pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )
      ).thenReturn(
        Future.successful(
          BatchChRISPollResult.PostProcessingFailed(
            response = pollResponse,
            exception = new RuntimeException("post-poll failed")
          )
        )
      )

      service.process(Seq(verificationSubmission)).futureValue mustBe Seq(
        expectedReportContent
      )

      verify(mockSubmissionService)
        .syncVerificationSessionForPolling(verificationSubmission)

      verify(mockSubmissionService)
        .pollSubmissionAndUpdateGovTalkStatusForBatch(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )

      verifyNoMoreInteractions(mockSubmissionService)
    }
  }

  private trait Setup {
    given ExecutionContext =
      scala.concurrent.ExecutionContext.global

    given HeaderCarrier =
      HeaderCarrier()

    val mockSubmissionService: SubmissionService =
      mock[SubmissionService]

    val service =
      new VerificationPollingProcessService(mockSubmissionService)

    val emailRecipient = "user@example.com"

    val verificationSubmission: VerificationSubmissionToPoll =
      VerificationSubmissionToPoll(
        submissionId = 90001L,
        submissionType = "VERIFICATIONS",
        agentId = Some("A123456"),
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC123",
        instanceId = "instance-verification-001",
        status = "ACCEPTED",
        verificationBatchResourceRef = 70001L
      )

    val chrisSession: ChrisSubmissionSessionData =
      ChrisSubmissionSessionData(
        submissionId = verificationSubmission.submissionId.toString,
        instanceId = verificationSubmission.instanceId,
        correlationId = "corr-123",
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 1,
        pollInterval = 5,
        pollUrl = "http://localhost:6997/submission/ChRIS/poll/IR-CIS-VERIFY/0?final=SUBMITTED",
        govTalkStatus = None
      )

    val syncedSession: SubmissionService.SyncedVerificationSession =
      SubmissionService.SyncedVerificationSession(
        sessionData = chrisSession,
        emailRecipient = Some(emailRecipient)
      )

    val pollResponse: ChrisPollResponse =
      ChrisPollResponse(
        status = SUBMITTED,
        correlationId = chrisSession.correlationId,
        pollUrl = None,
        pollInterval = None,
        error = None,
        irMarkReceived = None,
        lastMessageDate = None,
        acceptedTime = None,
        govTalkErrorStatus = None
      )

    val expectedReportContent: PollReportContent =
      PollReportContent(
        user = verificationSubmission.instanceId,
        submissionType = verificationSubmission.submissionType,
        submissionId = verificationSubmission.submissionId.toString,
        govTalkRequestStatus = verificationSubmission.status,
        currentReturnStatus = "SUBMITTED",
        employerReference = s"${verificationSubmission.taxOfficeNumber}/${verificationSubmission.taxOfficeReference}",
        correlationId = chrisSession.correlationId,
        agentId = "A123456"
      )
  }
}
