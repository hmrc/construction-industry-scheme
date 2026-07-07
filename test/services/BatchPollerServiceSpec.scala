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
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.services.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}


class BatchPollerServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "BatchPollerService run" - {

    "must call both polling process services and complete successfully when both submission types are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(nonEmptyResponse))

      when(mockVerificationPollingProcessService.process(Seq(verificationSubmission))(using hc))
        .thenReturn(Future.unit)

      when(mockMonthlyReturnPollingProcessService.process(Seq(monthlyReturnSubmission))(using hc))
        .thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockVerificationPollingProcessService).process(Seq(verificationSubmission))(using hc)
      verify(mockMonthlyReturnPollingProcessService).process(Seq(monthlyReturnSubmission))(using hc)

      verifyNoInteractions(mockGeneratePollReportService)

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must call SubmissionService and complete successfully when empty lists are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(emptyResponse))

      when(mockGeneratePollReportService.generatePollReport()(using hc))
        .thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockGeneratePollReportService).generatePollReport()(using hc)

      verifyNoInteractions(
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService
      )

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must recover and complete successfully when SubmissionService fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.failed(new RuntimeException("formp-proxy failed")))

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)

      verifyNoInteractions(
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must only call VerificationPollingProcessService when only verification submissions are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(verificationOnlyResponse))

      when(mockVerificationPollingProcessService.process(Seq(verificationSubmission))(using hc))
        .thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockVerificationPollingProcessService).process(Seq(verificationSubmission))(using hc)

      verifyNoInteractions(
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must only call MonthlyReturnPollingProcessService when only monthly return submissions are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(monthlyReturnOnlyResponse))

      when(mockMonthlyReturnPollingProcessService.process(Seq(monthlyReturnSubmission))(using hc))
        .thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockMonthlyReturnPollingProcessService).process(Seq(monthlyReturnSubmission))(using hc)

      verifyNoInteractions(
        mockVerificationPollingProcessService,
        mockGeneratePollReportService
      )

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must attempt monthly return polling when verification polling fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(nonEmptyResponse))

      when(mockVerificationPollingProcessService.process(Seq(verificationSubmission))(using hc))
        .thenReturn(Future.failed(new RuntimeException("verification polling failed")))

      when(mockMonthlyReturnPollingProcessService.process(Seq(monthlyReturnSubmission))(using hc))
        .thenReturn(Future.unit)

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockVerificationPollingProcessService).process(Seq(verificationSubmission))(using hc)
      verify(mockMonthlyReturnPollingProcessService).process(Seq(monthlyReturnSubmission))(using hc)

      verifyNoInteractions(mockGeneratePollReportService)

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }

    "must complete successfully when monthly return polling fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(nonEmptyResponse))

      when(mockVerificationPollingProcessService.process(Seq(verificationSubmission))(using hc))
        .thenReturn(Future.unit)

      when(mockMonthlyReturnPollingProcessService.process(Seq(monthlyReturnSubmission))(using hc))
        .thenReturn(Future.failed(new RuntimeException("monthly return polling failed")))

      service.run().futureValue mustBe ()

      verify(mockSubmissionService).getSubmissionsToPoll()(using hc)
      verify(mockVerificationPollingProcessService).process(Seq(verificationSubmission))(using hc)
      verify(mockMonthlyReturnPollingProcessService).process(Seq(monthlyReturnSubmission))(using hc)

      verifyNoInteractions(mockGeneratePollReportService)

      verifyNoMoreInteractions(
        mockSubmissionService,
        mockVerificationPollingProcessService,
        mockMonthlyReturnPollingProcessService,
        mockGeneratePollReportService
      )
    }
  }

  private trait Setup {
    given ExecutionContext  = scala.concurrent.ExecutionContext.global
    given hc: HeaderCarrier = HeaderCarrier()

    val mockSubmissionService: SubmissionService =
      mock[SubmissionService]

    val mockVerificationPollingProcessService: VerificationPollingProcessService =
      mock[VerificationPollingProcessService]

    val mockMonthlyReturnPollingProcessService: MonthlyReturnPollingProcessService =
      mock[MonthlyReturnPollingProcessService]

    val mockGeneratePollReportService: GeneratePollReportService =
      mock[GeneratePollReportService]

    val service = new BatchPollerService(
      submissionService = mockSubmissionService,
      verificationPollingProcessService = mockVerificationPollingProcessService,
      monthlyReturnPollingProcessService = mockMonthlyReturnPollingProcessService,
      generatePollReportService = mockGeneratePollReportService
    )

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

    val monthlyReturnSubmission: MonthlyReturnSubmissionToPoll =
      MonthlyReturnSubmissionToPoll(
        submissionId = 90002L,
        submissionType = "MONTHLY_RETURN",
        status = "ACCEPTED",
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

    val verificationOnlyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(verificationSubmission),
        monthlyReturnSubmissions = Seq.empty
      )

    val monthlyReturnOnlyResponse: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq.empty,
        monthlyReturnSubmissions = Seq(monthlyReturnSubmission)
      )
  }
}
