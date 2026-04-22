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

case class GetSubmittedMonthlyReturnsDataResponse(
  // scheme: ContractorScheme,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  contractorName: String,
  // monthlyReturn: MonthlyReturn,
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  returnType: String,
  monthlyReturnItems: Seq[MonthlyReturnItem],
  // submission: Submission,
  acceptedTime: Option[String],
  receiptReferenceNumber: Option[String] // hmrcMarkGgis: Option[String],
)

object GetSubmittedMonthlyReturnsDataResponse:
  given format: OFormat[GetSubmittedMonthlyReturnsDataResponse] = Json.format[GetSubmittedMonthlyReturnsDataResponse]
