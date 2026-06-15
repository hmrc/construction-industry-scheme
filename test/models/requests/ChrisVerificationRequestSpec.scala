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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.SubcontractorCurrentVerification
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}

class ChrisVerificationRequestSpec extends SpecBase {

  private val subcontractor: SubcontractorCurrentVerification =
    SubcontractorCurrentVerification(
      subcontractorId = 1L,
      subbieResourceRef = Some(1001L),
      firstName = Some("John"),
      secondName = None,
      surname = Some("Smith"),
      tradingName = None,
      utr = Some("1234567890"),
      nino = Some("AB123456C"),
      crn = None,
      partnerUtr = None,
      partnershipTradingName = None,
      subcontractorType = Some("individual"),
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("Test Area"),
      addressLine3 = None,
      addressLine4 = None,
      country = Some("GB"),
      postcode = Some("AA1 1AA"),
      worksReferenceNumber = Some("WRN123")
    )

  "ChrisVerificationRequest" - {

    "serialize to JSON correctly" in {

      val model = ChrisVerificationRequest(
        instanceId = "id-1",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "ABC456",
        contractorUTR = "1234567890",
        contractorAORef = "123/AB456",
        verificationBatchId = "batch-1",
        verificationBatchResourceRef = "batch-ref",
        emailRecipient = Some("test@test.com"),
        subcontractors = Seq(subcontractor),
        verifications = Seq(
          VerificationDetails(
            subcontractorName = "John Smith",
            verificationResourceRef = "ref-1",
            proceedVerification = true
          )
        )
      )

      val json = Json.toJson(model)

      (json \ "instanceId").as[String] mustBe "id-1"
      (json \ "isAgent").as[Boolean] mustBe false
      (json \ "clientTaxOfficeNumber").as[String] mustBe "123"
      (json \ "clientTaxOfficeRef").as[String] mustBe "ABC456"
      (json \ "contractorUTR").as[String] mustBe "1234567890"
      (json \ "contractorAORef").as[String] mustBe "123/AB456"
      (json \ "verificationBatchId").as[String] mustBe "batch-1"
      (json \ "verificationBatchResourceRef").as[String] mustBe "batch-ref"
      (json \ "emailRecipient").as[String] mustBe "test@test.com"

      val subcontractorJson = (json \ "subcontractors")(0)

      (subcontractorJson \ "subcontractorId").as[Long] mustBe 1L
      (subcontractorJson \ "subbieResourceRef").as[Long] mustBe 1001L
      (subcontractorJson \ "firstName").as[String] mustBe "John"
      (subcontractorJson \ "surname").as[String] mustBe "Smith"
      (subcontractorJson \ "utr").as[String] mustBe "1234567890"
      (subcontractorJson \ "nino").as[String] mustBe "AB123456C"
      (subcontractorJson \ "subcontractorType").as[String] mustBe "individual"
      (subcontractorJson \ "addressLine1").as[String] mustBe "1 Test Street"
      (subcontractorJson \ "postcode").as[String] mustBe "AA1 1AA"
      (subcontractorJson \ "worksReferenceNumber").as[String] mustBe "WRN123"

      val verificationJson = (json \ "verifications")(0)

      (verificationJson \ "subcontractorName").as[String] mustBe "John Smith"
      (verificationJson \ "verificationResourceRef").as[String] mustBe "ref-1"
      (verificationJson \ "proceedVerification").as[Boolean] mustBe true
    }

    "deserialize from JSON correctly" in {

      val json = Json.parse(
        """
          |{
          |  "instanceId": "id-1",
          |  "isAgent": false,
          |  "clientTaxOfficeNumber": "123",
          |  "clientTaxOfficeRef": "ABC456",
          |  "contractorUTR": "1234567890",
          |  "contractorAORef": "123/AB456",
          |  "verificationBatchId": "batch-1",
          |  "verificationBatchResourceRef": "batch-ref",
          |  "emailRecipient": "test@test.com",
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 1,
          |      "subbieResourceRef": 1001,
          |      "firstName": "John",
          |      "secondName": null,
          |      "surname": "Smith",
          |      "tradingName": null,
          |      "utr": "1234567890",
          |      "nino": "AB123456C",
          |      "crn": null,
          |      "partnerUtr": null,
          |      "partnershipTradingName": null,
          |      "subcontractorType": "individual",
          |      "addressLine1": "1 Test Street",
          |      "addressLine2": "Test Area",
          |      "addressLine3": null,
          |      "addressLine4": null,
          |      "country": "GB",
          |      "postcode": "AA1 1AA",
          |      "worksReferenceNumber": "WRN123"
          |    }
          |  ],
          |  "verifications": [
          |    {
          |      "subcontractorName": "John Smith",
          |      "verificationResourceRef": "ref-1",
          |      "proceedVerification": true
          |    }
          |  ],
          |  "action": "verify",
          |  "declaration": "yes"
          |}
          |""".stripMargin
      )

      val result = json.as[ChrisVerificationRequest]

      result.instanceId mustBe "id-1"
      result.isAgent mustBe false
      result.clientTaxOfficeNumber mustBe "123"
      result.clientTaxOfficeRef mustBe "ABC456"
      result.contractorUTR mustBe "1234567890"
      result.contractorAORef mustBe "123/AB456"
      result.verificationBatchId mustBe "batch-1"
      result.verificationBatchResourceRef mustBe "batch-ref"
      result.emailRecipient mustBe Some("test@test.com")

      result.subcontractors must contain only subcontractor

      val verification = result.verifications.head
      verification.subcontractorName mustBe "John Smith"
      verification.verificationResourceRef mustBe "ref-1"
      verification.proceedVerification mustBe true
    }

    "round-trip serialize and deserialize correctly" in {

      val model = ChrisVerificationRequest(
        instanceId = "id-1",
        isAgent = true,
        clientTaxOfficeNumber = "999",
        clientTaxOfficeRef = "XYZ",
        contractorUTR = "1234567890",
        contractorAORef = "123/AB456",
        verificationBatchId = "batch-1",
        verificationBatchResourceRef = "batch-ref",
        emailRecipient = None,
        subcontractors = Seq(subcontractor),
        verifications = Seq.empty
      )

      val json   = Json.toJson(model)
      val result = json.as[ChrisVerificationRequest]

      result mustBe model
    }

    "handle missing optional fields correctly" in {

      val json = Json.parse(
        """
          |{
          |  "instanceId": "id-1",
          |  "isAgent": false,
          |  "clientTaxOfficeNumber": "123",
          |  "clientTaxOfficeRef": "ABC456",
          |  "contractorUTR": "1234567890",
          |  "contractorAORef": "123/AB456",
          |  "verificationBatchId": "batch-1",
          |  "verificationBatchResourceRef": "batch-ref",
          |  "subcontractors": [],
          |  "verifications": [],
          |  "action": "verify",
          |  "declaration": "yes"
          |}
          |""".stripMargin
      )

      val result = json.as[ChrisVerificationRequest]

      result.emailRecipient mustBe None
      result.subcontractors mustBe empty
      result.verifications mustBe empty
    }
  }
}
