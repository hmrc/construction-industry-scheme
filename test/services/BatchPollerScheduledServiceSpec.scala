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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.services.{BatchPollerScheduledService, BatchPollerService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.TimestampSupport
import uk.gov.hmrc.mongo.lock.{Lock, LockRepository}

import java.time.Instant
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class BatchPollerScheduledServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val lockRepository     = mock[LockRepository]
  private val timestampSupport   = mock[TimestampSupport]
  private val appConfig          = mock[AppConfig]
  private val batchPollerService = mock[BatchPollerService]

  private val now = Instant.parse("2026-06-03T00:00:00Z")

  override def beforeEach(): Unit = {
    super.beforeEach()
    org.mockito.Mockito.reset(lockRepository, timestampSupport, appConfig, batchPollerService)
    when(appConfig.batchPollerJobLockTtl).thenReturn(30.minutes)
    when(timestampSupport.timestamp()).thenReturn(now)
    // ScheduledLockService refreshes first; returning false routes through takeLock
    when(lockRepository.refreshExpiry(any(), any(), any())).thenReturn(Future.successful(false))
  }

  private def service =
    new BatchPollerScheduledService(lockRepository, timestampSupport, appConfig, batchPollerService)

  "BatchPollerScheduledService.invoke" - {

    "run the job and complete successfully when the lock is acquired" in {
      val lock = Lock("batch-poller-job", "owner", now, now.plusSeconds(1800))
      when(lockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(lock)))

      // body finished within the TTL window, so the lock is disowned (not released) to expire naturally
      when(lockRepository.disownLock(any(), any(), any())).thenReturn(Future.unit)

      when(
        batchPollerService.run()(using any[HeaderCarrier])
      ).thenReturn(Future.unit)

      service.invoke.futureValue // completes without failing

      verify(lockRepository).takeLock(any(), any(), any())
      verify(lockRepository).disownLock(any(), any(), any())
      verify(batchPollerService)
        .run()(using any[HeaderCarrier])
    }

    "skip the job without failing when the lock is held by another instance" in {
      when(lockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(None))

      service.invoke.futureValue // completes without failing

      verify(lockRepository).takeLock(any(), any(), any())
      verify(lockRepository, never()).disownLock(any(), any(), any())
      verify(batchPollerService, never())
        .run()(using any[HeaderCarrier])
    }
  }
}
