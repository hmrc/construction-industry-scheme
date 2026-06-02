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

package models

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.SubcontractorCurrentVerification

class SubcontractorCurrentVerificationSpec extends SpecBase {

  "SubcontractorCurrentVerification" - {

    "serialize to JSON correctly" in {

      val subcontractor = SubcontractorCurrentVerification(
        subcontractorId = 1L,
        subbieResourceRef = Some(10L),
        firstName = Some("John"),
        secondName = Some("Paul"),
        surname = Some("Smith"),
        tradingName = Some("ACME"),
        utr = Some("1111111111"),
        nino = Some("AA123456A"),
        crn = Some("AC012345"),
        partnerUtr = Some("5860920998"),
        partnershipTradingName = Some("ACME trading"),
        subcontractorType = Some("soletrader"),
        addressLine1 = Some("Line 1"),
        addressLine2 = Some("Line 2"),
        addressLine3 = Some("Line 3"),
        addressLine4 = Some("Line 4"),
        country = Some("UK"),
        postcode = Some("NE1 1AA"),
        worksReferenceNumber = Some("WRN123")
      )

      val json = Json.toJson(subcontractor)

      (json \ "subcontractorId").as[Long] mustBe 1L
      (json \ "subbieResourceRef").as[Long] mustBe 10L
      (json \ "firstName").as[String] mustBe "John"
      (json \ "secondName").as[String] mustBe "Paul"
      (json \ "surname").as[String] mustBe "Smith"
      (json \ "tradingName").as[String] mustBe "ACME"
      (json \ "utr").as[String] mustBe "1111111111"
      (json \ "nino").as[String] mustBe "AA123456A"
      (json \ "crn").as[String] mustBe "AC012345"
      (json \ "partnerUtr").as[String] mustBe "5860920998"
      (json \ "partnershipTradingName").as[String] mustBe "ACME trading"
      (json \ "subcontractorType").as[String] mustBe "soletrader"
      (json \ "addressLine1").as[String] mustBe "Line 1"
      (json \ "addressLine2").as[String] mustBe "Line 2"
      (json \ "addressLine3").as[String] mustBe "Line 3"
      (json \ "addressLine4").as[String] mustBe "Line 4"
      (json \ "country").as[String] mustBe "UK"
      (json \ "postcode").as[String] mustBe "NE1 1AA"
      (json \ "worksReferenceNumber").as[String] mustBe "WRN123"
    }

    "deserialize from JSON correctly" in {

      val json = Json.parse(
        """
          |{
          |  "subcontractorId": 1,
          |  "subbieResourceRef": 10,
          |  "firstName": "John",
          |  "secondName": "Paul",
          |  "surname": "Smith",
          |  "tradingName": "ACME",
          |  "utr": "1111111111",
          |  "nino": "AA123456A",
          |  "crn": "AC012345",
          |  "partnerUtr": "5860920998",
          |  "partnershipTradingName": "ACME trading",
          |  "subcontractorType": "soletrader",
          |  "addressLine1": "Line 1",
          |  "addressLine2": "Line 2",
          |  "addressLine3": "Line 3",
          |  "addressLine4": "Line 4",
          |  "country": "UK",
          |  "postcode": "NE1 1AA",
          |  "worksReferenceNumber": "WRN123"
          |}
          |""".stripMargin
      )

      val result = json.as[SubcontractorCurrentVerification]

      result.subcontractorId mustBe 1L
      result.subbieResourceRef mustBe Some(10L)
      result.firstName mustBe Some("John")
      result.secondName mustBe Some("Paul")
      result.surname mustBe Some("Smith")
      result.tradingName mustBe Some("ACME")
      result.utr mustBe Some("1111111111")
      result.nino mustBe Some("AA123456A")
      result.crn mustBe Some("AC012345")
      result.partnerUtr mustBe Some("5860920998")
      result.partnershipTradingName mustBe Some("ACME trading")
      result.subcontractorType mustBe Some("soletrader")
      result.addressLine1 mustBe Some("Line 1")
      result.addressLine2 mustBe Some("Line 2")
      result.addressLine3 mustBe Some("Line 3")
      result.addressLine4 mustBe Some("Line 4")
      result.country mustBe Some("UK")
      result.postcode mustBe Some("NE1 1AA")
      result.worksReferenceNumber mustBe Some("WRN123")
    }

    "round-trip serialize and deserialize correctly" in {

      val subcontractor = SubcontractorCurrentVerification(
        subcontractorId = 1L,
        subbieResourceRef = Some(10L),
        firstName = Some("John"),
        secondName = Some("Paul"),
        surname = Some("Smith"),
        tradingName = Some("ACME"),
        utr = Some("1111111111"),
        nino = Some("AA123456A"),
        crn = Some("AC012345"),
        partnerUtr = Some("5860920998"),
        partnershipTradingName = Some("ACME trading"),
        subcontractorType = Some("soletrader"),
        addressLine1 = Some("Line 1"),
        addressLine2 = Some("Line 2"),
        addressLine3 = Some("Line 3"),
        addressLine4 = Some("Line 4"),
        country = Some("UK"),
        postcode = Some("NE1 1AA"),
        worksReferenceNumber = Some("WRN123")
      )

      val json   = Json.toJson(subcontractor)
      val result = json.as[SubcontractorCurrentVerification]

      result mustBe subcontractor
    }

    "handle missing optional fields correctly" in {

      val json = Json.parse(
        """
          |{
          |  "subcontractorId": 2
          |}
          |""".stripMargin
      )

      val result = json.as[SubcontractorCurrentVerification]

      result.subcontractorId mustBe 2L
      result.subbieResourceRef mustBe None
      result.firstName mustBe None
      result.secondName mustBe None
      result.surname mustBe None
      result.tradingName mustBe None
      result.utr mustBe None
      result.nino mustBe None
      result.crn mustBe None
      result.partnerUtr mustBe None
      result.partnershipTradingName mustBe None
      result.subcontractorType mustBe None
      result.addressLine1 mustBe None
      result.addressLine2 mustBe None
      result.addressLine3 mustBe None
      result.addressLine4 mustBe None
      result.country mustBe None
      result.postcode mustBe None
      result.worksReferenceNumber mustBe None
    }
  }
}
