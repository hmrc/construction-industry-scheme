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

import play.api.libs.json.{JsObject, JsString, Json, OFormat, Reads, Writes}

import java.time.LocalDateTime

case class Subcontractor(
  subcontractorId: Long,
  utr: Option[String],
  pageVisited: Option[Int],
  partnerUtr: Option[String],
  crn: Option[String],
  firstName: Option[String],
  nino: Option[String],
  secondName: Option[String],
  surname: Option[String],
  partnershipTradingName: Option[String],
  tradingName: Option[String],
  subcontractorType: Option[String],
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String],
  country: Option[String],
  postCode: Option[String],
  emailAddress: Option[String],
  phoneNumber: Option[String],
  mobilePhoneNumber: Option[String],
  worksReferenceNumber: Option[String],
  createDate: Option[LocalDateTime],
  lastUpdate: Option[LocalDateTime],
  subbieResourceRef: Option[Long],
  matched: Option[String],
  autoVerified: Option[String],
  verified: Option[String],
  verificationNumber: Option[String],
  taxTreatment: Option[String],
  verificationDate: Option[LocalDateTime],
  version: Option[Int],
  updatedTaxTreatment: Option[String],
  lastMonthlyReturnDate: Option[LocalDateTime],
  pendingVerifications: Option[Int]
) {
  def displayName: String =
    (
      subcontractorType.map(_.toLowerCase),
      firstName,
      surname,
      tradingName
    ) match {
      case (Some("soletrader"), Some(firstName), Some(surname), _)             => s"$firstName $surname"
      case (Some("soletrader"), None, Some(surname), _)                        => surname
      case (Some("soletrader" | "company" | "trust"), _, _, Some(tradingName)) => tradingName
      case (Some("partnership"), _, _, Some(tradingName))                      =>
        partnershipTradingName
          .getOrElse(tradingName)
      case _                                                                   => "No name provided"
    }
}

object Subcontractor:
  given reads: Reads[Subcontractor]   = Json.reads[Subcontractor]
  given writes: Writes[Subcontractor] = s =>
    Json
      .writes[Subcontractor]
      .writes(s) + ("displayName", JsString(s.displayName))
