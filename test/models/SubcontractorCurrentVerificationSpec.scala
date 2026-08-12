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

import java.time.LocalDateTime

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
        emailAddress = Some("john@test.com"),
        phoneNumber = Some("01911234567"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("WRN123"),
        matched = Some("Y"),
        autoVerified = Some("N"),
        verified = Some("Y"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("0"),
        verificationDate = Some(LocalDateTime.parse("2026-07-23T10:15:30")),
        version = Some(1),
        updatedTaxTreatment = Some("1"),
        lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-06-30T00:00:00")),
        pendingVerifications = Some(2)
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
      (json \ "emailAddress").as[String] mustBe "john@test.com"
      (json \ "phoneNumber").as[String] mustBe "01911234567"
      (json \ "mobilePhoneNumber").as[String] mustBe "07123456789"
      (json \ "matched").as[String] mustBe "Y"
      (json \ "autoVerified").as[String] mustBe "N"
      (json \ "verified").as[String] mustBe "Y"
      (json \ "verificationNumber").as[String] mustBe "V123456"
      (json \ "taxTreatment").as[String] mustBe "0"
      (json \ "verificationDate").as[String] mustBe "2026-07-23T10:15:30"
      (json \ "version").as[Int] mustBe 1
      (json \ "updatedTaxTreatment").as[String] mustBe "1"
      (json \ "lastMonthlyReturnDate").as[String] mustBe "2026-06-30T00:00:00"
      (json \ "pendingVerifications").as[Int] mustBe 2
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
          |  "worksReferenceNumber": "WRN123",
          |  "emailAddress": "john@test.com",
          |  "phoneNumber": "01911234567",
          |  "mobilePhoneNumber": "07123456789",
          |  "matched": "Y",
          |  "autoVerified": "N",
          |  "verified": "Y",
          |  "verificationNumber": "V123456",
          |  "taxTreatment": "0",
          |  "verificationDate": "2026-07-23T10:15:30",
          |  "version": 1,
          |  "updatedTaxTreatment": "1",
          |  "lastMonthlyReturnDate": "2026-06-30T00:00:00",
          |  "pendingVerifications": 2
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
      result.emailAddress mustBe Some("john@test.com")
      result.phoneNumber mustBe Some("01911234567")
      result.mobilePhoneNumber mustBe Some("07123456789")
      result.matched mustBe Some("Y")
      result.autoVerified mustBe Some("N")
      result.verified mustBe Some("Y")
      result.verificationNumber mustBe Some("V123456")
      result.taxTreatment mustBe Some("0")
      result.verificationDate mustBe Some(LocalDateTime.parse("2026-07-23T10:15:30"))
      result.version mustBe Some(1)
      result.updatedTaxTreatment mustBe Some("1")
      result.lastMonthlyReturnDate mustBe Some(LocalDateTime.parse("2026-06-30T00:00:00"))
      result.pendingVerifications mustBe Some(2)
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
        emailAddress = Some("john@test.com"),
        phoneNumber = Some("01911234567"),
        mobilePhoneNumber = Some("07123456789"),
        worksReferenceNumber = Some("WRN123"),
        matched = Some("Y"),
        autoVerified = Some("N"),
        verified = Some("Y"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("0"),
        verificationDate = Some(LocalDateTime.parse("2026-07-23T10:15:30")),
        version = Some(1),
        updatedTaxTreatment = Some("1"),
        lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-06-30T00:00:00")),
        pendingVerifications = Some(2)
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
      result.emailAddress mustBe None
      result.phoneNumber mustBe None
      result.mobilePhoneNumber mustBe None
      result.matched mustBe None
      result.autoVerified mustBe None
      result.verified mustBe None
      result.verificationNumber mustBe None
      result.taxTreatment mustBe None
      result.verificationDate mustBe None
      result.version mustBe None
      result.updatedTaxTreatment mustBe None
      result.lastMonthlyReturnDate mustBe None
      result.pendingVerifications mustBe None
    }
  }
}
