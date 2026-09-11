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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}

import uk.gov.hmrc.constructionindustryscheme.models.response.UpdateSubcontractorResponse

final class UpdateSubcontractorResponseSpec extends PlaySpec {

  "UpdateSubcontractorResponse" should {

    val model = UpdateSubcontractorResponse(
      version = 6
    )

    "serialize to JSON" in {
      Json.toJson(model) mustBe Json.obj(
        "version" -> 6
      )
    }

    "deserialize from JSON" in {
      Json
        .obj("version" -> 6)
        .validate[UpdateSubcontractorResponse] mustBe JsSuccess(model)
    }

    "fail to deserialize when version is missing" in {
      Json
        .obj()
        .validate[UpdateSubcontractorResponse]
        .isError mustBe true
    }

    "fail to deserialize when version is not a number" in {
      Json
        .obj("version" -> "6")
        .validate[UpdateSubcontractorResponse]
        .isError mustBe true
    }
  }
}
