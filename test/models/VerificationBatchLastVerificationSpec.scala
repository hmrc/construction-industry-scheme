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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationBatchLastVerification

class VerificationBatchLastVerificationSpec extends SpecBase {
  "VerificationBatchLastVerification" - {
    "serialize to JSON correctly" in {
      val verificationBatch = VerificationBatchLastVerification(
        verificationBatchId = Some(99L),
        verifBatchResourceRef = Some(1234567L),
        verificationBatchStatus = Some("ACCEPTED")
      )
      val json              = Json.toJson(verificationBatch)
      (json \ "verificationBatchId").as[Long] mustBe 99L
      (json \ "verifBatchResourceRef").as[Long] mustBe 1234567L
      (json \ "verificationBatchStatus").as[String] mustBe "ACCEPTED"
    }

    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """|{ 
            |"verificationBatchId": 99, 
            | "verifBatchResourceRef": 1234567 
          |}""".stripMargin
      )
      val result = json.as[VerificationBatchLastVerification]
      result.verificationBatchId mustBe Some(99L)
      result.verifBatchResourceRef mustBe Some(1234567L)
    }

    "round-trip serialize and deserialize correctly" in {
      val verificationBatch = VerificationBatchLastVerification(
        verificationBatchId = Some(99L),
        verifBatchResourceRef = Some(1234567L),
        verificationBatchStatus = Some("ACCEPTED")
      )
      val json              = Json.toJson(verificationBatch)
      val result            = json.as[VerificationBatchLastVerification]
      result mustBe verificationBatch
    }
  }
}
