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
import uk.gov.hmrc.constructionindustryscheme.models.*

final case class GetNewestVerificationBatchResponse(
                                                     scheme: Seq[ContractorScheme],
                                                     subcontractors: Seq[Subcontractor],
                                                     verificationBatch: Seq[VerificationBatch],
                                                     verifications: Seq[Verification],
                                                     submission: Seq[Submission],
                                                     monthlyReturn: Seq[MonthlyReturn],
                                                     mrSubmission: Seq[Submission]
                                                   )

object GetNewestVerificationBatchResponse {
  given format: OFormat[GetNewestVerificationBatchResponse] = Json.format[GetNewestVerificationBatchResponse]
}

