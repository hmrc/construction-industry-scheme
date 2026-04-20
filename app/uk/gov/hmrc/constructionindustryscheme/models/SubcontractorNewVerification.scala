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

import play.api.libs.json.{JsObject, Json, OFormat, Reads, Writes}

import java.time.LocalDateTime

case class SubcontractorNewVerification(
  subcontractorId: Long,
  firstName: Option[String],
  secondName: Option[String],
  surname: Option[String],
  tradingName: Option[String],
  verified: Option[String],
  verificationNumber: Option[String],
  taxTreatment: Option[String],
  verificationDate: Option[LocalDateTime],
  lastMonthlyReturnDate: Option[LocalDateTime]
)

object SubcontractorNewVerification {
  given format: OFormat[SubcontractorNewVerification] = Json.format[SubcontractorNewVerification]
}
