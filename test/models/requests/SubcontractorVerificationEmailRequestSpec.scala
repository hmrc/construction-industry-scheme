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
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.SubcontractorVerificationEmailRequest

class SubcontractorVerificationEmailRequestSpec extends SpecBase {

  "SubcontractorVerificationEmailRequest" - {

    "read from JSON" in {
      val json = Json.obj("email" -> "test@test.com")

      json.validate[SubcontractorVerificationEmailRequest] mustBe JsSuccess(
        SubcontractorVerificationEmailRequest("test@test.com")
      )
    }

    "write to JSON" in {
      val json = Json.toJson(SubcontractorVerificationEmailRequest("test@test.com"))

      (json \ "email").as[String] mustBe "test@test.com"
    }
  }
}
