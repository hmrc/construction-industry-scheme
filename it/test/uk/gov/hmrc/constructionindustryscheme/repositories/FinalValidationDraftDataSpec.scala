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

class FinalValidationDraftDataSpec extends SpecBase {

  private val lastUpdated =
    Instant.parse("2026-09-07T12:00:00Z")

  private val data =
    FinalValidationDraftData(
      id = "draft-123",
      userId = "cred-123",
      instanceId = "1",
      context = "monthly",
      data = Json.obj(
        "subcontractors" -> Json.arr()
      ),
      version = 0L,
      lastUpdated = lastUpdated
    )

  "FinalValidationDraftData" - {

    "serialise and deserialise" in {

      val json =
        Json.toJson(data)

      json.as[FinalValidationDraftData] mustBe data
    }
  }

  "FinalValidationDraftDataKeys" - {

    "contain the expected Mongo field names" in {

      FinalValidationDraftDataKeys.idField          mustBe "id"
      FinalValidationDraftDataKeys.userIdField      mustBe "userId"
      FinalValidationDraftDataKeys.instanceIdField  mustBe "instanceId"
      FinalValidationDraftDataKeys.contextField     mustBe "context"
      FinalValidationDraftDataKeys.dataField        mustBe "data"
      FinalValidationDraftDataKeys.versionField     mustBe "version"
      FinalValidationDraftDataKeys.lastUpdatedField mustBe "lastUpdated"
    }
  }
}
