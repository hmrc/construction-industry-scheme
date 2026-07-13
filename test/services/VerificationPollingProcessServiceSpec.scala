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
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetSubmissionWithVerificationBatchResponse, VerificationSubmissionToPoll}
import uk.gov.hmrc.constructionindustryscheme.services.{SubmissionService, VerificationPollingProcessService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class VerificationPollingProcessServiceSpec extends SpecBase {

  "VerificationPollingProcessService process" - {

    "must call SubmissionService for each verification submission" in new Setup {
      when(submissionService.getSubmissionWithVerificationBatch(verificationSubmission1))
        .thenReturn(Future.successful(response))

      when(submissionService.getSubmissionWithVerificationBatch(verificationSubmission2))
        .thenReturn(Future.successful(response))

      service.process(Seq(verificationSubmission1, verificationSubmission2)).futureValue mustBe ()

      verify(submissionService).getSubmissionWithVerificationBatch(verificationSubmission1)
      verify(submissionService).getSubmissionWithVerificationBatch(verificationSubmission2)
    }
  }

  private trait Setup {
    implicit val hc: HeaderCarrier    = HeaderCarrier()
    implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

    val submissionService: SubmissionService =
      mock[SubmissionService]

    val service =
      new VerificationPollingProcessService(submissionService)

    val verificationSubmission1 =
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

    val verificationSubmission2 =
      verificationSubmission1.copy(
        submissionId = 90002L,
        instanceId = "instance-verification-002",
        verificationBatchResourceRef = 70002L
      )

    val response =
      GetSubmissionWithVerificationBatchResponse(
        scheme = None,
        subcontractors = Seq.empty,
        verifications = Seq.empty,
        verificationBatch = None,
        submission = None
      )
  }
}
