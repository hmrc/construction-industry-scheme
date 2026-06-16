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

package uk.gov.hmrc.constructionindustryscheme.services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.mongodb.scala.model.Filters
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.{CurrentTimestampSupport, MongoComponent}
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport

import scala.concurrent.{ExecutionContext, Future}

class BatchPollerScheduledServiceIntegrationSpec
    extends SpecBase
    with DefaultPlayMongoRepositorySupport[Lock]
    with ScalaFutures
    with IntegrationPatience {

  private lazy val lockApp =
    GuiceApplicationBuilder()
      .configure("schedules.batch-poller-job.enabled" -> false)
      .overrides(bind[MongoComponent].toInstance(mongoComponent))
      .build()

  override protected val repository: MongoLockRepository = lockApp.injector.instanceOf[MongoLockRepository]

  private val appConfig = lockApp.injector.instanceOf[AppConfig]

  private val lockId = "batch-poller-job"

  private val batchPollerService = mock[BatchPollerService]

  private val service =
    new BatchPollerScheduledService(repository, new CurrentTimestampSupport(), appConfig, batchPollerService)

  private def lockDocFor(id: String): Option[Lock] =
    repository.collection.find(Filters.equal("_id", id)).headOption().futureValue

  "BatchPollerScheduledService.invoke" - {

    "skip the job, leaving the existing lock untouched, when another instance already holds the lock" in {
      val otherOwner = "another-instance"

      // another instance takes the lock first
      repository.takeLock(lockId, otherOwner, appConfig.batchPollerJobLockTtl).futureValue mustBe defined
      repository.isLocked(lockId, otherOwner).futureValue shouldBe true

      service.invoke.futureValue // completes without failing

      // the lock is still owned by the other instance - our service did not acquire or steal it
      repository.isLocked(lockId, otherOwner).futureValue shouldBe true
      lockDocFor(lockId).map(_.owner)                     shouldBe Some(otherOwner)

      verify(batchPollerService, never()).run()(any[ExecutionContext], any[HeaderCarrier])
    }

    "acquire the lock and run when no other instance holds it" in {
      when(batchPollerService.run()(any[ExecutionContext], any[HeaderCarrier]))
        .thenReturn(Future.unit)

      lockDocFor(lockId) shouldBe None

      service.invoke.futureValue // completes without failing

      // a lock has been created for the job (held/disowned to expire naturally, not released)
      lockDocFor(lockId) shouldBe defined

      verify(batchPollerService).run()(any[ExecutionContext], any[HeaderCarrier])
    }
  }
}