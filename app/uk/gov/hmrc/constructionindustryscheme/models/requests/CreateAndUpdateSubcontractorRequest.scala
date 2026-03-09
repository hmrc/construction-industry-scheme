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

package uk.gov.hmrc.constructionindustryscheme.models.requests

import play.api.libs.json.*

import uk.gov.hmrc.constructionindustryscheme.models.SubcontractorType
import uk.gov.hmrc.constructionindustryscheme.models.SoleTrader
import uk.gov.hmrc.constructionindustryscheme.models.Partnership
import uk.gov.hmrc.constructionindustryscheme.models.Company

sealed trait CreateAndUpdateSubcontractorRequest {
  def cisId: String
  def subcontractorType: SubcontractorType
}

object CreateAndUpdateSubcontractorRequest {
  final case class SoleTraderRequest(
    cisId: String,
    subcontractorType: SubcontractorType = SoleTrader,
    utr: Option[String] = None,
    nino: Option[String] = None,
    firstName: Option[String] = None,
    secondName: Option[String] = None,
    surname: Option[String] = None,
    tradingName: Option[String] = None,
    addressLine1: Option[String] = None,
    addressLine2: Option[String] = None,
    city: Option[String] = None,
    county: Option[String] = None,
    country: Option[String] = None,
    postcode: Option[String] = None,
    emailAddress: Option[String] = None,
    phoneNumber: Option[String] = None,
    mobilePhoneNumber: Option[String] = None,
    worksReferenceNumber: Option[String] = None
  ) extends CreateAndUpdateSubcontractorRequest

  final case class CompanyRequest(
    cisId: String,
    subcontractorType: SubcontractorType = Company,
    utr: Option[String] = None,
    crn: Option[String] = None,
    tradingName: Option[String] = None,
    addressLine1: Option[String] = None,
    addressLine2: Option[String] = None,
    city: Option[String] = None,
    county: Option[String] = None,
    country: Option[String] = None,
    postcode: Option[String] = None,
    emailAddress: Option[String] = None,
    phoneNumber: Option[String] = None,
    mobilePhoneNumber: Option[String] = None,
    worksReferenceNumber: Option[String] = None
  ) extends CreateAndUpdateSubcontractorRequest

  final case class PartnershipRequest(
    cisId: String,
    subcontractorType: SubcontractorType = Partnership,
    utr: Option[String] = None,
    partnerUtr: Option[String] = None,
    partnershipTradingName: Option[String] = None,
    tradingName: Option[String] = None,
    addressLine1: Option[String] = None,
    addressLine2: Option[String] = None,
    city: Option[String] = None,
    county: Option[String] = None,
    country: Option[String] = None,
    postcode: Option[String] = None,
    emailAddress: Option[String] = None,
    phoneNumber: Option[String] = None,
    mobilePhoneNumber: Option[String] = None,
    worksReferenceNumber: Option[String] = None
  ) extends CreateAndUpdateSubcontractorRequest

  given OFormat[SoleTraderRequest] = Json.format[SoleTraderRequest]

  given OFormat[CompanyRequest] = Json.format[CompanyRequest]

  given OFormat[PartnershipRequest] = Json.format[PartnershipRequest]

  given OFormat[CreateAndUpdateSubcontractorRequest] = new OFormat[CreateAndUpdateSubcontractorRequest] {

    override def reads(json: JsValue): JsResult[CreateAndUpdateSubcontractorRequest] =
      (json \ "subcontractorType").validate[SubcontractorType].flatMap {
        case SoleTrader  => json.validate[SoleTraderRequest]
        case Company     => json.validate[CompanyRequest]
        case Partnership => json.validate[PartnershipRequest]
        case other       => JsError(s"Unsupported subcontractorType: $other")
      }

    override def writes(o: CreateAndUpdateSubcontractorRequest): JsObject = o match {
      case s: SoleTraderRequest  => Json.toJsObject(s)
      case c: CompanyRequest     => Json.toJsObject(c)
      case p: PartnershipRequest => Json.toJsObject(p)
    }
  }
}
