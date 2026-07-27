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
import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.services.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class BatchPollerServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "BatchPollerService run" - {

    val startTime =
      System.currentTimeMillis()

    "must call GeneratePollReportService with empty report when empty submission lists are returned" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(emptyResponse))

      when(
        mockGeneratePollReportService.generatePollReport(
          Seq.empty[PollReportContent]
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockGeneratePollReportService)
        .generatePollReport(Seq.empty[PollReportContent])

      verifyNoInteractions(mockMonthlyReturnPollingProcessService)
      verifyNoInteractions(mockVerificationPollingProcessService)
    }

    "must process monthly return submissions and call GeneratePollReportService with monthly report content" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(monthlyReturnOnlyResponse))

      when(
        mockMonthlyReturnPollingProcessService.process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)
      ).thenReturn(
        Future.successful(monthlyReturnReportContent)
      )

      when(
        mockGeneratePollReportService.generatePollReport(
          monthlyReturnReportContent
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockMonthlyReturnPollingProcessService)
        .process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)

      verifyNoInteractions(mockVerificationPollingProcessService)

      verify(mockGeneratePollReportService)
        .generatePollReport(monthlyReturnReportContent)
    }

    "must process verification submissions and call GeneratePollReportService with verification report content" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(verificationOnlyResponse))

      when(
        mockVerificationPollingProcessService.process(
          Seq(verificationSubmission)
        )(using hc)
      ).thenReturn(
        Future.successful(verificationReportContent)
      )

      when(
        mockGeneratePollReportService.generatePollReport(
          verificationReportContent
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockVerificationPollingProcessService)
        .process(Seq(verificationSubmission))(using hc)

      verifyNoInteractions(mockMonthlyReturnPollingProcessService)

      verify(mockGeneratePollReportService)
        .generatePollReport(verificationReportContent)
    }

    "must process verification and monthly return submissions and call GeneratePollReportService with combined report content" in new Setup {
      val combinedReportContent =
        verificationReportContent ++ monthlyReturnReportContent

      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(nonEmptyResponse))

      when(
        mockVerificationPollingProcessService.process(
          Seq(verificationSubmission)
        )(using hc)
      ).thenReturn(
        Future.successful(verificationReportContent)
      )

      when(
        mockMonthlyReturnPollingProcessService.process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)
      ).thenReturn(
        Future.successful(monthlyReturnReportContent)
      )

      when(
        mockGeneratePollReportService.generatePollReport(
          combinedReportContent
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockVerificationPollingProcessService)
        .process(Seq(verificationSubmission))(using hc)

      verify(mockMonthlyReturnPollingProcessService)
        .process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)

      verify(mockGeneratePollReportService)
        .generatePollReport(combinedReportContent)
    }

    "must recover and complete when SubmissionService fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(
          Future.failed(
            new RuntimeException("formp-proxy failed")
          )
        )

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verifyNoInteractions(mockMonthlyReturnPollingProcessService)
      verifyNoInteractions(mockVerificationPollingProcessService)
      verifyNoInteractions(mockGeneratePollReportService)
    }

    "must recover and complete when verification polling fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(verificationOnlyResponse))

      when(
        mockVerificationPollingProcessService.process(
          Seq(verificationSubmission)
        )(using hc)
      ).thenReturn(
        Future.failed(
          new RuntimeException("verification polling failed")
        )
      )

      when(
        mockGeneratePollReportService.generatePollReport(
          Seq.empty[PollReportContent]
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockVerificationPollingProcessService)
        .process(Seq(verificationSubmission))(using hc)

      verifyNoInteractions(mockMonthlyReturnPollingProcessService)

      verify(mockGeneratePollReportService)
        .generatePollReport(Seq.empty[PollReportContent])
    }

    "must recover and complete when monthly return polling fails" in new Setup {
      when(mockSubmissionService.getSubmissionsToPoll()(using hc))
        .thenReturn(Future.successful(monthlyReturnOnlyResponse))

      when(
        mockMonthlyReturnPollingProcessService.process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)
      ).thenReturn(
        Future.failed(
          new RuntimeException("monthly polling failed")
        )
      )

      when(
        mockGeneratePollReportService.generatePollReport(
          Seq.empty[PollReportContent]
        )
      ).thenReturn(Future.unit)

      service.run(startTime).futureValue mustBe ()

      verify(mockSubmissionService)
        .getSubmissionsToPoll()(using hc)

      verify(mockMonthlyReturnPollingProcessService)
        .process(
          Seq(monthlyReturnSubmission),
          startTime
        )(using hc)

      verifyNoInteractions(mockVerificationPollingProcessService)

      verify(mockGeneratePollReportService)
        .generatePollReport(Seq.empty[PollReportContent])
    }
  }

  private trait Setup {

    given ExecutionContext =
      ExecutionContext.global

    given hc: HeaderCarrier =
      HeaderCarrier()

    val mockSubmissionService: SubmissionService =
      mock[SubmissionService]

    val mockGeneratePollReportService: GeneratePollReportService =
      mock[GeneratePollReportService]

    val mockVerificationPollingProcessService: VerificationPollingProcessService =
      mock[VerificationPollingProcessService]

    val mockMonthlyReturnPollingProcessService: MonthlyReturnPollingProcessService =
      mock[MonthlyReturnPollingProcessService]

    val service =
      new BatchPollerService(
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

    val verificationReportContent: Seq[PollReportContent] =
      Seq(
        PollReportContent(
          user = "instance-verification-001",
          submissionType = "VERIFICATIONS",
          submissionId = "90001",
          govTalkRequestStatus = "ACCEPTED",
          currentReturnStatus = "ACCEPTED",
          employerReference = "123/ABC123",
          correlationId = "verification-correlation-id-001",
          agentId = "A123456"
        )
      )

    val monthlyReturnReportContent: Seq[PollReportContent] =
      Seq(
        PollReportContent(
          user = "instance-monthly-return-001",
          submissionType = "MONTHLY_RETURN",
          submissionId = "90002",
          govTalkRequestStatus = "ACCEPTED",
          currentReturnStatus = "ACCEPTED",
          employerReference = "123/456789",
          correlationId = "monthly-correlation-id-001",
          agentId = "A123456"
        )
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
