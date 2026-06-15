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
import org.mockito.Mockito.{verifyNoInteractions, when}
import uk.gov.hmrc.constructionindustryscheme.services.{MonthlyReturnPollingProcessService, MonthlyReturnService}

import scala.concurrent.Future

class MonthlyReturnPollingProcessServiceSpec extends SpecBase {
  private val monthlyReturnService = mock[MonthlyReturnService]

  private val service = new MonthlyReturnPollingProcessService(monthlyReturnService)

  "MonthlyReturnPollingProcessService" - {

    "must not call getMonthlyReturnForEdit when there are no submissions" in {
      service.process(Seq.empty).futureValue mustBe ()

      verifyNoInteractions(monthlyReturnService)
    }

    "must fail when getMonthlyReturnForEdit fails" in {
      val submission = MonthlyReturnSubmissionToPoll(
        submissionId = 100,
        submissionType = "type",
        status = "Started",
        taxOfficeNumber = "123",
        taxOfficeReference = "AZ123",
        taxYear = "2026",
        taxMonth = "2",
        instanceId = "1",
        agentId = None
      )

      when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("failed")))

      service.process(Seq(submission)).failed.futureValue.getMessage mustBe "failed"
    }
  }
}
