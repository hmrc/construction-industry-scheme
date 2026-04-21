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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationCurrVerification

class VerificationCurrVerificationSpec extends SpecBase {
  "VerificationCurrVerification" - {
    "serialize to JSON correctly" in {
      val verification = VerificationCurrVerification(
        verificationId = 1L,
        verificationBatchId = Some(10L),
        subcontractorId = Some(2L),
        verificationResourceRef = Some(20L)
      )
      val json         = Json.toJson(verification)

      (json \ "verificationId").as[Long] mustBe 1L
      (json \ "verificationBatchId").as[Long] mustBe 10L
      (json \ "subcontractorId").as[Long] mustBe 2L
      (json \ "verificationResourceRef").as[Long] mustBe 20L
    }
    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """
          |{
          |  "verificationId": 1,
          |  "verificationBatchId": 10,
          |  "subcontractorId": 2,
          |  "verificationResourceRef": 20
          |}
          |""".stripMargin
      )
      val result = json.as[VerificationCurrVerification]
      result.verificationId mustBe 1L
      result.verificationBatchId mustBe Some(10L)
      result.subcontractorId mustBe Some(2L)
      result.verificationResourceRef mustBe Some(20L)
    }
    "round-trip serialize and deserialize correctly" in {
      val verification = VerificationCurrVerification(
        verificationId = 1L,
        verificationBatchId = Some(10L),
        subcontractorId = Some(2L),
        verificationResourceRef = Some(20L)
      )
      val json         = Json.toJson(verification)
      val result       = json.as[VerificationCurrVerification]
      result mustBe verification
    }
  }
}
