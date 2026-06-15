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

package uk.gov.hmrc.constructionindustryscheme.models.response

import play.api.libs.json.{Json, OFormat}

case class VerificationSubmissionToPoll(
  submissionId: Long,
  submissionType: String,
  agentId: Option[String],
  taxOfficeNumber: String,
  taxOfficeReference: String,
  instanceId: String,
  status: String,
  verificationBatchResourceRef: Long
)

object VerificationSubmissionToPoll {
  given format: OFormat[VerificationSubmissionToPoll] =
    Json.format[VerificationSubmissionToPoll]
}

case class MonthlyReturnSubmissionToPoll(
  submissionId: Long,
  submissionType: String,
  status: String,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  taxYear: String,
  taxMonth: String,
  instanceId: String,
  agentId: Option[String]
)

object MonthlyReturnSubmissionToPoll {
  given format: OFormat[MonthlyReturnSubmissionToPoll] =
    Json.format[MonthlyReturnSubmissionToPoll]
}

case class GetBatchPollSubmissionsResponse(
  verificationSubmissions: Seq[VerificationSubmissionToPoll],
  monthlyReturnSubmissions: Seq[MonthlyReturnSubmissionToPoll]
)

object GetBatchPollSubmissionsResponse {
  given format: OFormat[GetBatchPollSubmissionsResponse] =
    Json.format[GetBatchPollSubmissionsResponse]
}
