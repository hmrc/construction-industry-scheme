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

import java.time.Instant
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class ChrisSubmissionSessionRepositorySpec
    extends SpecBase
    with DefaultPlayMongoRepositorySupport[ChrisSubmissionSessionData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  override protected val repository: ChrisSubmissionSessionRepository = newRepository()

  private def newRepository(): ChrisSubmissionSessionRepository =
    GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent)
      )
      .build()
      .injector
      .instanceOf[ChrisSubmissionSessionRepository]

  private val sessionData = ChrisSubmissionSessionData(
    submissionId = "sub-123",
    instanceId = "instance-123",
    correlationId = "corr-123",
    lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
    numPolls = 3,
    pollInterval = 10,
    pollUrl = "/poll/123",
    govTalkStatus = None
  )

  "upsert" - {
    "successfully saves and retrieves data" in {
      repository.upsert(sessionData).futureValue

      repository.get(sessionData.submissionId).futureValue.value shouldBe sessionData
    }

    "updates an existing record for the same submissionId" in {
      val updated = sessionData.copy(
        correlationId = "corr-999",
        numPolls = 9,
        pollInterval = 30,
        pollUrl = "/poll/999"
      )

      repository.upsert(sessionData).futureValue
      repository.upsert(updated).futureValue

      repository.get(sessionData.submissionId).futureValue.value shouldBe updated
    }
  }

  "get" - {
    "returns None if record does not exist" in {
      repository.get("does-not-exist").futureValue shouldBe None
    }
  }

  "delete" - {
    "removes record and get returns None" in {
      repository.upsert(sessionData).futureValue

      repository.delete(sessionData.submissionId).futureValue shouldBe true
      repository.get(sessionData.submissionId).futureValue    shouldBe None
    }

    "deleting non-existent record returns true" in {
      repository.delete("unknown-id").futureValue shouldBe true
    }
  }

  "lastMessageDate field" - {
    "persists lastMessageDate timestamp" in {
      repository.upsert(sessionData).futureValue

      val raw = mongoComponent.database
        .getCollection[BsonDocument]("chris-submission-session-records")
        .find()
        .headOption()
        .futureValue
        .value

      Instant.ofEpochMilli(raw.get("lastMessageDate").asDateTime().getValue) shouldBe sessionData.lastMessageDate
    }
  }
}
