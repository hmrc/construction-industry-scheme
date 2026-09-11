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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.DeleteVerificationResponse

class DeleteVerificationResponseSpec extends SpecBase {

  "DeleteVerificationResponse JSON format" - {

    "serialises to JSON" in {
      val model = DeleteVerificationResponse(verificationsCounter = Some(1L))

      Json.toJson(model) mustBe Json.obj("verificationsCounter" -> 1L)
    }

    "deserialises from JSON" in {
      val json = Json.obj("verificationsCounter" -> 1L)

      json.as[DeleteVerificationResponse] mustBe DeleteVerificationResponse(verificationsCounter = Some(1L))
    }

    "round-trips when verificationsCounter is absent" in {
      val model   = DeleteVerificationResponse(verificationsCounter = None)
      val json    = Json.toJson(model)
      val decoded = json.as[DeleteVerificationResponse]

      decoded mustBe model
    }
  }
}
