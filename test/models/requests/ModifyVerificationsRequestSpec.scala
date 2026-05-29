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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.{CreateVerifications, DeleteVerifications}
import uk.gov.hmrc.constructionindustryscheme.models.requests.ModifyVerificationsRequest

class ModifyVerificationsRequestSpec extends PlaySpec {
  "ModifyVerificationsRequest" should {

    "serialise to JSON when all property is present" in {
      val model = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

      Json.toJson(model) mustBe Json.obj(
        "instanceId"          -> "abc-123",
        "deleteVerifications" -> Json.obj(
          "verificationResourceReferences" -> Seq(111L, 222L)
        ),
        "createVerifications" -> Json.obj(
          "verificationBatchResourceRef"   -> 10L,
          "verificationResourceReferences" -> Seq(333L, 444L)
        )
      )
    }

    "serialise to JSON when deleteVerifications is absent" in {
      val model = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = None,
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

      Json.toJson(model) mustBe Json.obj(
        "instanceId"          -> "abc-123",
        "createVerifications" -> Json.obj(
          "verificationBatchResourceRef"   -> 10L,
          "verificationResourceReferences" -> Seq(333L, 444L)
        )
      )
    }

    "serialise to JSON when createVerifications is absent" in {
      val model = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = None
      )

      Json.toJson(model) mustBe Json.obj(
        "instanceId"          -> "abc-123",
        "deleteVerifications" -> Json.obj(
          "verificationResourceReferences" -> Seq(111L, 222L)
        )
      )
    }

    "deserialise from JSON when all property is present" in {
      val json = Json.obj(
        "instanceId"          -> "abc-123",
        "deleteVerifications" -> Json.obj(
          "verificationResourceReferences" -> Seq(111L, 222L)
        ),
        "createVerifications" -> Json.obj(
          "verificationBatchResourceRef"   -> 10L,
          "verificationResourceReferences" -> Seq(333L, 444L)
        )
      )

      json.as[ModifyVerificationsRequest] mustBe
        ModifyVerificationsRequest(
          instanceId = "abc-123",
          deleteVerifications = Some(
            DeleteVerifications(
              verificationResourceReferences = Seq(111L, 222L)
            )
          ),
          createVerifications = Some(
            CreateVerifications(
              verificationBatchResourceRef = 10L,
              verificationResourceReferences = Seq(333L, 444L)
            )
          )
        )
    }

    "deserialise from JSON when deleteVerifications is absent" in {
      val json = Json.obj(
        "instanceId"          -> "abc-123",
        "createVerifications" -> Json.obj(
          "verificationBatchResourceRef"   -> 10L,
          "verificationResourceReferences" -> Seq(333L, 444L)
        )
      )

      json.as[ModifyVerificationsRequest] mustBe
        ModifyVerificationsRequest(
          instanceId = "abc-123",
          deleteVerifications = None,
          createVerifications = Some(
            CreateVerifications(
              verificationBatchResourceRef = 10L,
              verificationResourceReferences = Seq(333L, 444L)
            )
          )
        )
    }

    "deserialise from JSON when createVerifications is absent" in {
      val json = Json.obj(
        "instanceId"          -> "abc-123",
        "deleteVerifications" -> Json.obj(
          "verificationResourceReferences" -> Seq(111L, 222L)
        )
      )

      json.as[ModifyVerificationsRequest] mustBe
        ModifyVerificationsRequest(
          instanceId = "abc-123",
          deleteVerifications = Some(
            DeleteVerifications(
              verificationResourceReferences = Seq(111L, 222L)
            )
          ),
          createVerifications = None
        )
    }

    "round trip to and from JSON" in {
      val model = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

      Json.toJson(model).as[ModifyVerificationsRequest] mustBe model
    }
  }
}
