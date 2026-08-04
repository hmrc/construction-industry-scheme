package models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsError, JsObject, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.models.{Company, Partnership, SoleTrader, Trust}

class CreateAndUpdateSubcontractorRequestSpec extends AnyWordSpec with Matchers {

  "CreateAndUpdateSubcontractorRequest (JSON)" should {

    "read and write with required fields only (soletrader)" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890",
          |  "subcontractorType": "soletrader"
          |}
          |""".stripMargin
      )

      val model = json.as[CreateAndUpdateSubcontractorRequest]
      model mustBe CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
        cisId = "1234567890",
        subcontractorType = SoleTrader
      )

      Json.toJson(model) mustBe json
    }

    "read and write with required fields only (company)" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890",
          |  "subcontractorType": "company"
          |}
          |""".stripMargin
      )

      val model = json.as[CreateAndUpdateSubcontractorRequest]
      model mustBe CreateAndUpdateSubcontractorRequest.CompanyRequest(
        cisId = "1234567890",
        subcontractorType = Company
      )

      Json.toJson(model) mustBe json
    }

    "read and write with required fields only (partnership)" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890",
          |  "subcontractorType": "partnership"
          |}
          |""".stripMargin
      )

      val model = json.as[CreateAndUpdateSubcontractorRequest]
      model mustBe CreateAndUpdateSubcontractorRequest.PartnershipRequest(
        cisId = "1234567890",
        subcontractorType = Partnership
      )

      Json.toJson(model) mustBe json
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

    "round-trip (write then read) with all fields populated (soletrader)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
          cisId = "1234567890",
          subcontractorType = SoleTrader,
          utr = Some("1234567890"),
          nino = Some("AA123456A"),
          firstName = Some("John"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          tradingName = Some("ACME"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Line 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-123")
        )

      val json = Json.parse(
        """{
          | "cisId":"1234567890",
          | "subcontractorType":"soletrader",
          | "utr":"1234567890",
          | "nino":"AA123456A",
          | "firstName":"John",
          | "secondName":"Q",
          | "surname":"Smith",
          | "tradingName":"ACME",
          | "addressLine1":"1 Main Street",
          | "addressLine2":"Line 2",
          | "city":"London",
          | "county":"Greater London",
          | "country":"United Kingdom",
          | "postcode":"AA1 1AA",
          | "emailAddress":"test@test.com",
          | "phoneNumber":"01234567890",
          | "mobilePhoneNumber":"07123456789",
          | "worksReferenceNumber":"WRN-123"
          |}""".stripMargin
      )

      json.as[CreateAndUpdateSubcontractorRequest] mustBe model
      Json.toJson(model) mustBe json
    }

    "round-trip (write then read) with all fields populated (company)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.CompanyRequest(
          cisId = "1234567890",
          subcontractorType = Company,
          utr = Some("1234567890"),
          crn = Some("CRN123"),
          tradingName = Some("ACME LTD"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Line 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-123")
        )

      val json = Json.parse(
        """{
          | "cisId":"1234567890",
          | "subcontractorType":"company",
          | "utr":"1234567890",
          | "crn":"CRN123",
          | "tradingName":"ACME LTD",
          | "addressLine1":"1 Main Street",
          | "addressLine2":"Line 2",
          | "city":"London",
          | "county":"Greater London",
          | "country":"United Kingdom",
          | "postcode":"AA1 1AA",
          | "emailAddress":"test@test.com",
          | "phoneNumber":"01234567890",
          | "mobilePhoneNumber":"07123456789",
          | "worksReferenceNumber":"WRN-123"
          |}""".stripMargin
      )

      json.as[CreateAndUpdateSubcontractorRequest] mustBe model
      Json.toJson(model) mustBe json
    }

    "round-trip (write then read) with all fields populated (partnership)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.PartnershipRequest(
          cisId = "1234567890",
          subcontractorType = Partnership,
          utr = Some("1234567890"),
          partnerUtr = Some("9999999999"),
          partnerCrn = Some("CRN123"),
          partnerNino = Some("AA123456A"),
          partnershipTradingName = Some("My Partnership"),
          partnerTradingName = Some("Nominated Partner"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Line 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-123")
        )

      val json = Json.parse(
        """{
          | "cisId":"1234567890",
          | "subcontractorType":"partnership",
          | "utr":"1234567890",
          | "partnerUtr":"9999999999",
          | "partnerCrn":"CRN123",
          | "partnerNino":"AA123456A",
          | "partnershipTradingName":"My Partnership",
          | "partnerTradingName":"Nominated Partner",
          | "addressLine1":"1 Main Street",
          | "addressLine2":"Line 2",
          | "city":"London",
          | "county":"Greater London",
          | "country":"United Kingdom",
          | "postcode":"AA1 1AA",
          | "emailAddress":"test@test.com",
          | "phoneNumber":"01234567890",
          | "mobilePhoneNumber":"07123456789",
          | "worksReferenceNumber":"WRN-123"
          |}""".stripMargin
      )

      json.as[CreateAndUpdateSubcontractorRequest] mustBe model
      Json.toJson(model) mustBe json
    }

    "fail to read when cisId is missing" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractorType": "soletrader"
          |}
          |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }

    "fail to read when subcontractorType is missing" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890"
          |}
          |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }

    "fail to read subcontractorType with wrong value" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890",
          |  "subcontractorType": "invalid type"
          |}
          |""".stripMargin
      )

      json.validate[CreateAndUpdateSubcontractorRequest].isError mustBe true
    }

    "omit None fields when writing JSON (example: company minimal)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.CompanyRequest(
          cisId = "CIS-omit-nones",
          subcontractorType = Company
        )

      val json = Json.toJson(model).as[JsObject]

      json.keys must contain allOf ("cisId", "subcontractorType")
      json.keys must not contain "utr"
      json.keys must not contain "crn"
      json.keys must not contain "postcode"
      json.keys must not contain "emailAddress"
    }

    "read and write with required fields only (trust)" in {
      val json = Json.parse(
        """
          |{
          |  "cisId": "1234567890",
          |  "subcontractorType": "trust"
          |}
          |""".stripMargin
      )

      val model = json.as[CreateAndUpdateSubcontractorRequest]
      model mustBe CreateAndUpdateSubcontractorRequest.TrustRequest(
        cisId = "1234567890",
        subcontractorType = Trust
      )

      Json.toJson(model) mustBe json
    }

    "round-trip (write then read) with all fields populated (trust)" in {
      val model: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.TrustRequest(
          cisId = "1234567890",
          subcontractorType = Trust,
          utr = Some("1234567890"),
          trustTradingName = Some("The Big Trust"),
          addressLine1 = Some("1 Trust Street"),
          addressLine2 = Some("Line 2"),
          city = Some("London"),
          county = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("trust@test.com"),
          phoneNumber = Some("02000000000"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN-TRUST-1")
        )

      val json = Json.parse(
        """{
          | "cisId":"1234567890",
          | "subcontractorType":"trust",
          | "utr":"1234567890",
          | "trustTradingName":"The Big Trust",
          | "addressLine1":"1 Trust Street",
          | "addressLine2":"Line 2",
          | "city":"London",
          | "county":"Greater London",
          | "country":"United Kingdom",
          | "postcode":"AA1 1AA",
          | "emailAddress":"trust@test.com",
          | "phoneNumber":"02000000000",
          | "mobilePhoneNumber":"07123456789",
          | "worksReferenceNumber":"WRN-TRUST-1"
          |}""".stripMargin
      )

      json.as[CreateAndUpdateSubcontractorRequest] mustBe model
      Json.toJson(model) mustBe json
    }
  }
}
