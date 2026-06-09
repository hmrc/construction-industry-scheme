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

package uk.gov.hmrc.constructionindustryscheme.repositories

import base.SpecBase
import play.api.libs.json.Json

import java.time.Instant

class JourneyHandoffDataSpec extends SpecBase {

  "JourneyHandoffData" - {

    "must serialise and deserialise" in {
      val instant = Instant.parse("2026-05-06T15:49:35.000Z")

      val model = JourneyHandoffData(
        id = "handoff-123",
        userId = "cred-123",
        journeyType = "amend-monthly-return",
        data = Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        ),
        lastUpdated = instant
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "id"          -> "handoff-123",
        "userId"      -> "cred-123",
        "journeyType" -> "amend-monthly-return",
        "data"        -> Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        ),
        "lastUpdated" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> instant.toEpochMilli.toString
          )
        )
      )

      json.as[JourneyHandoffData] mustBe model
    }
  }

  "JourneyHandoffDataKeys" - {

    "must contain expected field names" in {
      JourneyHandoffDataKeys.idField mustBe "id"
      JourneyHandoffDataKeys.userIdField mustBe "userId"
      JourneyHandoffDataKeys.journeyTypeField mustBe "journeyType"
      JourneyHandoffDataKeys.dataField mustBe "data"
      JourneyHandoffDataKeys.lastUpdatedField mustBe "lastUpdated"
    }
  }
}
