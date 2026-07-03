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
import uk.gov.hmrc.constructionindustryscheme.models.Subcontractor
import uk.gov.hmrc.constructionindustryscheme.models.response.GetSubcontractorListResponse

import java.time.LocalDateTime

final class GetSubcontractorListResponseSpec extends PlaySpec {

  "GetSubcontractorListResponse" should {

    val subcontractor = Subcontractor(
      subcontractorId = 999L,
      utr = Some("1234567890"),
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

    val model = GetSubcontractorListResponse(
      subcontractors = List(subcontractor)
    )

    "serialize to JSON including the computed displayName" in {
      val json = Json.toJson(model)

      (json \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 999L
      (json \ "subcontractors")(0).\("utr").as[String] mustBe "1234567890"
      (json \ "subcontractors")(0).\("subcontractorType").as[String] mustBe "soletrader"
      (json \ "subcontractors")(0).\("displayName").as[String] mustBe "John Smith"
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 999,
          |      "utr": "1234567890",
          |      "pageVisited": 1,
          |      "firstName": "John",
          |      "nino": "AA123456A",
          |      "secondName": "Q",
          |      "surname": "Smith",
          |      "tradingName": "John Smith Trading",
          |      "subcontractorType": "soletrader",
          |      "addressLine1": "1 Test Street",
          |      "addressLine2": "Flat 2",
          |      "addressLine3": "London",
          |      "country": "United Kingdom",
          |      "postcode": "AA1 1AA",
          |      "emailAddress": "subcontractor@example.com",
          |      "phoneNumber": "01234567890",
          |      "mobilePhoneNumber": "07123456789",
          |      "worksReferenceNumber": "WR-123",
          |      "createDate": "2026-06-15T10:00:00",
          |      "lastUpdate": "2026-06-15T10:05:00",
          |      "subbieResourceRef": 456,
          |      "matched": "Y",
          |      "autoVerified": "N",
          |      "verified": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verificationDate": "2026-06-15T10:05:00",
          |      "version": 1,
          |      "updatedTaxTreatment": "NET",
          |      "lastMonthlyReturnDate": "2026-05-15T10:05:00",
          |      "pendingVerifications": 0
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      json.validate[GetSubcontractorListResponse] mustBe JsSuccess(model)
    }

    "deserialize an empty subcontractor list" in {
      val json = Json.obj(
        "subcontractors" -> Json.arr()
      )

      json.validate[GetSubcontractorListResponse] mustBe JsSuccess(
        GetSubcontractorListResponse(List.empty)
      )
    }

    "fail to deserialize when subcontractors is missing" in {
      Json.obj()
        .validate[GetSubcontractorListResponse]
        .isError mustBe true
    }
  }
}
