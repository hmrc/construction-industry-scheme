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

package uk.gov.hmrc.constructionindustryscheme.repositories

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.constructionindustryscheme.models.VerificationActionIndicator
import uk.gov.hmrc.constructionindustryscheme.models.response.GetGovTalkStatusResponse
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.{Instant, LocalDateTime}

case class ChrisSubmissionSessionData(
  submissionId: String,
  instanceId: String,
  correlationId: String,
  lastMessageDate: Instant,
  numPolls: Int,
  pollInterval: Int,
  pollUrl: String,
  govTalkStatus: Option[GetGovTalkStatusResponse],
  monthlyReturnContext: Option[StoredMonthlyReturnContext] = None,
  verificationContext: Option[StoredVerificationContext] = None
)

case class StoredMonthlyReturnContext(
  hmrcMarkGenerated: String,
  submissionRequestDate: LocalDateTime
)

case class StoredVerificationContext(
  verificationBatchResourceRef: Long,
  hmrcMarkGenerated: String,
  submissionRequestDate: LocalDateTime,
  actionIndicators: Seq[VerificationActionIndicator],
  requestedVerifications: Seq[StoredRequestedVerification]
)

case class StoredRequestedVerification(
  verificationResourceRef: Long,
  subcontractorId: Long,
  subbieResourceRef: Option[Long],
  subcontractorName: String,
  actionIndicator: String,
  proceedVerification: Boolean,
  foreName: Option[String],
  middleName: Option[String],
  surname: Option[String],
  tradingName: Option[String],
  utr: Option[String],
  nino: Option[String],
  crn: Option[String],
  subcontractorType: Option[String],
  partnershipUtr: Option[String]
)

object StoredMonthlyReturnContext {
  given format: OFormat[StoredMonthlyReturnContext] = Json.format[StoredMonthlyReturnContext]
}

object StoredVerificationContext {
  given format: OFormat[StoredVerificationContext] = Json.format[StoredVerificationContext]
}

object StoredRequestedVerification {
  given format: OFormat[StoredRequestedVerification] = Json.format[StoredRequestedVerification]
}

object ChrisSubmissionSessionData {
  given dateFormat: Format[Instant]                = MongoJavatimeFormats.instantFormat
  given format: Format[ChrisSubmissionSessionData] = Json.format[ChrisSubmissionSessionData]
}
