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
import java.time.Instant

case class SubmittedMonthlyReturnsResponse(
  scheme: SchemeData,
  monthlyReturns: Seq[MonthlyReturnData],
  submissions: Seq[SubmissionData]
)

object SubmittedMonthlyReturnsResponse {
  given format: OFormat[SubmittedMonthlyReturnsResponse] = Json.format[SubmittedMonthlyReturnsResponse]
}

case class SchemeData(
  name: String,
  taxOfficeNumber: String,
  taxOfficeReference: String
)

object SchemeData {
  given format: OFormat[SchemeData] = Json.format[SchemeData]
}

case class MonthlyReturnData(
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  nilReturnIndicator: String,
  status: String,
  supersededBy: Option[Long],
  amendmentStatus: Option[String],
  monthlyReturnItems: Option[String]
)

object MonthlyReturnData {
  given format: OFormat[MonthlyReturnData] = Json.format[MonthlyReturnData]
}

case class SubmissionData(
  submissionId: Long,
  submissionType: Option[String],
  activeObjectId: Option[Long],
  status: String,
  hmrcMarkGenerated: Option[String],
  hmrcMarkGgis: Option[String],
  emailRecipient: Option[String],
  acceptedTime: Option[Instant]
)

object SubmissionData {
  given format: OFormat[SubmissionData] = Json.format[SubmissionData]
}
