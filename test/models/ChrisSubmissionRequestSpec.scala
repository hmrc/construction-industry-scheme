/*
 * Copyright 2025 HM Revenue & Customs
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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisSubmissionRequest

class ChrisSubmissionRequestSpec extends AnyWordSpec with Matchers {

  private val model = ChrisSubmissionRequest(
    utr = "1234567890",
    aoReference = "123/AB456",
    informationCorrect = "yes",
    inactivity = "no",
    monthYear = "2025-05",
    email = "test@test.com",
    isAgent = false,
    clientTaxOfficeNumber = "",
    clientTaxOfficeRef = ""
  )

  private val json: JsValue = Json.obj(
    "utr"                   -> "1234567890",
    "aoReference"           -> "123/AB456",
    "informationCorrect"    -> "yes",
    "inactivity"            -> "no",
    "monthYear"             -> "2025-05",
    "email"                 -> "test@test.com",
    "isAgent"               -> false,
    "clientTaxOfficeNumber" -> "",
    "clientTaxOfficeRef"    -> ""
  )

  "ChrisSubmissionRequest JSON format" should {

    "write a model to JSON" in {
      Json.toJson(model) mustEqual json
    }

    "read JSON into a model" in {
      json.validate[ChrisSubmissionRequest] mustEqual JsSuccess(model)
    }

    "round-trip (model -> json -> model)" in {
      Json.toJson(model).validate[ChrisSubmissionRequest].get mustEqual model
    }

    "fail to read when a required field is missing" in {
      val missing = json.as[JsObject] - "utr"
      val result  = missing.validate[ChrisSubmissionRequest]
      result.isError mustBe true
      val errors  = result.fold(identity, _ => fail("expected JsError"))
      errors.exists { case (path, _) => path == (JsPath \ "utr") } mustBe true
    }

    "fail to read when a field has the wrong type" in {
      val wrongType = json.as[JsObject] + ("inactivity" -> JsNumber(1))
      val result    = wrongType.validate[ChrisSubmissionRequest]
      result.isError mustBe true

      val errors = result.fold(identity, _ => fail("expected JsError"))
      errors.exists { case (path, _) => path == (JsPath \ "inactivity") } mustBe true
    }
  }
}
