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

package uk.gov.hmrc.constructionindustryscheme.services.clientlist

import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.{Json, OFormat, Reads, Writes}
import uk.gov.hmrc.constructionindustryscheme.models.CacheItem

import javax.inject.{Inject, Singleton}
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

@Singleton
class CacheService @Inject(actorSystem: ActorSystem) {

  private val cache: TrieMap[String, String] = TrieMap.empty

  def cache[T](key: String, value: T, ttl: FiniteDuration)(using Writes[T]): Unit = {
    val expiresAt = System.currentTimeMillis() + ttl.toMillis
    cache.update(key, Json.toJson(CacheItem[T](value, expiresAt)).toString)
    scheduleEviction(key, ttl)
  }

  def get[T](key: String)(using Reads[T]): Option[T] = {
    cache.get(key).flatMap { cachedString =>
      Json.parse(cachedString).asOpt[CacheItem[T]].flatMap { cachedJson =>
        if (System.currentTimeMillis() < cachedJson.expiresAt) {
          Some(cachedJson.value)
        } else {
          cache.remove(key)
          None
        }
      }
    }
  }

  def clear(key: String): Unit =
    cache.remove(key)

  def refresh[T](key: String, ttl: FiniteDuration)(using Reads[T], Writes[T]): Unit = {
    for {
      cachedString <- cache.get(key)
      cachedItem <- Json.parse(cachedString).asOpt[CacheItem[T]]
    } {
      val expiresAt = System.currentTimeMillis() + ttl.toMillis
      cache.update(key, Json.toJson(cachedItem.copy(expiresAt = expiresAt)).toString)
      scheduleEviction(key, ttl)
    }
  }

  private def scheduleEviction(key: String, ttl: FiniteDuration): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    actorSystem.scheduler.scheduleOnce(ttl) {
      cache.remove(key)
    }
  }
}