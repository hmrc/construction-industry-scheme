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

package models.requests

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.Subcontractor

import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateSubcontractorRequest

import java.time.LocalDateTime

final class UpdateSubcontractorRequestSpec extends PlaySpec {

  "UpdateSubcontractorRequest" should {

    val subcontractor = Subcontractor(
      subcontractorId = 999L,
      utr = Some("1234567890"),
      pageVisited = Some(1),
      partnerUtr = Some("2222222222"),
      crn = Some("CRN123"),
      firstName = Some("John"),
      nino = Some("AA123456A"),
      secondName = Some("Q"),
      surname = Some("Smith"),
      partnershipTradingName = Some("Partnership Trading"),
      tradingName = Some("John Smith Trading"),
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("1 Main Street"),
      addressLine2 = Some("Flat 2"),
      addressLine3 = Some("London"),
      addressLine4 = Some("Greater London"),
      country = Some("United Kingdom"),
      postcode = Some("AA1 1AA"),
      emailAddress = Some("subcontractor@example.com"),
      phoneNumber = Some("01234567890"),
      mobilePhoneNumber = Some("07123456789"),
      worksReferenceNumber = Some("WR-123"),
      createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
      lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
      subbieResourceRef = Some(10L),
      matched = Some("Y"),
      autoVerified = Some("N"),
      verified = Some("Y"),
      verificationNumber = Some("V123456"),
      taxTreatment = Some("NET"),
      verificationDate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
      version = Some(5),
      updatedTaxTreatment = Some("NET"),
      lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-05-15T10:05:00")),
      pendingVerifications = Some(0)
    )

    val model = UpdateSubcontractorRequest(
      cisId = "abc-123",
      subcontractor = subcontractor
    )

    "serialize to JSON" in {
      val json = Json.toJson(model)

      (json \ "cisId").as[String] mustBe "abc-123"

      (json \ "subcontractor" \ "subcontractorId").as[Long] mustBe 999L
      (json \ "subcontractor" \ "subbieResourceRef").as[Long] mustBe 10L
      (json \ "subcontractor" \ "utr").as[String] mustBe "1234567890"
      (json \ "subcontractor" \ "subcontractorType").as[String] mustBe "soletrader"
      (json \ "subcontractor" \ "firstName").as[String] mustBe "John"
      (json \ "subcontractor" \ "surname").as[String] mustBe "Smith"
      (json \ "subcontractor" \ "version").as[Int] mustBe 5
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "abc-123",
          |  "subcontractor": {
          |    "subcontractorId": 999,
          |    "utr": "1234567890",
          |    "pageVisited": 1,
          |    "partnerUtr": "2222222222",
          |    "crn": "CRN123",
          |    "firstName": "John",
          |    "nino": "AA123456A",
          |    "secondName": "Q",
          |    "surname": "Smith",
          |    "partnershipTradingName": "Partnership Trading",
          |    "tradingName": "John Smith Trading",
          |    "subcontractorType": "soletrader",
          |    "addressLine1": "1 Main Street",
          |    "addressLine2": "Flat 2",
          |    "addressLine3": "London",
          |    "addressLine4": "Greater London",
          |    "country": "United Kingdom",
          |    "postcode": "AA1 1AA",
          |    "emailAddress": "subcontractor@example.com",
          |    "phoneNumber": "01234567890",
          |    "mobilePhoneNumber": "07123456789",
          |    "worksReferenceNumber": "WR-123",
          |    "createDate": "2026-06-15T10:00:00",
          |    "lastUpdate": "2026-06-15T10:05:00",
          |    "subbieResourceRef": 10,
          |    "matched": "Y",
          |    "autoVerified": "N",
          |    "verified": "Y",
          |    "verificationNumber": "V123456",
          |    "taxTreatment": "NET",
          |    "verificationDate": "2026-06-15T10:05:00",
          |    "version": 5,
          |    "updatedTaxTreatment": "NET",
          |    "lastMonthlyReturnDate": "2026-05-15T10:05:00",
          |    "pendingVerifications": 0
          |  }
          |}
          |""".stripMargin
      )

      json.validate[UpdateSubcontractorRequest] mustBe JsSuccess(model)
    }

    "deserialize when optional fields are missing" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "abc-123",
          |  "subcontractor": {
          |    "subcontractorId": 999,
          |    "subbieResourceRef": 10,
          |    "subcontractorType": "soletrader",
          |    "firstName": "John",
          |    "surname": "Smith",
          |    "version": 5
          |  }
          |}
          |""".stripMargin
      )

      val result = json.validate[UpdateSubcontractorRequest]

      result.isSuccess mustBe true
      result.get.cisId mustBe "abc-123"
      result.get.subcontractor.subcontractorId mustBe 999L
      result.get.subcontractor.subbieResourceRef mustBe Some(10L)
      result.get.subcontractor.firstName mustBe Some("John")
      result.get.subcontractor.surname mustBe Some("Smith")
      result.get.subcontractor.version mustBe Some(5)
    }

    "fail to deserialize when cisId is missing" in {
      Json
        .obj(
          "subcontractor" -> Json.obj(
            "subcontractorId" -> 999,
            "subbieResourceRef" -> 10
          )
        )
        .validate[UpdateSubcontractorRequest]
        .isError mustBe true
    }

    "fail to deserialize when subcontractor is missing" in {
      Json
        .obj("cisId" -> "abc-123")
        .validate[UpdateSubcontractorRequest]
        .isError mustBe true
    }
  }
}