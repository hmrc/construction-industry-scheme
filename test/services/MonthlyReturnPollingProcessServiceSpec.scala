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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, verifyNoInteractions, when}
import uk.gov.hmrc.constructionindustryscheme.models.Submission
import uk.gov.hmrc.constructionindustryscheme.models.requests.GetMonthlyReturnForEditRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetMonthlyReturnForEditResponse, MonthlyReturnSubmissionToPoll}
import uk.gov.hmrc.constructionindustryscheme.services.{MonthlyReturnPollingProcessService, MonthlyReturnService, SubmissionService}

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future

class MonthlyReturnPollingProcessServiceSpec extends SpecBase {
  private val monthlyReturnService = mock[MonthlyReturnService]

  private val submissionService = mock[SubmissionService]

  private val service = new MonthlyReturnPollingProcessService(monthlyReturnService, submissionService)

  private val startTime = System.currentTimeMillis()

  "MonthlyReturnPollingProcessService" - {

    "must not call getMonthlyReturnForEdit when there are no submissions" in {
      service.process(Seq.empty, startTime).futureValue mustBe ()

      verifyNoInteractions(monthlyReturnService)
    }

    "must not fail when getMonthlyReturnForEdit fails" in {
      val submission1 = MonthlyReturnSubmissionToPoll(
        submissionId = 100,
        submissionType = "type",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = 2026,
        taxMonth = 1,
        instanceId = "1",
        agentId = None
      )

      val submission2 = MonthlyReturnSubmissionToPoll(
        submissionId = 101,
        submissionType = "type",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = 2026,
        taxMonth = 2,
        instanceId = "2",
        agentId = None
      )

      when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("failed")), Future.successful(()))

      service.process(Seq(submission1, submission2), startTime).futureValue mustBe ()

      verify(monthlyReturnService, times(2)).getMonthlyReturnForEdit(any())(any())
    }

    "must call getMonthlyReturnForEdit for each monthly return submission" in {
      val submission1 = MonthlyReturnSubmissionToPoll(
        submissionId = 100,
        submissionType = "type",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = 2026,
        taxMonth = 4,
        instanceId = "1",
        agentId = None
      )

      val submission2 = MonthlyReturnSubmissionToPoll(
        submissionId = 101,
        submissionType = "type",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = 2026,
        taxMonth = 5,
        instanceId = "2",
        agentId = None
      )

      val response = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(monthlyReturnService.getMonthlyReturnForEdit(any())(any())).thenReturn(Future.successful(response))

      when(submissionService.processMonthlyReturnGovTalkStatusCheck(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      service.process(Seq(submission1, submission2), startTime).futureValue mustBe ()

      verify(monthlyReturnService).getMonthlyReturnForEdit(
        eqTo(
          GetMonthlyReturnForEditRequest(
            "1",
            taxYear = 2026,
            taxMonth = 4,
            isAmendment = Some(false)
          )
        )
      )(any())

      verify(monthlyReturnService).getMonthlyReturnForEdit(
        eqTo(
          GetMonthlyReturnForEditRequest(
            "2",
            taxYear = 2026,
            taxMonth = 5,
            isAmendment = Some(false)
          )
        )
      )(any())
    }

    "must log warning when submission has been polling for more than 24 hours" in {

      val submission = MonthlyReturnSubmissionToPoll(
        submissionId = 100,
        submissionType = "Original",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = 2026,
        taxMonth = 4,
        instanceId = "1",
        agentId = None
      )

      val oldSubmissionRequestDate = LocalDateTime.now(ZoneId.of("Europe/London")).minusHours(25)

      val response = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq(
          Submission(
            submissionId = 100,
            submissionType = "Original",
            activeObjectId = None,
            status = None,
            hmrcMarkGenerated = None,
            hmrcMarkGgis = None,
            emailRecipient = None,
            acceptedTime = None,
            createDate = None,
            lastUpdate = None,
            schemeId = 1,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = Some(oldSubmissionRequestDate),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
        .thenReturn(Future.successful(response))

      when(submissionService.processMonthlyReturnGovTalkStatusCheck(any(), any(), any())(any()))
        .thenReturn(Future.successful(()))

      service.process(Seq(submission), System.currentTimeMillis()).futureValue mustBe ()

      verify(submissionService).processMonthlyReturnGovTalkStatusCheck(
        eqTo("1"),
        eqTo("100"),
        any()
      )(any())
    }
  }
}
