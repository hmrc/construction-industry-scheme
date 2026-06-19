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

package uk.gov.hmrc.constructionindustryscheme.models

import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredMonthlyReturnContext, StoredRequestedVerification, StoredVerificationContext}

import java.time.LocalDateTime

sealed trait ChrisSubmissionContext {
  def hmrcMarkGenerated: String
  def submissionRequestDate: LocalDateTime

  def monthlyReturnContext: Option[StoredMonthlyReturnContext]
  def verificationContext: Option[StoredVerificationContext]
}

final case class MonthlyReturnSubmissionContext(
  hmrcMarkGenerated: String,
  submissionRequestDate: LocalDateTime
) extends ChrisSubmissionContext {

  override def monthlyReturnContext: Option[StoredMonthlyReturnContext] =
    Some(
      StoredMonthlyReturnContext(
        hmrcMarkGenerated = hmrcMarkGenerated,
        submissionRequestDate = submissionRequestDate
      )
    )

  override def verificationContext: Option[StoredVerificationContext] = None

}

final case class VerificationSubmissionContext(
  hmrcMarkGenerated: String,
  submissionRequestDate: LocalDateTime,
  verificationBatchResourceRef: Long,
  actionIndicators: Seq[VerificationActionIndicator],
  requestedVerifications: Seq[StoredRequestedVerification]
) extends ChrisSubmissionContext {

  override def monthlyReturnContext: Option[StoredMonthlyReturnContext] = None

  override def verificationContext: Option[StoredVerificationContext] =
    Some(
      StoredVerificationContext(
        hmrcMarkGenerated = hmrcMarkGenerated,
        submissionRequestDate = submissionRequestDate,
        verificationBatchResourceRef = verificationBatchResourceRef,
        actionIndicators = actionIndicators,
        requestedVerifications = requestedVerifications
      )
    )
}
