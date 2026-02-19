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
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*
import java.time.Instant

class JsonDataEntrySpec extends SpecBase {
  import uk.gov.hmrc.constructionindustryscheme.repositories.JsonDataEntry.given

  "JsonDataEntry serializes to JSON" in {
    val now = Instant.parse("2024-05-01T10:15:30.123Z")
    val entry = JsonDataEntry("foo", Json.obj("a" -> 1, "b" -> "bar"), now)
    val json = Json.toJson(entry)

    (json \ "id").as[String] shouldBe "foo"
    (json \ "data").as[JsValue] shouldBe Json.obj("a" -> 1, "b" -> "bar")
    (json \ "lastUpdated").as[Instant] shouldBe now
  }

  "JsonDataEntry deserializes from JSON" in {
    val now = Instant.parse("2024-05-01T13:55:01.001Z")
    val js = Json.obj(
      "id" -> "random",
      "data" -> Json.obj("x" -> 5),
      "lastUpdated" -> now
    )

    val result = js.validate[JsonDataEntry]
    result.isSuccess shouldBe true
    result.get shouldBe JsonDataEntry("random", Json.obj("x" -> 5), now)
  }

  "Format is symmetrical (round-trip)" in {
    val entry = JsonDataEntry("rt", Json.obj("qq" -> 999), Instant.now.truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
    val parsed = Json.fromJson[JsonDataEntry](Json.toJson(entry)).get
    parsed shouldBe entry
  }
}