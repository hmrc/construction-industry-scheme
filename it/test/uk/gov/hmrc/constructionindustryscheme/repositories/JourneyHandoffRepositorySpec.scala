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
import com.mongodb.client.model.Filters

import java.time.Instant
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.concurrent.IntegrationPatience
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class JourneyHandoffRepositorySpec
    extends SpecBase
    with DefaultPlayMongoRepositorySupport[JourneyHandoffData]
    with IntegrationPatience {

  override protected val repository: JourneyHandoffRepository = newRepository()

  private def newRepository(): JourneyHandoffRepository =
    GuiceApplicationBuilder()
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent)
      )
      .build()
      .injector
      .instanceOf[JourneyHandoffRepository]

  private val userId      = "cred-123"
  private val journeyType = "amend-monthly-return"

  private val handoffData = Json.obj(
    "instanceId"   -> "1",
    "taxYear"      -> 2026,
    "taxMonth"     -> 4,
    "returnType"   -> "standard",
    "acceptedTime" -> "2026-04-20T21:49:19.702Z"
  )

  "create and get" - {

    "successfully saves and retrieves handoff data" in {
      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      val result = repository.get(handoffId, userId, journeyType).futureValue.value

      result.id          shouldBe handoffId
      result.userId      shouldBe userId
      result.journeyType shouldBe journeyType
      result.data        shouldBe handoffData
    }

    "creates a unique handoff id for each record" in {
      val firstId  = repository.create(userId, journeyType, handoffData).futureValue
      val secondId = repository.create(userId, journeyType, handoffData).futureValue

      firstId must not equal secondId

      repository.get(firstId, userId, journeyType).futureValue.value.id  shouldBe firstId
      repository.get(secondId, userId, journeyType).futureValue.value.id shouldBe secondId
    }
  }

  "get" - {

    "returns None if id does not exist" in {
      repository.get("does-not-exist", userId, journeyType).futureValue shouldBe None
    }

    "returns None if userId does not match" in {
      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      repository.get(handoffId, "different-user", journeyType).futureValue shouldBe None
    }

    "returns None if journeyType does not match" in {
      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      repository.get(handoffId, userId, "different-journey").futureValue shouldBe None
    }
  }

  "delete" - {

    "removes record and get returns None" in {
      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      repository.delete(handoffId, userId, journeyType).futureValue shouldBe true
      repository.get(handoffId, userId, journeyType).futureValue    shouldBe None
    }

    "does not delete record if userId does not match" in {
      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      repository.delete(handoffId, "different-user", journeyType).futureValue shouldBe true

      repository.get(handoffId, userId, journeyType).futureValue.value.id shouldBe handoffId
    }

    "deleting non-existent record returns true" in {
      repository.delete("unknown-id", userId, journeyType).futureValue shouldBe true
    }
  }

  "lastUpdated field" - {

    "persists lastUpdated timestamp" in {
      val beforeCreate = Instant.now().minusSeconds(1)

      val handoffId = repository.create(userId, journeyType, handoffData).futureValue

      val raw = mongoComponent.database
        .getCollection[BsonDocument]("journey-handoff-records")
        .find(Filters.eq("id", handoffId))
        .headOption()
        .futureValue
        .value

      val persistedLastUpdated =
        Instant.ofEpochMilli(raw.get("lastUpdated").asDateTime().getValue)

      persistedLastUpdated.isAfter(beforeCreate) shouldBe true
    }
  }
}
