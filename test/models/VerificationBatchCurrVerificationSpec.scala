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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationBatchCurrVerification

class VerificationBatchCurrVerificationSpec extends SpecBase {
  "VerificationBatchCurrVerification" - {
    "serialize to JSON correctly" in {
      val verificationBatch = VerificationBatchCurrVerification(
        verificationBatchId = 1L,
        verifBatchResourceRef = Some(10L)
      )
      val json              = Json.toJson(verificationBatch)

      (json \ "verificationBatchId").as[Long] mustBe 1L
      (json \ "verifBatchResourceRef").as[Long] mustBe 10L
    }
    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """
          |{
          |  "verificationBatchId": 1,
          |  "verifBatchResourceRef": 10
          |}
          |""".stripMargin
      )
      val result = json.as[VerificationBatchCurrVerification]
      result.verificationBatchId mustBe 1L
      result.verifBatchResourceRef mustBe Some(10L)
    }
    "round-trip serialize and deserialize correctly" in {
      val verificationBatch = VerificationBatchCurrVerification(
        verificationBatchId = 1L,
        verifBatchResourceRef = Some(10L)
      )
      val json              = Json.toJson(verificationBatch)
      val result            = json.as[VerificationBatchCurrVerification]
      result mustBe verificationBatch
    }
  }
}
