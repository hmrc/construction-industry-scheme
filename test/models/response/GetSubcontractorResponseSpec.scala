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

package models.response

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, Subcontractor}
import uk.gov.hmrc.constructionindustryscheme.models.response.{
  GetSubcontractorOtherInfo,
  GetSubcontractorResponse
}

import java.time.LocalDateTime

final class GetSubcontractorResponseSpec extends PlaySpec {

  "GetSubcontractorResponse" should {

    val scheme = ContractorScheme(
      schemeId = 123,
      instanceId = "abc-123",
      accountsOfficeReference = "123PA00123456",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456",
      utr = Some("1234567890"),
      name = Some("Test Contractor Ltd"),
      emailAddress = Some("contractor@example.com"),
      displayWelcomePage = Some("Y"),
      prePopCount = Some(1),
      prePopSuccessful = Some("Y"),
      subcontractorCounter = Some(1),
      verificationBatchCounter = Some(1),
      version = Some(1)
    )

    val subcontractor = Subcontractor(
      subcontractorId = 999L,
      utr = Some("0987654321"),
      pageVisited = Some(1),
      partnerUtr = None,
      crn = None,
      firstName = Some("John"),
      nino = Some("AA123456A"),
      secondName = Some("Q"),
      surname = Some("Smith"),
      partnershipTradingName = None,
      tradingName = Some("John Smith Trading"),
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("Flat 2"),
      addressLine3 = Some("London"),
      addressLine4 = None,
      country = Some("United Kingdom"),
      postcode = Some("AA1 1AA"),
      emailAddress = Some("subcontractor@example.com"),
      phoneNumber = Some("01234567890"),
      mobilePhoneNumber = Some("07123456789"),
      worksReferenceNumber = Some("WR-123"),
      createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
      lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
      subbieResourceRef = Some(456L),
      matched = Some("Y"),
      autoVerified = Some("N"),
      verified = Some("Y"),
      verificationNumber = Some("V123456"),
      taxTreatment = Some("NET"),
      verificationDate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
      version = Some(1),
      updatedTaxTreatment = Some("NET"),
      lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-05-15T10:05:00")),
      pendingVerifications = Some(0)
    )

    val otherInfo = GetSubcontractorOtherInfo(
      utr = "1111111111"
    )

    val model = GetSubcontractorResponse(
      scheme = Some(scheme),
      subcontractor = Some(subcontractor),
      otherInfo = Seq(otherInfo)
    )

    "serialize to JSON" in {
      val json = Json.toJson(model)

      (json \ "scheme" \ "schemeId").as[Int] mustBe 123
      (json \ "scheme" \ "instanceId").as[String] mustBe "abc-123"
      (json \ "scheme" \ "name").as[String] mustBe "Test Contractor Ltd"

      (json \ "subcontractor" \ "subcontractorId").as[Long] mustBe 999L
      (json \ "subcontractor" \ "subbieResourceRef").as[Long] mustBe 456L
      (json \ "subcontractor" \ "utr").as[String] mustBe "0987654321"
      (json \ "subcontractor" \ "subcontractorType").as[String] mustBe "soletrader"
      (json \ "subcontractor" \ "displayName").as[String] mustBe "John Smith"

      (json \ "otherInfo")(0).\("utr").as[String] mustBe "1111111111"
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "scheme": {
          |    "schemeId": 123,
          |    "instanceId": "abc-123",
          |    "accountsOfficeReference": "123PA00123456",
          |    "taxOfficeNumber": "123",
          |    "taxOfficeReference": "AB456",
          |    "utr": "1234567890",
          |    "name": "Test Contractor Ltd",
          |    "emailAddress": "contractor@example.com",
          |    "displayWelcomePage": "Y",
          |    "prePopCount": 1,
          |    "prePopSuccessful": "Y",
          |    "subcontractorCounter": 1,
          |    "verificationBatchCounter": 1,
          |    "version": 1
          |  },
          |  "subcontractor": {
          |    "subcontractorId": 999,
          |    "utr": "0987654321",
          |    "pageVisited": 1,
          |    "firstName": "John",
          |    "nino": "AA123456A",
          |    "secondName": "Q",
          |    "surname": "Smith",
          |    "tradingName": "John Smith Trading",
          |    "subcontractorType": "soletrader",
          |    "addressLine1": "1 Test Street",
          |    "addressLine2": "Flat 2",
          |    "addressLine3": "London",
          |    "country": "United Kingdom",
          |    "postcode": "AA1 1AA",
          |    "emailAddress": "subcontractor@example.com",
          |    "phoneNumber": "01234567890",
          |    "mobilePhoneNumber": "07123456789",
          |    "worksReferenceNumber": "WR-123",
          |    "createDate": "2026-06-15T10:00:00",
          |    "lastUpdate": "2026-06-15T10:05:00",
          |    "subbieResourceRef": 456,
          |    "matched": "Y",
          |    "autoVerified": "N",
          |    "verified": "Y",
          |    "verificationNumber": "V123456",
          |    "taxTreatment": "NET",
          |    "verificationDate": "2026-06-15T10:05:00",
          |    "version": 1,
          |    "updatedTaxTreatment": "NET",
          |    "lastMonthlyReturnDate": "2026-05-15T10:05:00",
          |    "pendingVerifications": 0
          |  },
          |  "otherInfo": [
          |    {
          |      "utr": "1111111111"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      json.validate[GetSubcontractorResponse] mustBe JsSuccess(model)
    }

    "deserialize empty response" in {
      val json = Json.parse(
        """
          |{
          |  "scheme": null,
          |  "subcontractor": null,
          |  "otherInfo": []
          |}
          |""".stripMargin
      )

      json.validate[GetSubcontractorResponse] mustBe JsSuccess(
        GetSubcontractorResponse(
          scheme = None,
          subcontractor = None,
          otherInfo = Seq.empty
        )
      )
    }

    "fail to deserialize when required fields are missing" in {
      Json.obj()
        .validate[GetSubcontractorResponse]
        .isError mustBe true
    }
  }

  "GetSubcontractorOtherInfo" should {

    "serialize to JSON" in {
      Json.toJson(GetSubcontractorOtherInfo("1111111111")) mustBe Json.obj(
        "utr" -> "1111111111"
      )
    }

    "deserialize from JSON" in {
      Json.obj("utr" -> "1111111111")
        .validate[GetSubcontractorOtherInfo] mustBe JsSuccess(
        GetSubcontractorOtherInfo("1111111111")
      )
    }
  }
}