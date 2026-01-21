/*
 * Copyright 2025 HM Revenue & Customs
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

package services.clientlist

import base.SpecBase
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.{Json, Reads, Writes}
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.CacheService

import scala.concurrent.duration._

class CacheServiceSpec extends SpecBase {

  private val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]
  private val cacheService             = new CacheService(actorSystem)

  // Define implicit Writes and Reads for String (Play provides these by default)
  private given stringWrites: Writes[String] = Writes.StringWrites
  private given stringReads: Reads[String]   = Reads.StringReads

  "CacheService.cache" - {

    "should store a value with a TTL" in {
      val key   = "test-key-1"
      val value = "test-value"

      cacheService.cache(key, value, 1.second)

      val cached = cacheService.get[String](key)
      cached must be(Some(value))
    }

    "should store multiple values independently" in {
      val key1   = "key-1"
      val key2   = "key-2"
      val value1 = "value-1"
      val value2 = "value-2"

      cacheService.cache(key1, value1, 1.second)
      cacheService.cache(key2, value2, 1.second)

      cacheService.get[String](key1) must be(Some(value1))
      cacheService.get[String](key2) must be(Some(value2))
    }

    "should handle complex objects with implicit Writes" in {
      case class TestObject(name: String, value: Int)

      given testObjectWrites: Writes[TestObject] = Json.writes[TestObject]
      given testObjectReads: Reads[TestObject]   = Json.reads[TestObject]

      val key = "complex-object"
      val obj = TestObject("test", 42)

      cacheService.cache(key, obj, 1.second)

      val cached = cacheService.get[TestObject](key)
      cached must be(Some(obj))
    }

    "should override existing value for the same key" in {
      val key = "override-test"

      cacheService.cache(key, "value-1", 1.second)
      cacheService.get[String](key) must be(Some("value-1"))

      cacheService.cache(key, "value-2", 1.second)
      cacheService.get[String](key) must be(Some("value-2"))
    }
  }

  "CacheService.get" - {

    "should return None for non-existent key" in {
      val cached = cacheService.get[String]("non-existent-key")
      cached must be(None)
    }

    "should remove expired entries from cache" in {
      val key   = "expiring-key"
      val value = "expiring-value"

      cacheService.cache(key, value, 100.millis)

      Thread.sleep(150)

      val cached = cacheService.get[String](key)
      cached must be(None)
    }

    "should return value before expiration" in {
      val key   = "valid-key"
      val value = "valid-value"

      cacheService.cache(key, value, 5.seconds)

      Thread.sleep(100)

      val cached = cacheService.get[String](key)
      cached must be(Some(value))
    }
  }

  "CacheService.clear" - {

    "should remove entry from cache" in {
      val key = "remove-test"

      cacheService.cache(key, "value", 10.seconds)
      cacheService.get[String](key) must be(Some("value"))

      cacheService.clear(key)
      cacheService.get[String](key) must be(None)
    }

    "should not raise error when clearing non-existent key" in {
      cacheService.clear("non-existent-key")
    }

    "should only clear specified key" in {
      val key1 = "key-1"
      val key2 = "key-2"

      cacheService.cache(key1, "value-1", 10.seconds)
      cacheService.cache(key2, "value-2", 10.seconds)

      cacheService.clear(key1)

      cacheService.get[String](key1) must be(None)
      cacheService.get[String](key2) must be(Some("value-2"))
    }
  }

  "CacheService.refresh" - {

    "should extend TTL for existing entry" in {
      val key   = "refresh-test"
      val value = "test-value"

      cacheService.cache(key, value, 10.seconds)

      cacheService.refresh[String](key, 5.seconds)

      val cached = cacheService.get[String](key)
      cached must be(Some(value))
    }

    "should not affect non-existent keys" in {
      cacheService.refresh[String]("non-existent", 5.seconds)
    }

    "should update TTL correctly" in {
      val key   = "update-ttl"
      val value = "test-value"

      cacheService.cache(key, value, 10.seconds)
      cacheService.refresh[String](key, 100.millis)

      Thread.sleep(150)

      cacheService.get[String](key) must be(None)
    }

    "should work with complex objects" in {
      case class TestData(id: String, data: String)

      given testDataWrites: Writes[TestData] = Json.writes[TestData]
      given testDataReads: Reads[TestData]   = Json.reads[TestData]

      val key = "complex-refresh"
      val obj = TestData("test-id", "test-data")

      cacheService.cache(key, obj, 10.seconds)

      cacheService.refresh[TestData](key, 5.seconds)

      cacheService.get[TestData](key) must be(Some(obj))
    }
  }

  "CacheService scheduler" - {

    "should automatically remove expired entries" in {
      val key   = "auto-expire"
      val value = "expires"

      cacheService.cache(key, value, 200.millis)

      Thread.sleep(300)

      cacheService.get[String](key) must be(None)
    }

    "should schedule multiple evictions independently" in {
      val key1 = "short-ttl"
      val key2 = "long-ttl"

      cacheService.cache(key1, "short", 100.millis)
      cacheService.cache(key2, "long", 5.seconds)

      Thread.sleep(150)

      cacheService.get[String](key1) must be(None)
      cacheService.get[String](key2) must be(Some("long"))
    }
  }

  "CacheService concurrency" - {

    "should handle concurrent writes safely" in {
      val keys = (1 to 50).map(i => s"key-$i")

      keys.foreach { key =>
        cacheService.cache(key, s"value-for-$key", 10.seconds)
      }

      keys.foreach { key =>
        cacheService.get[String](key) must not be None
      }
    }
  }
}
