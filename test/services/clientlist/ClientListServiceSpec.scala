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
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.{CacheService, ClientListService, NoBusinessIntervalsException}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

import scala.concurrent.{ExecutionContext, Future}
import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import play.api.libs.json.{Format, Writes}

class ClientListServiceSpec extends SpecBase {

  private val appConfig: AppConfig     = app.injector.instanceOf[AppConfig]
  private val actorSystem: ActorSystem = app.injector.instanceOf[ActorSystem]

  class TestClientListService(
    datacacheProxyConnector: DatacacheProxyConnector,
    clientExchangeProxyConnector: ClientExchangeProxyConnector,
    audit: AuditService,
    cacheService: CacheService
  )(implicit ec: ExecutionContext)
      extends ClientListService(
        datacacheProxyConnector,
        clientExchangeProxyConnector,
        audit,
        appConfig,
        cacheService,
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
          browserIntervalMs = browserMs,
          businessIntervalsMs = business.toList
        )
      )
  }

  private def setupService(
    datacache: DatacacheProxyConnector = mock[DatacacheProxyConnector],
    clientExchangeProxy: ClientExchangeProxyConnector = mock[ClientExchangeProxyConnector],
    audit: AuditService = mock[AuditService]
  ): (TestClientListService, DatacacheProxyConnector, ClientExchangeProxyConnector, AuditService, CacheService) = {
    val cacheService: CacheService = new RealCacheService()

    val service = new TestClientListService(datacache, clientExchangeProxy, audit, cacheService)
    (service, datacache, clientExchangeProxy, audit, cacheService)
  }

  private def waitTimeCacheOf(service: ClientListService): TrieMap[String, AsynchronousProcessWaitTime] = {
    val field = classOf[ClientListService].getDeclaredField("waitTimeCache")
    field.setAccessible(true)
    field.get(service).asInstanceOf[TrieMap[String, AsynchronousProcessWaitTime]]
  }

  "AC1 - initial Succeeded should complete successfully" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.Succeeded))

    whenReady(service.process("cred-1", "agent-001")) { _ =>
      verify(audit, never()).clientListRetrievalFailed(any, any, any)(any)
    }
  }

  "AC2 - initial Failed should throw ClientListDownloadFailedException" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.Failed))

    val status = service.process("cred-1", "agent-001").futureValue

    status shouldBe ClientListStatus.Failed
    verify(audit).clientListRetrievalFailed(eqTo("cred-1"), eqTo("initial"), any[Option[String]])(any)
  }

  "AC3 - InitiateDownload should call client-exchange-proxy to fetch wait plan" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.Succeeded)
      )

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(100L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    whenReady(service.process("cred-1", "agent-001")) { _ =>
      verify(clientExchangeProxy, times(1)).initiate(any, any, any)(any)
    }
  }

  "AC4 - InitiateDownload with no business intervals should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.InitiateDownload))

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = Nil
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    val ex = service.process("cred-1", "agent-001").failed.futureValue

    ex shouldBe a[NoBusinessIntervalsException]

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("business"),
      eqTo(Some("no-business-intervals"))
    )(any)
  }

  "AC5 - should call datacache after each business interval" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()
    val businessIntervals                                       = List(10L, 20L, 30L)

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

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    whenReady(service.process("cred-1", "agent-001")) { _ =>
      verify(datacache, times(4))
        .getClientListDownloadStatus(any, any, any)(any)
    }
  }

  "AC6a - terminal Succeeded during business phase should abort loop and succeed" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.Succeeded)
      )

    whenReady(service.process("cred-1", "agent-001")) { _ =>
      verify(audit, never()).clientListRetrievalFailed(any, any, any)(any)
    }
  }

  "AC6b - terminal Failed during business phase should abort loop and fail" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.Failed)
      )

    val status = service.process("cred-1", "agent-001").futureValue
    status shouldBe ClientListStatus.Failed
  }

  "AC7 - InitiateDownload after final business interval should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InitiateDownload)
      )

    val status = service.process("cred-1", "agent-001").futureValue
    status shouldBe ClientListStatus.InitiateDownload

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("business#1"),
      eqTo(Some("initiate-on-final-business-interval"))
    )(any)
  }

  "AC8 & AC9 - InProgress after browser wait should throw in-progress exception and audit 3008" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress)
      )

    val status = service.process("cred-1", "agent-001").futureValue
    status shouldBe ClientListStatus.InProgress

    verify(audit).clientListRetrievalInProgress(
      eqTo("cred-1"),
      eqTo("browser")
    )(any)
  }

  "AC10 - InitiateDownload after browser wait should throw SystemException and audit 3046" in {
    val (service, datacache, clientExchangeProxy, audit, cache) = setupService()

    val waitPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 1000L,
      businessIntervalsMs = List(10L, 20L)
    )

    when(clientExchangeProxy.initiate(any, any, any)(any))
      .thenReturn(Future.successful(waitPlan))

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(
        Future.successful(ClientListStatus.InitiateDownload),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InProgress),
        Future.successful(ClientListStatus.InitiateDownload)
      )

    val status = service.process("cred-1", "agent-001").futureValue
    status shouldBe InitiateDownload

    verify(audit).clientListRetrievalFailed(
      eqTo("cred-1"),
      eqTo("browser"),
      eqTo(Some("initiate-after-browser"))
    )(any)
  }

  "AC11a - initial InProgress should use cached intervals when present" in {
    val (service, datacache, clientExchangeProxy, audit, _) = setupService()

    val cachedPlan = AsynchronousProcessWaitTime(
      browserIntervalMs = 9999L,
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

    whenReady(service.process("cred-1", "agent-001")) { _ =>
      verify(clientExchangeProxy, never()).initiate(any[String], any[String], any[String])(any[HeaderCarrier])
      service.lastWaitPlan shouldBe Some(cachedPlan)
    }
  }

  "AC11b - initial InProgress should use default intervals when no cached plan exists" in {
    val (service, datacache, clientExchangeProxy, audit, _) = setupService()

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

    whenReady(service.process("cred-2", "agent-002")) { _ =>
      verify(clientExchangeProxy, never()).initiate(any[String], any[String], any[String])(any[HeaderCarrier])
      service.lastWaitPlan shouldBe Some(defaultPlan)
    }
  }

  "AC12 - initial InProgress should follow business and browser phases same as InitiateDownload" in {
    val (service, datacache, clientExchangeProxy, audit, _) = setupService()

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

    when(audit.clientListRetrievalInProgress(any, any)(any))
      .thenReturn(Future.successful(AuditResult.Success))

    val status = service.process("cred-1", "agent-001").futureValue

    status shouldBe InProgress
    verify(clientExchangeProxy, never()).initiate(any, any, any)(any)

    verify(audit).clientListRetrievalInProgress(
      eqTo("cred-1"),
      eqTo("browser")
    )(any[HeaderCarrier])

    service.lastWaitPlan shouldBe Some(defaultPlan)
    verify(datacache, times(4)).getClientListDownloadStatus(any, any, any)(any)
  }

  "getStatus should delegate to datacacheProxyConnector with serviceName and grace" in {
    val (service, datacache, _, _, _) = setupService()

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val expectedServiceName = appConfig.cisServiceName
    val expectedGrace       = appConfig.cisGracePeriodSeconds

    when(datacache.getClientListDownloadStatus(any, any, any)(any))
      .thenReturn(Future.successful(ClientListStatus.Succeeded))

    val status = service.getStatus("cred-1").futureValue

    status shouldBe ClientListStatus.Succeeded
    verify(datacache, times(1)).getClientListDownloadStatus(
      eqTo("cred-1"),
      eqTo(expectedServiceName),
      eqTo(expectedGrace)
    )(any[HeaderCarrier])
  }

  "ClientListService.getClientList" - {

    val irAgentId = "SA123456"
    val credId    = "cred-123"

    "should return ClientSearchResult when connector succeeds" in {
      val (service, datacache, _, _, cache) = setupService()

      val expectedResult = ClientSearchResult(
        clients = List(
          CisTaxpayerSearchResult(
            uniqueId = "client-1",
            taxOfficeNumber = "111",
            taxOfficeRef = "test111",
            aoDistrict = Some("district1"),
            aoPayType = Some("type1"),
            aoCheckCode = Some("check1"),
            aoReference = Some("ref1"),
            validBusinessAddr = Some("Y"),
            correlation = Some("corr1"),
            ggAgentId = Some("agent1"),
            employerName1 = Some("Test Company Ltd"),
            employerName2 = Some("Test Company"),
            agentOwnRef = Some("own-ref-1"),
            schemeName = Some("Test Scheme")
          )
        ),
        totalCount = 1,
        clientNameStartingCharacters = List("T")
      )

      when(datacache.getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(expectedResult))

      val result = service.getClientList(irAgentId, credId).futureValue

      result                              shouldBe expectedResult
      result.totalCount                   shouldBe 1
      result.clients.size                 shouldBe 1
      result.clients.head.uniqueId        shouldBe "client-1"
      result.clientNameStartingCharacters shouldBe List("T")

      verify(datacache, times(1)).getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier])
    }

    "should return empty ClientSearchResult when no clients found" in {
      val (service, datacache, _, _, cache) = setupService()

      val emptyResult = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List.empty
      )

      when(datacache.getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier]))
        .thenReturn(Future.successful(emptyResult))

      val result = service.getClientList(irAgentId, credId).futureValue

      result.totalCount                   shouldBe 0
      result.clients                      shouldBe empty
      result.clientNameStartingCharacters shouldBe empty

      verify(datacache, times(1)).getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier])
    }

    "should propagate UpstreamErrorResponse when connector fails with 400" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("Bad request", 400, 400)))

      val ex = service.getClientList(irAgentId, credId).failed.futureValue

      ex                                                shouldBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe 400
      ex.getMessage                                     shouldBe "Bad request"
    }

    "should propagate UpstreamErrorResponse when connector fails with 500" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.getClientList(eqTo(irAgentId), eqTo(credId))(using any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("Internal server error", 500, 500)))

      val ex = service.getClientList(irAgentId, credId).failed.futureValue

      ex                                                shouldBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe 500
      ex.getMessage                                     shouldBe "Internal server error"
    }

    "should delegate to datacache connector with correct parameters" in {
      val (service, datacache, _, _, cache) = setupService()

      val testIrAgentId = "TEST123"
      val testCredId    = "test-cred"

      val result = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List.empty
      )

      when(datacache.getClientList(any[String], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(result))

      service.getClientList(testIrAgentId, testCredId).futureValue

      verify(datacache, times(1)).getClientList(eqTo(testIrAgentId), eqTo(testCredId))(using any[HeaderCarrier])
    }
  }

  "ClientListService.hasClient" - {

    val taxOfficeNumber    = "123"
    val taxOfficeReference = "AB456"
    val agentId            = "SA123456"
    val credId             = "cred-123"

    "return true when hasClient from connector returns true" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.hasClient(any, any, any, any)(any))
        .thenReturn(Future.successful(true))

      val result = service
        .hasClient(taxOfficeNumber, taxOfficeReference, agentId, credId)(using
          mock[HeaderCarrier]
        )
        .futureValue

      result shouldBe true
      verify(datacache, times(1)).hasClient(any, any, any, any)(any)
    }

    "return false when hasClient from connector returns false" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.hasClient(any, any, any, any)(any))
        .thenReturn(Future.successful(false))

      val result = service
        .hasClient(taxOfficeNumber, taxOfficeReference, agentId, credId)(using
          mock[HeaderCarrier]
        )
        .futureValue

      result shouldBe false
      verify(datacache, times(1)).hasClient(any, any, any, any)(any)
    }

    "return cached result when cache hit occurs" in {
      val (service, datacache, _, _, cache) = setupService()

      // Pre-populate cache
      cache.cache[Boolean](s"hasClient:$taxOfficeNumber:$taxOfficeReference:$agentId:$credId", true, 1.hour)

      val result = service
        .hasClient(taxOfficeNumber, taxOfficeReference, agentId, credId)(using
          mock[HeaderCarrier]
        )
        .futureValue

      result shouldBe true
      // Verify connector was never called (cache hit)
      verify(datacache, never()).hasClient(any, any, any, any)(any)
    }

    "cache the result when cache miss occurs" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.hasClient(any, any, any, any)(any))
        .thenReturn(Future.successful(true))

      val result = service
        .hasClient(taxOfficeNumber, taxOfficeReference, agentId, credId)(using
          mock[HeaderCarrier]
        )
        .futureValue

      result shouldBe true

      // Verify result is in cache now
      val cachedResult = cache.get[Boolean](s"hasClient:$taxOfficeNumber:$taxOfficeReference:$agentId:$credId")
      cachedResult shouldBe Some(true)
    }

    "propagate connector errors" in {
      val (service, datacache, _, _, cache) = setupService()

      when(datacache.hasClient(any, any, any, any)(any))
        .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", 503, 503)))

      val ex = service
        .hasClient(taxOfficeNumber, taxOfficeReference, agentId, credId)(using
          mock[HeaderCarrier]
        )
        .failed
        .futureValue

      ex                                                shouldBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode shouldBe 503
    }
  }

  class RealCacheService extends CacheService(actorSystem) {
    // Simple test cache implementation that bypasses ActorSystem
    override def cache[T](key: String, value: T, ttl: FiniteDuration)(using Format[T]): Unit =
      super.cache(key, value, 10.hours) // Use longer TTL for tests to avoid expiration
  }

}
