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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.{Company, Partnership, SoleTrader}
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest.*

class CreateAndUpdateSubcontractorRequestSpec extends AnyWordSpec with Matchers {

  "CreateAndUpdateSubcontractorRequest JSON format" should {

    "round-trip (write then read) SoleTraderRequest with all fields populated" in {
      val model: CreateAndUpdateSubcontractorRequest =
        SoleTraderRequest(
          cisId = "CIS-123",
          utr = Some("1234567890"),
          nino = Some("AB123456C"),
          firstName = Some("Jane"),
          secondName = Some("Q"),
          surname = Some("Doe"),
          country = Some("United Kingdom"),
          tradingName = Some("ABC Ltd"),
          addressLine1 = Some("10 Downing Street"),
          addressLine2 = Some("Westminster"),
          city = Some("London"),
          county = Some("Greater London"),
          postcode = Some("SW1A 2AA"),
          emailAddress = Some("jane.doe@example.com"),
          phoneNumber = Some("0123456789"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-001")
        )

      val json = Json.toJson(model)
      val back = json.as[CreateAndUpdateSubcontractorRequest]

      back mustEqual model

      (json \ "cisId").as[String] mustBe "CIS-123"
      (json \ "subcontractorType").as[String] mustBe "soletrader"
      (json \ "firstName").as[String] mustBe "Jane"
      (json \ "secondName").as[String] mustBe "Q"
      (json \ "surname").as[String] mustBe "Doe"
      (json \ "country").as[String] mustBe "United Kingdom"
      (json \ "postcode").as[String] mustBe "SW1A 2AA"
      (json \ "county").as[String] mustBe "Greater London"
      (json \ "mobilePhoneNumber").as[String] mustBe "07123456789"
    }

    "round-trip (write then read) CompanyRequest with all fields populated" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CompanyRequest(
          cisId = "CIS-456",
          utr = Some("1234567890"),
          crn = Some("CRN123"),
          tradingName = Some("ACME Ltd"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Suite 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("acme@test.com"),
          phoneNumber = Some("02000000000"),
          mobilePhoneNumber = Some("07111111111"),
          worksReferenceNumber = Some("WRN-999")
        )

      val json = Json.toJson(model)
      val back = json.as[CreateAndUpdateSubcontractorRequest]

      back mustEqual model
      (json \ "subcontractorType").as[String] mustBe "company"
      (json \ "crn").as[String] mustBe "CRN123"
    }

    "round-trip (write then read) PartnershipRequest with all fields populated" in {
      val model: CreateAndUpdateSubcontractorRequest =
        PartnershipRequest(
          cisId = "CIS-789",
          utr = Some("1111111111"),
          partnerUtr = Some("2222222222"),
          crn = Some("CRN123"),
          nino = Some("AA123456A"),
          partnershipTradingName = Some("My Partnership"),
          tradingName = Some("Nominated Partner"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Flat 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("partner@test.com"),
          phoneNumber = Some("02000000000"),
          mobilePhoneNumber = Some("07222222222"),
          worksReferenceNumber = Some("WRN-123")
        )

      val json = Json.toJson(model)
      val back = json.as[CreateAndUpdateSubcontractorRequest]

      back mustEqual model
      (json \ "subcontractorType").as[String] mustBe "partnership"
      (json \ "partnerUtr").as[String] mustBe "2222222222"
      (json \ "partnershipTradingName").as[String] mustBe "My Partnership"
      (json \ "crn").as[String] mustBe "CRN123"
      (json \ "nino").as[String] mustBe "AA123456A"
    }

    "read minimal valid JSON for sole trader (only required fields)" in {
      val json = Json.parse(
        s"""
           |{
           |  "cisId": "CIS-999",
           |  "subcontractorType": "${SoleTrader.toString}"
           |}
           |""".stripMargin
      )

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isSuccess mustBe true

      result.get mustBe SoleTraderRequest(cisId = "CIS-999")
    }

    "read minimal valid JSON for company (only required fields)" in {
      val json = Json.parse(
        s"""
           |{
           |  "cisId": "CIS-888",
           |  "subcontractorType": "${Company.toString}"
           |}
           |""".stripMargin
      )

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isSuccess mustBe true

      result.get mustBe CompanyRequest(cisId = "CIS-888")
    }

    "should fail to read when subcontractorType is unsupported" in {
      val json = Json.parse("""{ "cisId": "CIS-123", "subcontractorType": "banana" }""")

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isError mustBe true

      val msg = result match {
        case JsError(errs) => errs.flatMap(_._2).flatMap(_.messages).mkString(" | ")
        case _             => fail("Expected JsError")
      }

      msg must include("Unsupported subcontractorType: banana")
    }

    "read minimal valid JSON for partnership (only required fields)" in {
      val json = Json.parse(
        s"""
           |{
           |  "cisId": "CIS-777",
           |  "subcontractorType": "${Partnership.toString}"
           |}
           |""".stripMargin
      )

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isSuccess mustBe true

      result.get mustBe PartnershipRequest(cisId = "CIS-777")
    }

    "fail to read when 'cisId' is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "subcontractorType": "${SoleTrader.toString}"
           |}
           |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }

    "fail to read when 'subcontractorType' is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "cisId": "CIS-123"
           |}
           |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }

    "omit None fields when writing JSON (sole trader)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        SoleTraderRequest(
          cisId = "CIS-omit-nones"
        )

      val json = Json.toJson(model).as[JsObject]

      json.keys must contain allOf ("cisId", "subcontractorType")
      json.keys must not contain "partnerUtr"
      json.keys must not contain "emailAddress"
      json.keys must not contain "postcode"
      json.keys must not contain "mobilePhoneNumber"
      json.keys must not contain "city"
      json.keys must not contain "county"
      json.keys must not contain "firstName"
      json.keys must not contain "secondName"
      json.keys must not contain "surname"
      json.keys must not contain "country"
    }

    "ignore unknown fields when reading JSON (forward compatibility)" in {
      val json = Json.parse(
        s"""
           |{
           |  "cisId": "CIS-unknown-ok",
           |  "subcontractorType": "${SoleTrader.toString}",
           |  "ignoredField": "some value",
           |  "anotherIgnored": 123
           |}
           |""".stripMargin
      )

      val result = json.validate[CreateAndUpdateSubcontractorRequest]
      result.isSuccess mustBe true
      result.get.cisId mustBe "CIS-unknown-ok"
    }

    "fail for unsupported subcontractorType" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "CIS-123",
          |  "subcontractorType": "somethingElse"
          |}
          |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }
  }
}
