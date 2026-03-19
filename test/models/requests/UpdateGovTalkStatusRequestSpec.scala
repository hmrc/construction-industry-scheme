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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateGovTalkStatusRequest

class UpdateGovTalkStatusRequestSpec extends AnyWordSpec with Matchers {

  "UpdateGovTalkStatusRequest format" should {
    "round-trip json" in {
      val request = UpdateGovTalkStatusRequest(
        userIdentifier = "user-123",
        formResultID = "form-456",
        protocolStatus = "dataRequest"
      )

      Json.toJson(request).validate[UpdateGovTalkStatusRequest] shouldBe JsSuccess(request)
    }
  }
}
