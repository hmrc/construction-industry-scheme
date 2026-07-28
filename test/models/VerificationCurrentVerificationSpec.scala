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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationCurrentVerification

class VerificationCurrentVerificationSpec extends SpecBase {

  "VerificationCurrentVerification" - {

    "serialize to JSON correctly" in {
      val verification = VerificationCurrentVerification(
        verificationId = 1L,
        verificationBatchId = Some(10L),
        subcontractorId = Some(2L),
        verificationResourceRef = Some(20L),
        subcontractorName = Some("John Smith"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("0"),
        actionIndicator = Some("A"),
        proceed = Some("Y"),
        matched = Some("Y")
      )

      val json = Json.toJson(verification)

      (json \ "verificationId").as[Long] mustBe 1L
      (json \ "verificationBatchId").as[Long] mustBe 10L
      (json \ "subcontractorId").as[Long] mustBe 2L
      (json \ "verificationResourceRef").as[Long] mustBe 20L
      (json \ "subcontractorName").as[String] mustBe "John Smith"
      (json \ "verificationNumber").as[String] mustBe "V123456"
      (json \ "taxTreatment").as[String] mustBe "0"
      (json \ "actionIndicator").as[String] mustBe "A"
      (json \ "proceed").as[String] mustBe "Y"
      (json \ "matched").as[String] mustBe "Y"
    }

    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "verificationId": 1,
          |  "verificationBatchId": 10,
          |  "subcontractorId": 2,
          |  "verificationResourceRef": 20,
          |  "subcontractorName": "John Smith",
          |  "verificationNumber": "V123456",
          |  "taxTreatment": "0",
          |  "actionIndicator": "A",
          |  "proceed": "Y",
          |  "matched": "Y"
          |}
          |""".stripMargin
      )

      val result = json.as[VerificationCurrentVerification]

      result.verificationId mustBe 1L
      result.verificationBatchId mustBe Some(10L)
      result.subcontractorId mustBe Some(2L)
      result.verificationResourceRef mustBe Some(20L)
      result.subcontractorName mustBe Some("John Smith")
      result.verificationNumber mustBe Some("V123456")
      result.taxTreatment mustBe Some("0")
      result.actionIndicator mustBe Some("A")
      result.proceed mustBe Some("Y")
      result.matched mustBe Some("Y")
    }

    "round-trip serialize and deserialize correctly" in {
      val verification = VerificationCurrentVerification(
        verificationId = 1L,
        verificationBatchId = Some(10L),
        subcontractorId = Some(2L),
        verificationResourceRef = Some(20L),
        subcontractorName = Some("John Smith"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("0"),
        actionIndicator = Some("A"),
        proceed = Some("Y"),
        matched = Some("Y")
      )

      val json = Json.toJson(verification)
      val result = json.as[VerificationCurrentVerification]

      result mustBe verification
    }

    "handle missing optional fields correctly" in {
      val json = Json.parse(
        """
          |{
          |  "verificationId": 1
          |}
          |""".stripMargin
      )

      val result = json.as[VerificationCurrentVerification]

      result.verificationId mustBe 1L
      result.verificationBatchId mustBe None
      result.subcontractorId mustBe None
      result.verificationResourceRef mustBe None
      result.subcontractorName mustBe None
      result.verificationNumber mustBe None
      result.taxTreatment mustBe None
      result.actionIndicator mustBe None
      result.proceed mustBe None
      result.matched mustBe None
    }
  }
}
