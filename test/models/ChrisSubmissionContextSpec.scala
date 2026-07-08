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

package models

import base.SpecBase

import java.time.LocalDateTime
import uk.gov.hmrc.constructionindustryscheme.models.{MonthlyReturnSubmissionContext, VerificationSubmissionContext}
import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredMonthlyReturnContext, StoredVerificationContext}

class ChrisSubmissionContextSpec extends SpecBase {

  "MonthlyReturnSubmissionContext" - {
    "return monthly return context and no verification context" in {
      val dateTime = LocalDateTime.parse("2026-06-19T10:00:00")

      val context = MonthlyReturnSubmissionContext(
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = dateTime
      )

      context.monthlyReturnContext mustBe Some(
        StoredMonthlyReturnContext(
          hmrcMarkGenerated = "hmrc-mark",
          submissionRequestDate = dateTime
        )
      )

      context.verificationContext mustBe None
    }
  }

  "VerificationSubmissionContext" - {
    "return verification context and no monthly return context" in {
      val dateTime = LocalDateTime.parse("2026-06-19T10:00:00")

      val context = VerificationSubmissionContext(
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = dateTime,
        verificationBatchResourceRef = 5L,
        actionIndicators = Seq.empty,
        requestedVerifications = Seq.empty
      )

      context.monthlyReturnContext mustBe None

      context.verificationContext mustBe Some(
        StoredVerificationContext(
          hmrcMarkGenerated = "hmrc-mark",
          submissionRequestDate = dateTime,
          verificationBatchResourceRef = 5L,
          actionIndicators = Seq.empty,
          requestedVerifications = Seq.empty
        )
      )
    }
  }
}
