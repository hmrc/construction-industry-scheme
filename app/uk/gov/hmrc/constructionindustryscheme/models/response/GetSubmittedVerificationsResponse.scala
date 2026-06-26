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

import java.time.LocalDateTime

final case class GetSubmittedVerificationsResponse(
                                                    scheme: Seq[ContractorScheme],
                                                    subcontractors: Seq[Subcontractor],
                                                    verificationBatches: Seq[GetSubmittedVerificationBatch],
                                                    verifications: Seq[GetSubmittedVerification],
                                                    submissions: Seq[Submission]
                                                  )

object GetSubmittedVerificationsResponse {
  given format: OFormat[GetSubmittedVerificationsResponse] =
    Json.format[GetSubmittedVerificationsResponse]
}

final case class GetSubmittedVerificationBatch(
                                                verificationBatchId: Long,
                                                schemeId: Long,
                                                verificationsCounter: Option[Long],
                                                verifBatchResourceRef: Option[Long],
                                                proceedSession: Option[String],
                                                confirmArrangement: Option[String],
                                                confirmCorrect: Option[String],
                                                status: Option[String],
                                                verificationNumber: Option[String],
                                                createDate: Option[LocalDateTime],
                                                lastUpdate: Option[LocalDateTime],
                                                version: Option[Int]
                                              )

object GetSubmittedVerificationBatch:
  given format: OFormat[GetSubmittedVerificationBatch] = Json.format[GetSubmittedVerificationBatch]


case class GetSubmittedVerification(
                                     verificationId: Long,
                                     matched: Option[String],
                                     verificationNumber: Option[String],
                                     taxTreatment: Option[String],
                                     actionIndicator: Option[String],
                                     verificationBatchId: Option[Long],
                                     schemeId: Option[Long],
                                     subcontractorId: Option[Long],
                                     subcontractorName: Option[String],
                                     verificationResourceRef: Option[Long],
                                     proceed: Option[String],
                                     createDate: Option[LocalDateTime],
                                     lastUpdate: Option[LocalDateTime],
                                     version: Option[Int]
                                   )

object GetSubmittedVerification:
  given format: OFormat[GetSubmittedVerification] = Json.format[GetSubmittedVerification]

