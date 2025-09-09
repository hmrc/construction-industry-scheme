/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryscheme.models.responses

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

case class RDSMonthlyReturnDetails(monthlyReturnId: Long,
                                   taxYear: Int,
                                   taxMonth: Int,
                                   nilReturnIndicator: Option[String] = None,
                                   decEmpStatusConsidered: Option[String] = None,
                                   decAllSubsVerified: Option[String] = None,
                                   decInformationCorrect: Option[String] = None,
                                   decNoMoreSubPayments: Option[String] = None,
                                   decNilReturnNoPayments: Option[String] = None,
                                   status: Option[String] = None,
                                   lastUpdate: Option[LocalDateTime] = None,
                                   amendment: Option[String] = None,
                                   supersededBy: Option[Long] = None)
  
object RDSMonthlyReturnDetails {
  implicit val format: OFormat[RDSMonthlyReturnDetails] = Json.format[RDSMonthlyReturnDetails]
}

case class RDSDatacacheResponse(monthlyReturnList: Seq[RDSMonthlyReturnDetails])

object RDSDatacacheResponse {
  
  import RDSMonthlyReturnDetails.format
  
  implicit val format:OFormat[RDSDatacacheResponse] = Json.format[RDSDatacacheResponse]
}
