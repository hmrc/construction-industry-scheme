/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mongodb.scala.bson.BsonDocument
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.matchers.should.Matchers.should
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.crypto.{Crypted, Decrypter, Encrypter, SymmetricCryptoFactory}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import java.time.Instant

class AgentClientRepositorySpec
  extends SpecBase
    with DefaultPlayMongoRepositorySupport[AgentClientData]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  override protected val repository: AgentClientRepository = newRepository(false)

  private def newRepository(toggle: Boolean): AgentClientRepository = {
    val app = GuiceApplicationBuilder()
      .configure(
        "encryptionToggle" -> toggle,
        "agentClientCrypto.key" -> "Bk/WzqlUJk4/M279rO+BJYVtLkRq4lxH9Wn2A0k9lqo="
      )
      .overrides(
        bind[MongoComponent].toInstance(mongoComponent)
      )
      .build()
    app.injector.instanceOf[AgentClientRepository]
  }

  private val userAnswersJson: JsValue =
    Json.obj("uniqueId" -> 123, "taxOfficeNumber" -> 345, "taxOfficeReference" -> "AB123")
  private val userAnswersCache =
    AgentClientData(
      "id",
      userAnswersJson.toString(),
      Instant.now()
    )

  "with crypto" - {
    val repository = newRepository(true)
    val cryptoKey = "Bk/WzqlUJk4/M279rO+BJYVtLkRq4lxH9Wn2A0k9lqo="
    val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(cryptoKey)

    "upsert" - {
      "successfully save and retrieve data" in {
        repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
        val result = repository.get(userAnswersCache.id).futureValue.value
        result mustBe userAnswersJson
      }

      "encrypt the payload in db" in {
        repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
        val raw = mongoComponent.database
          .getCollection[BsonDocument]("agent-client-records")
          .find()
          .headOption()
          .futureValue
          .value
        val decrypted = crypto.decrypt(Crypted(raw.get("data").asString.getValue)).value
        Json.parse(decrypted) mustBe userAnswersJson
      }
    }

    "get" - {
      "returns None if record does not exist" in {
        repository.get("does-not-exist").futureValue mustBe None
      }
    }
  }

  "without crypto" - {
    val repository = newRepository(false)
    val userAnswersCache = AgentClientData("id2", userAnswersJson.toString, Instant.now)

    "upsert" - {
      "successfully saves and retrieves unencrypted data" in {
        repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
        val result = repository.get(userAnswersCache.id).futureValue.value
        result mustBe userAnswersJson
      }

      "stores plain JSON in db" in {
        repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
        val raw = mongoComponent.database
          .getCollection[BsonDocument]("agent-client-records")
          .find()
          .headOption()
          .futureValue
          .value
        val bsonDoc = raw.get("data").asDocument()
        val jsValue = Json.parse(bsonDoc.toJson())
        jsValue mustBe userAnswersJson

      }
    }

    "get" - {
      "returns None if record does not exist" in {
        repository.get("missing-id").futureValue mustBe None
      }
    }
  }

  "remove" - {
    val repository = newRepository(false)
    "removes record and get returns None" in {
      repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
      repository.remove(userAnswersCache.id).futureValue
      repository.get(userAnswersCache.id).futureValue mustBe None
    }

    "removing non-existent record returns true (idempotent)" in {
      repository.remove("unknown-id").futureValue mustBe true
    }
  }

  "lastUpdated field" - {
    val repository = newRepository(false)
    "persists and updates lastUpdated timestamp" in {
      repository.upsert(userAnswersCache.id, userAnswersJson).futureValue
      val raw = mongoComponent.database
        .getCollection[BsonDocument]("agent-client-records")
        .find()
        .headOption()
        .futureValue
        .value
      val dbMillis = raw.get("lastUpdated").asDateTime().getValue
      Instant.ofEpochMilli(dbMillis).isBefore(Instant.now.plusSeconds(5)) mustBe true
    }
  }
}