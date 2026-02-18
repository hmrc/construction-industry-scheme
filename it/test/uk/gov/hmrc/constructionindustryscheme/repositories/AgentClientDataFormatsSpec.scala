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
import java.time.temporal.ChronoUnit

case class AgentClientData(id: String, data: String, lastUpdated: Instant)
object AgentClientDataFormats {
  given dateFormat: Format[Instant]     = uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat
  given format: Format[AgentClientData] = Json.format[AgentClientData]
}

class AgentClientDataFormatsSpec extends SpecBase {
  import AgentClientDataFormats.given

  "AgentClientData serializes to JSON" in {
    val now = Instant.parse("2024-10-01T12:34:56.789Z")
    val agentClientData = AgentClientData("abc123", "xyz-data", now)

    val json = Json.toJson(agentClientData)
    (json \ "id").as[String] shouldBe "abc123"
    (json \ "data").as[String] shouldBe "xyz-data"
    (json \ "lastUpdated").as[Instant] shouldBe now
  }

  "AgentClientData deserializes from JSON" in {
    val now = Instant.parse("2024-11-02T13:14:15.921Z")
    val json = Json.obj(
      "id" -> "id42",
      "data" -> "test-data",
      "lastUpdated" -> now
    )
    val result = json.validate[AgentClientData]
    result.isSuccess shouldBe true
    result.get shouldBe AgentClientData("id42", "test-data", now)
  }

  "Format is symmetrical (round-trip)" in {
    // Truncate to milliseconds to match formatter precision
    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)
    val agentClientData = AgentClientData("roundtrip", "data", now)
    val json = Json.toJson(agentClientData)
    val parsed = Json.fromJson[AgentClientData](json).get
    parsed shouldBe agentClientData
  }
}
