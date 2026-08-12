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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationLastVerification

class VerificationLastVerificationSpec extends SpecBase {
  "VerificationLastVerification" - {
    "serialize to JSON correctly" in {
      val verification = VerificationLastVerification(
        verificationId = 1001L,
        verificationBatchId = Some(99L),
        verificationResourcesRef = Some(12345L),
        matched = Some("Y"),
        verificationNumber = Some("V0000000001"),
        taxTreatment = Some("0"),
        subcontractorName = Some("James Star")
      )
      val json         = Json.toJson(verification)
      (json \ "verificationId").as[Long] mustBe 1001L
      (json \ "verificationBatchId").as[Long] mustBe 99L
      (json \ "verificationResourcesRef").as[Long] mustBe 12345L
      (json \ "matched").as[String] mustBe "Y"
      (json \ "verificationNumber").as[String] mustBe "V0000000001"
      (json \ "taxTreatment").as[String] mustBe "0"
      (json \ "subcontractorName").as[String] mustBe "James Star"
    }
    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """|{ 
            |"verificationId": 1001, 
            | "verificationBatchId": 99, 
            | "verificationResourcesRef": 12345, 
            | "matched": "Y", 
            | "verificationNumber": "V0000000001", 
            | "taxTreatment": "0", 
            | "subcontractorName": "James Star" 
          |}""".stripMargin
      )
      val result = json.as[VerificationLastVerification]
      result.verificationId mustBe 1001L
      result.verificationBatchId mustBe Some(99L)
      result.verificationResourcesRef mustBe Some(12345L)
      result.matched mustBe Some("Y")
      result.verificationNumber mustBe Some("V0000000001")
      result.taxTreatment mustBe Some("0")
      result.subcontractorName mustBe Some("James Star")
    }

    "round-trip serialize and deserialize correctly" in {
      val verification = VerificationLastVerification(
        verificationId = 1001L,
        verificationBatchId = Some(99L),
        verificationResourcesRef = Some(12345L),
        matched = Some("Y"),
        verificationNumber = Some("V0000000001"),
        taxTreatment = Some("0"),
        subcontractorName = Some("James Star")
      )
      val json         = Json.toJson(verification)
      val result       = json.as[VerificationLastVerification]
      result mustBe verification
    }
  }
}
