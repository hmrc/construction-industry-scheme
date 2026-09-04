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
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes, ReplaceOptions}
import play.api.libs.json.JsObject
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinalValidationDraftRepository @Inject() (
  mongoComponent: MongoComponent,
  config: AppConfig
)(using ec: ExecutionContext)
    extends PlayMongoRepository[FinalValidationDraftData](
      collectionName = "final-validation-drafts",
      mongoComponent = mongoComponent,
      domainFormat = FinalValidationDraftData.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending(FinalValidationDraftDataKeys.lastUpdatedField),
          IndexOptions()
            .name("lastUpdatedIndex")
            .expireAfter(config.cacheTtl, TimeUnit.SECONDS)
        ),
        IndexModel(
          Indexes.ascending(FinalValidationDraftDataKeys.idField),
          IndexOptions().name("idIndex").unique(true).background(false)
        ),
        IndexModel(
          Indexes.ascending(FinalValidationDraftDataKeys.userIdField, FinalValidationDraftDataKeys.instanceIdField),
          IndexOptions().name("userIdInstanceIdIndex").background(true)
        )
      ),
      replaceIndexes = true
    ) {

  private def now: Instant = Instant.now()

  def get(id: String, userId: String, instanceId: String): Future[Option[FinalValidationDraftData]] =
    collection
      .find(
        Filters.and(
          Filters.equal(FinalValidationDraftDataKeys.idField, id),
          Filters.equal(FinalValidationDraftDataKeys.userIdField, userId),
          Filters.equal(FinalValidationDraftDataKeys.instanceIdField, instanceId)
        )
      )
      .headOption()

  def create(userId: String, instanceId: String, context: String, data: JsObject): Future[String] = {
    val id = UUID.randomUUID().toString
    val draft = FinalValidationDraftData(
      id = id,
      userId = userId,
      instanceId = instanceId,
      context = context,
      data = data,
      version = 0L,
      lastUpdated = now
    )
    collection.insertOne(draft).toFuture().map(_ => id)
  }

  def replace(existing: FinalValidationDraftData, data: JsObject): Future[Boolean] = {
    val replacement = existing.copy(
      data = data,
      version = existing.version + 1,
      lastUpdated = now
    )

    collection.replaceOne(
      Filters.and(
        Filters.equal(FinalValidationDraftDataKeys.idField, existing.id),
        Filters.equal(FinalValidationDraftDataKeys.userIdField, existing.userId),
        Filters.equal(FinalValidationDraftDataKeys.instanceIdField, existing.instanceId),
        Filters.equal(FinalValidationDraftDataKeys.versionField, existing.version)
      ),
      replacement,
      ReplaceOptions().upsert(false)
    ).toFuture().map(_.getModifiedCount == 1)
  }

  def delete(id: String, userId: String, instanceId: String): Future[Boolean] =
    collection
      .deleteOne(
        Filters.and(
          Filters.equal(FinalValidationDraftDataKeys.idField, id),
          Filters.equal(FinalValidationDraftDataKeys.userIdField, userId),
          Filters.equal(FinalValidationDraftDataKeys.instanceIdField, instanceId)
        )
      )
      .toFuture()
      .map(_.wasAcknowledged())
}
