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
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}

class ChrisVerificationRequestSpec extends SpecBase {

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
          |  "verifications": [],
          |  "action": "verify",
          |  "declaration": "yes"
          |}
          |""".stripMargin
      )

      val result = json.as[ChrisVerificationRequest]

      result.emailRecipient mustBe None
      result.verifications mustBe empty
    }
  }
}
