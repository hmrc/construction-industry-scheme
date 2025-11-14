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
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.apache.pekko.actor.ActorSystem
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.connectors.{ClientExchangeProxyConnector, DatacacheProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.{InProgress, InitiateDownload}
import uk.gov.hmrc.constructionindustryscheme.services.AuditService
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.{ClientListService, NoBusinessIntervalsException}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.concurrent.TrieMap

class ClientListServiceSpec extends SpecBase {

  private val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  private val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

  class TestClientListService(
                               datacacheProxyConnector: DatacacheProxyConnector,
                               clientExchangeProxyConnector: ClientExchangeProxyConnector,
                               audit: AuditService
                             )(implicit ec: ExecutionContext)
    extends ClientListService(
      datacacheProxyConnector,
      clientExchangeProxyConnector,
      audit,
      appConfig,
      actorSystem
    ) {

    // capture the wait plan actually used by processWithWaitPlan
    @volatile var lastWaitPlan: Option[AsynchronousProcessWaitTime] = None

    override protected def sleep(ms: Long): Future[Unit] =
      Future.unit

    override protected def logWaitPlan(business: Seq[Long], browserMs: Long): Unit =
      super.logWaitPlan(business, browserMs)
      lastWaitPlan = Some(
        AsynchronousProcessWaitTime(
          browserIntervalMs   = browserMs,
          businessIntervalsMs = business.toList
        )
      )
  }

  private def setupService(
   datacache: DatacacheProxyConnector = mock[DatacacheProxyConnector],
   clientExchangeProxy: ClientExchangeProxyConnector = mock[ClientExchangeProxyConnector],
   audit: AuditService = mock[AuditService]
  ): (TestClientListService, DatacacheProxyConnector, ClientExchangeProxyConnector, AuditService) = {
    val service = new TestClientListService(datacache, clientExchangeProxy, audit)
    (service, datacache, clientExchangeProxy, audit)
  }

  private def waitTimeCacheOf(service: ClientListService): TrieMap[String, AsynchronousProcessWaitTime] = {
    val field = classOf[ClientListService].getDeclaredField("waitTimeCache")
    field.setAccessible(true)
    field.get(service).asInstanceOf[TrieMap[String, AsynchronousProcessWaitTime]]
  }

  "AC1 - initial Succeeded should complete successfully" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.Succeeded))

    whenReady(service.process("cred-1")) { _ =>
      verify(audit, never()).clientListRetrievalFailed(any, any, any)(any)
    }
  }

  "AC2 - initial Failed should throw ClientListDownloadFailedException" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.Failed))

    val status = service.process("cred-1").futureValue

    status shouldBe ClientListStatus.Failed
    verify(audit).clientListRetrievalFailed(eqTo("cred-1"), eqTo("initial"), any[Option[String]])(any)
  }

  "AC3 - InitiateDownload should call client-exchange-proxy to fetch wait plan" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload), 
        Future.successful(ClientListStatus.Succeeded) 
      )

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(100L) 
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    whenReady(service.process("cred-1")) { _ =>
      verify(clientExchangeProxy, times(1)).initiate(any, any)(any)
    }
  }

  "AC4 - InitiateDownload with no business intervals should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.InitiateDownload))

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = Nil
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    val ex = service.process("cred-1").failed.futureValue

    ex shouldBe a[NoBusinessIntervalsException]

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("business"),
      eqTo(Some("no-business-intervals"))
    )(any)
  }

  "AC5 - should call datacache after each business interval" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()
    val businessIntervals = List(10L, 20L, 30L)

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.Succeeded)
      )

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = businessIntervals
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    whenReady(service.process("cred-1")) { _ =>
      verify(datacache, times(4))
        .getClientListDownloadStatus(any, any, any)(any)
    }
  }

  "AC6a - terminal Succeeded during business phase should abort loop and succeed" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.Succeeded)
      )

    whenReady(service.process("cred-1")) { _ =>
      verify(audit, never()).clientListRetrievalFailed(any, any, any)(any)
    }
  }

  "AC6b - terminal Failed during business phase should abort loop and fail" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.Failed)
      )

    val status = service.process("cred-1").futureValue
    status shouldBe ClientListStatus.Failed
  }

  "AC7 - InitiateDownload after final business interval should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InitiateDownload)
      )

    val status = service.process("cred-1").futureValue
    status shouldBe ClientListStatus.InitiateDownload

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("business#1"),
      eqTo(Some("initiate-on-final-business-interval"))
    )(any)
  }

  "AC8 & AC9 - InProgress after browser wait should throw in-progress exception and audit 3008" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress)
      )

    val status = service.process("cred-1").futureValue
    status shouldBe ClientListStatus.InProgress

    verify(audit).clientListRetrievalInProgress(
      eqTo("cred-1"),
      eqTo("browser")
    )(any)
  }

  "AC10 - InitiateDownload after browser wait should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InitiateDownload)
      )

    val status = service.process("cred-1").futureValue
    status shouldBe InitiateDownload

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("browser"),
      eqTo(Some("initiate-after-browser"))
    )(any)
  }

  "AC11a - initial InProgress should use cached intervals when present" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val cachedPlan = AsynchronousProcessWaitTime(
      browserIntervalMs   = 9999L,
      businessIntervalsMs = List(111L, 222L)
    )

    val cache = waitTimeCacheOf(service)
    cache.update("cred-1", cachedPlan)

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.Succeeded)
      )

    whenReady(service.process("cred-1")) { _ =>
      verify(clientExchangeProxy, never()).initiate(any[String], any[String])(any[HeaderCarrier])
      service.lastWaitPlan shouldBe Some(cachedPlan)
    }
  }

  "AC11b - initial InProgress should use default intervals when no cached plan exists" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val cache = waitTimeCacheOf(service)
    cache.remove("cred-2")

    val defaultPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = appConfig.cisDefaultBrowserIntervalMs,
      businessIntervalsMs = appConfig.cisDefaultBusinessIntervalsMs
    )

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.Succeeded)
      )

    whenReady(service.process("cred-2")) { _ =>
      verify(clientExchangeProxy, never()).initiate(any[String], any[String])(any[HeaderCarrier])
      service.lastWaitPlan shouldBe Some(defaultPlan)
    }
  }

  "AC12 - initial InProgress should follow business and browser phases same as InitiateDownload" in {
    val (service, datacache, clientExchangeProxy, audit) = setupService()

    val cache = waitTimeCacheOf(service)
    cache.remove("cred-1")

    val defaultPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = appConfig.cisDefaultBrowserIntervalMs,
      businessIntervalsMs = appConfig.cisDefaultBusinessIntervalsMs
    )

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress)
      )

    when(audit.clientListRetrievalInProgress(any,any)(any))
      .thenReturn(Future.successful(AuditResult.Success))

    val status = service.process("cred-1").futureValue

    status shouldBe InProgress
    verify(clientExchangeProxy, never()).initiate(any, any)(any)

    verify(audit).clientListRetrievalInProgress(
      eqTo("cred-1"),
      eqTo("browser")
    )(any[HeaderCarrier])

    service.lastWaitPlan shouldBe Some(defaultPlan)
    verify(datacache, times(4)).getClientListDownloadStatus(any, any, any)(any)
  }

}
