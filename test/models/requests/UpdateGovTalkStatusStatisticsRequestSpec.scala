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

import java.time.LocalDateTime
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateGovTalkStatusStatisticsRequest

class UpdateGovTalkStatusStatisticsRequestSpec extends AnyWordSpec with Matchers {

  "UpdateGovTalkStatusStatisticsRequest" should {
    "round trip to JSON" in {
      val model = UpdateGovTalkStatusStatisticsRequest(
        userIdentifier = "user-123",
        formResultID = "sub-123",
        lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        numPolls = 3,
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      val json = Json.toJson(model)

      json.validate[UpdateGovTalkStatusStatisticsRequest].get mustBe model
    }
  }
}
