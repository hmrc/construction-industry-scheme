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
import org.scalatest.concurrent.IntegrationPatience
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

class FinalValidationDraftRepositorySpec
  extends SpecBase
    with DefaultPlayMongoRepositorySupport[FinalValidationDraftData]
    with IntegrationPatience {

  override protected val repository: FinalValidationDraftRepository =
    GuiceApplicationBuilder()
      .configure("schedules.batch-poller-job.enabled" -> false)
      .overrides(bind[MongoComponent].toInstance(mongoComponent))
      .build()
      .injector
      .instanceOf[FinalValidationDraftRepository]

  private val userId     = "cred-123"
  private val instanceId = "1"
  private val context    = "monthly"
  private val data       = Json.obj("subcontractors" -> Json.arr(Json.obj("subcontractorId" -> 10903L)))

  "create and get" - {

    "save and retrieve a draft" in {
      val id     = repository.create(userId, instanceId, context, data).futureValue
      val result = repository.get(id, userId, instanceId).futureValue.value

      result.id mustBe id
      result.userId mustBe userId
      result.instanceId mustBe instanceId
      result.context mustBe context
      result.data mustBe data
      result.version mustBe 0L
    }
  }

  "get" - {

    "return None when the draft does not exist" in {
      repository.get("unknown", userId, instanceId).futureValue mustBe None
    }

    "return None when userId does not match" in {
      val id = repository.create(userId, instanceId, context, data).futureValue

      repository.get(id, "different-user", instanceId).futureValue mustBe None
    }

    "return None when instanceId does not match" in {
      val id = repository.create(userId, instanceId, context, data).futureValue

      repository.get(id, userId, "different-instance").futureValue mustBe None
    }
  }

  "replace" - {

    "replace data and increment version" in {
      val id       = repository.create(userId, instanceId, context, data).futureValue
      val existing = repository.get(id, userId, instanceId).futureValue.value
      val updated  = Json.obj("status" -> "updated")

      repository.replace(existing, updated).futureValue mustBe true

      val result = repository.get(id, userId, instanceId).futureValue.value
      result.data mustBe updated
      result.version mustBe 1L
    }

    "return false for a stale version" in {
      val id       = repository.create(userId, instanceId, context, data).futureValue
      val existing = repository.get(id, userId, instanceId).futureValue.value

      repository.replace(existing, Json.obj("status" -> "first")).futureValue mustBe true
      repository.replace(existing, Json.obj("status" -> "stale")).futureValue mustBe false

      val result = repository.get(id, userId, instanceId).futureValue.value
      result.data mustBe Json.obj("status" -> "first")
      result.version mustBe 1L
    }
  }

  "delete" - {

    "delete a draft" in {
      val id = repository.create(userId, instanceId, context, data).futureValue

      repository.delete(id, userId, instanceId).futureValue mustBe true
      repository.get(id, userId, instanceId).futureValue mustBe None
    }

    "not delete a draft when userId does not match" in {
      val id = repository.create(userId, instanceId, context, data).futureValue

      repository.delete(id, "different-user", instanceId).futureValue mustBe true
      repository.get(id, userId, instanceId).futureValue.value.id mustBe id
    }
  }
}
