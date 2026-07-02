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

import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.Verification
import uk.gov.hmrc.constructionindustryscheme.models.response.VerificationSubmissionToPoll
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.constructionindustryscheme.services.{SubmissionService, VerificationPollingProcessService}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}

class VerificationPollingProcessServiceSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar {

  "VerificationPollingProcessService process" - {

    "must complete successfully for verification submissions" in new Setup {
      val submissions: Seq[VerificationSubmissionToPoll] = Seq(verificationSubmission)

      when(
        mockSubmissionService.syncChrisSessionFromPollingGovTalkStatus(
          verificationSubmission.instanceId,
          verificationSubmission.submissionId.toString
        )
      ).thenReturn(Future.successful(chrisSession))

      when(
        mockSubmissionService.pollSubmissionAndUpdateGovTalkStatus(
          verificationSubmission.submissionId.toString,
          chrisSession.pollUrl,
          Verification
        )
      ).thenReturn(Future.unit)

      service.process(submissions).futureValue mustBe ()

      verify(mockSubmissionService).syncChrisSessionFromPollingGovTalkStatus(
        verificationSubmission.instanceId,
        verificationSubmission.submissionId.toString
      )

      verify(mockSubmissionService).pollSubmissionAndUpdateGovTalkStatus(
        verificationSubmission.submissionId.toString,
        chrisSession.pollUrl,
        Verification
      )

      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must complete successfully for empty verification submissions" in new Setup {
      service.process(Seq.empty).futureValue mustBe ()

      verifyNoInteractions(mockSubmissionService)
    }
  }

  private trait Setup {
    given ExecutionContext = scala.concurrent.ExecutionContext.global
    given HeaderCarrier    = HeaderCarrier()

    val mockSubmissionService: SubmissionService =
      mock[SubmissionService]

    val service =
      new VerificationPollingProcessService(mockSubmissionService)

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
  }
}
