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
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateVerificationBatchAndVerificationsRequest

class CreateVerificationBatchAndVerificationsRequestSpec extends PlaySpec {

  "CreateVerificationBatchAndVerificationsRequest" should {

    "serialise to JSON when actionIndicator is present" in {
      val model = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "instance-123",
        verificationResourceReferences = Seq(123L, 456L, 789L),
        actionIndicator = Some("A")
      )

      Json.toJson(model) mustBe Json.obj(
        "instanceId"                     -> "instance-123",
        "verificationResourceReferences" -> Json.arr(123, 456, 789),
        "actionIndicator"                -> "A"
      )
    }

    "serialise to JSON when actionIndicator is absent" in {
      val model = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "instance-123",
        verificationResourceReferences = Seq(123L, 456L),
        actionIndicator = None
      )

      Json.toJson(model) mustBe Json.obj(
        "instanceId"                     -> "instance-123",
        "verificationResourceReferences" -> Json.arr(123, 456)
      )
    }

    "deserialise from JSON when actionIndicator is present" in {
      val json = Json.obj(
        "instanceId"                     -> "instance-123",
        "verificationResourceReferences" -> Json.arr(123, 456, 789),
        "actionIndicator"                -> "A"
      )

      json.as[CreateVerificationBatchAndVerificationsRequest] mustBe
        CreateVerificationBatchAndVerificationsRequest(
          instanceId = "instance-123",
          verificationResourceReferences = Seq(123L, 456L, 789L),
          actionIndicator = Some("A")
        )
    }

    "deserialise from JSON when actionIndicator is missing" in {
      val json = Json.obj(
        "instanceId"                     -> "instance-123",
        "verificationResourceReferences" -> Json.arr(123, 456)
      )

      json.as[CreateVerificationBatchAndVerificationsRequest] mustBe
        CreateVerificationBatchAndVerificationsRequest(
          instanceId = "instance-123",
          verificationResourceReferences = Seq(123L, 456L),
          actionIndicator = None
        )
    }

    "round trip to and from JSON" in {
      val model = CreateVerificationBatchAndVerificationsRequest(
        instanceId = "instance-123",
        verificationResourceReferences = Seq(123L, 456L, 789L),
        actionIndicator = Some("A")
      )

      Json.toJson(model).as[CreateVerificationBatchAndVerificationsRequest] mustBe model
    }
  }
}
