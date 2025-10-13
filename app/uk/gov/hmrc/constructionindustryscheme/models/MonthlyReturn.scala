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

package uk.gov.hmrc.constructionindustryscheme.models

import play.api.libs.json.{Json, OFormat}
import java.time.LocalDateTime

case class MonthlyReturn(
  monthlyReturnId: Long,
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
  supersededBy: Option[Long] = None
)

object MonthlyReturn {
  implicit val format: OFormat[MonthlyReturn] = Json.format[MonthlyReturn]
}

import play.api.libs.json._
import play.api.libs.functional.syntax._

case class UserMonthlyReturns(
  monthlyReturnList: Seq[MonthlyReturn],
  schemeVersion: Option[Int] = None
)

object UserMonthlyReturns {
  import MonthlyReturn.format

  implicit val reads: Reads[UserMonthlyReturns] = (
    (__ \ "monthlyReturnList").read[Seq[MonthlyReturn]] and
      (__ \ "schemeVersion").readNullable[Int]
  )(UserMonthlyReturns.apply _)

  implicit val writes: OWrites[UserMonthlyReturns] = (
    (__ \ "monthlyReturnList").write[Seq[MonthlyReturn]] and
      (__ \ "schemeVersion").writeNullable[Int]
  ){ ur => (ur.monthlyReturnList, ur.schemeVersion) }

  val empty: UserMonthlyReturns = UserMonthlyReturns(Seq.empty, None)
}