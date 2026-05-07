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

import com.google.inject.Inject
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import play.api.libs.json.JsObject
import uk.gov.hmrc.constructionindustryscheme.repositories.JourneyHandoffDataKeys.*
import java.time.Instant
import java.util.UUID

import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyHandoffRepository @Inject() (
  mongoComponent: MongoComponent,
  config: AppConfig
)(using ec: ExecutionContext)
  extends PlayMongoRepository[JourneyHandoffData](
    collectionName = "journey-handoff-records",
    mongoComponent = mongoComponent,
    domainFormat = JourneyHandoffData.format,
    indexes = Seq(
      IndexModel(
        Indexes.ascending(lastUpdatedField),
        IndexOptions()
          .name("lastUpdatedIndex")
          .expireAfter(config.cacheTtl, TimeUnit.SECONDS)
      ),
      IndexModel(
        Indexes.ascending(idField),
        IndexOptions().name("idIndex").unique(true).background(false)
      ),
      IndexModel(
        Indexes.ascending(userIdField, journeyTypeField),
        IndexOptions().name("userIdJourneyTypeIndex").background(true)
      )
    ),
    replaceIndexes = true
  ) {

  private def now: Instant = Instant.now()
  
  def get(id: String, userId: String, journeyType: String): Future[Option[JourneyHandoffData]] =
    collection
      .find(
        Filters.and(
          Filters.equal(idField, id),
          Filters.equal(userIdField, userId),
          Filters.equal(journeyTypeField, journeyType)
        )
      )
      .headOption()

  def create(userId: String, journeyType: String, data: JsObject): Future[String] =
    val id = UUID.randomUUID().toString
    
    val handoff = JourneyHandoffData(
      id = id,
      userId = userId,
      journeyType = journeyType,
      data = data,
      lastUpdated = now
    )
    
    collection
      .insertOne(handoff)
      .toFuture()
      .map(_ => id)

  def delete(id: String, userId: String, journeyType: String): Future[Boolean] =
    collection
      .deleteOne(
        Filters.and(
          Filters.equal(idField, id),
          Filters.equal(userIdField, userId),
          Filters.equal(journeyTypeField, journeyType)
        )
      )
      .toFuture()
      .map(_.wasAcknowledged())
}
