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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.services.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BatchPollerServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "BatchPollerService run" - {

    "must call SubmissionService and complete successfully when submissions are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(nonEmptyResponse))

      when(mockMonthlyReturnPollingProcessService.process(any())(any())).thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verifyNoInteractions(mockGeneratePollReportService)
      verify(mockMonthlyReturnPollingProcessService).process(any())(any())
      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must call GeneratePollReportService when empty submission lists are returned" in new Setup {

      when(mockSubmissionService.getSubmissionsToPoll()(using hc)).thenReturn(Future.successful(emptyResponse))

      when(
        mockGeneratePollReportService.generatePollReport(
          Seq.empty[PollReportContent]
        )
      ).thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockGeneratePollReportService).generatePollReport(Seq.empty[PollReportContent])
      verifyNoMoreInteractions(mockSubmissionService, mockGeneratePollReportService)
      verifyNoInteractions(mockMonthlyReturnPollingProcessService)
    }

    "must return unit without calling GeneratePollReportService when only verification submissions exist" in new Setup {
      val verificationOnlyResponse =
        GetBatchPollSubmissionsResponse(
          verificationSubmissions = Seq(verificationSubmission),
          monthlyReturnSubmissions = Seq.empty
        )

      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(verificationOnlyResponse))

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verifyNoInteractions(mockGeneratePollReportService)
      verifyNoInteractions(mockMonthlyReturnPollingProcessService)
      verifyNoMoreInteractions(mockSubmissionService)
    }

    "must recover and complete when SubmissionService fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.failed(new RuntimeException("formp-proxy failed")))

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verifyNoInteractions(mockGeneratePollReportService)
      verifyNoMoreInteractions(mockSubmissionService)
      verifyNoMoreInteractions(mockMonthlyReturnPollingProcessService)
    }
  }

  private trait Setup {
    given ExecutionContext  = ExecutionContext.global
    given hc: HeaderCarrier = HeaderCarrier()

    val mockSubmissionService: SubmissionService =
      mock[SubmissionService]

    val mockGeneratePollReportService: GeneratePollReportService =
      mock[GeneratePollReportService]

    val mockMonthlyReturnPollingProcessService: MonthlyReturnPollingProcessService =
      mock[MonthlyReturnPollingProcessService]

    val service = new BatchPollerService(
      submissionService = mockSubmissionService,
      generatePollReportService = mockGeneratePollReportService,
      monthlyReturnPollingProcessService = mockMonthlyReturnPollingProcessService
    )

    val verificationSubmission: VerificationSubmissionToPoll =
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

    val monthlyReturnSubmission: MonthlyReturnSubmissionToPoll =
      MonthlyReturnSubmissionToPoll(
        submissionId = 90002L,
        submissionType = "CIS300MR",
        status = "SUBMITTED",
        taxOfficeNumber = "123",
        taxOfficeReference = "456789",
        taxYear = 2025,
        taxMonth = 6,
        instanceId = "instance-monthly-return-001",
        agentId = Some("A123456")
      )

    val nonEmptyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(verificationSubmission),
        monthlyReturnSubmissions = Seq(monthlyReturnSubmission)
      )

    val emptyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq.empty,
        monthlyReturnSubmissions = Seq.empty
      )
  }
}
