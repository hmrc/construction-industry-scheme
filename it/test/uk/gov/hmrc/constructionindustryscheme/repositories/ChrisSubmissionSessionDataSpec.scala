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
import org.scalatest.matchers.should.Matchers.shouldBe

import java.time.Instant
import java.time.temporal.ChronoUnit
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.GetGovTalkStatusResponse
import ChrisSubmissionSessionData.given

class ChrisSubmissionSessionDataSpec extends SpecBase {

  "ChrisSubmissionSessionData serializes to JSON" in {
    val now = Instant.parse("2025-01-01T12:34:56.789Z")

    val data = ChrisSubmissionSessionData(
      submissionId = "sub-123",
      instanceId = "instance-123",
      correlationId = "corr-123",
      lastMessageDate = now,
      numPolls = 3,
      pollInterval = 10,
      pollUrl = "/poll/123",
      govTalkStatus = None
    )

    val json = Json.toJson(data)

    (json \ "submissionId").as[String]                       shouldBe "sub-123"
    (json \ "instanceId").as[String]                         shouldBe "instance-123"
    (json \ "correlationId").as[String]                      shouldBe "corr-123"
    (json \ "lastMessageDate").as[Instant]                   shouldBe now
    (json \ "numPolls").as[Int]                              shouldBe 3
    (json \ "pollInterval").as[Int]                          shouldBe 10
    (json \ "pollUrl").as[String]                            shouldBe "/poll/123"
    (json \ "govTalkStatus").asOpt[GetGovTalkStatusResponse] shouldBe None
  }

  "ChrisSubmissionSessionData deserializes from JSON" in {
    val now = Instant.parse("2025-02-02T01:02:03.456Z")

    val json = Json.obj(
      "submissionId"    -> "sub-456",
      "instanceId"      -> "instance-456",
      "correlationId"   -> "corr-456",
      "lastMessageDate" -> now,
      "numPolls"        -> 5,
      "pollInterval"    -> 15,
      "pollUrl"         -> "/poll/456",
      "govTalkStatus"   -> None
    )

    val result = json.validate[ChrisSubmissionSessionData]

    result.isSuccess shouldBe true
    result.get       shouldBe ChrisSubmissionSessionData(
      submissionId = "sub-456",
      instanceId = "instance-456",
      correlationId = "corr-456",
      lastMessageDate = now,
      numPolls = 5,
      pollInterval = 15,
      pollUrl = "/poll/456",
      govTalkStatus = None
    )
  }

  "Format is symmetrical (round-trip)" in {
    val now = Instant.now().truncatedTo(ChronoUnit.MILLIS)

    val data = ChrisSubmissionSessionData(
      submissionId = "sub-roundtrip",
      instanceId = "instance-roundtrip",
      correlationId = "corr-roundtrip",
      lastMessageDate = now,
      numPolls = 7,
      pollInterval = 20,
      pollUrl = "/poll/roundtrip",
      govTalkStatus = None
    )

    val json   = Json.toJson(data)
    val parsed = Json.fromJson[ChrisSubmissionSessionData](json).get

    parsed shouldBe data
  }
}
