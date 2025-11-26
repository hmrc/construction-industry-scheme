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

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.*
import scala.collection.concurrent.TrieMap
import uk.gov.hmrc.constructionindustryscheme.connectors.{ClientExchangeProxyConnector, DatacacheProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.*
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.services.AuditService
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.models.{AsynchronousProcessWaitTime, CisTaxpayer, ClientListStatus, EmployerReference}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

final case class ClientListDownloadFailedException(msg: String) extends RuntimeException(msg)

final case class ClientListDownloadInProgressException(msg: String) extends RuntimeException(msg)

final case class SystemException(msg: String) extends RuntimeException(msg)

final case class NoBusinessIntervalsException(msg: String) extends RuntimeException(msg)

@Singleton
class ClientListService @Inject()(
  datacacheProxyConnector: DatacacheProxyConnector,
  clientExchangeProxyConnector: ClientExchangeProxyConnector,
  audit: AuditService,
  appConfig: AppConfig,
  actorSystem: ActorSystem
)(implicit ec: ExecutionContext) extends Logging {

  private val serviceName = appConfig.cisServiceName
  private val grace = appConfig.cisGracePeriodSeconds

  def getClientList(irAgentId: String, credentialId: String)(using HeaderCarrier): Future[ClientSearchResult] = {
    datacacheProxyConnector.getClientList(irAgentId, credentialId)
  }

  def getClientTaxpayer(
   irAgentId: String,
   credentialId: String,
   uniqueId: String
  )(implicit hc: HeaderCarrier): Future[CisTaxpayer] = {

    datacacheProxyConnector
      .getClientList(irAgentId, credentialId)
      .flatMap { clientSearchResult =>
        val maybeClient =
          clientSearchResult.clients.find(_.uniqueId == uniqueId)

        maybeClient match {
          case Some(client) =>
            val employerRef = EmployerReference( 
              taxOfficeNumber = client.taxOfficeNumber,
              taxOfficeReference = client.taxOfficeRef
            )

            datacacheProxyConnector.getCisTaxpayer(employerRef)

          case None =>
            Future.failed(
              new NoSuchElementException(
                s"Client with uniqueId=$uniqueId not found for agent $irAgentId"
              )
            )
        }
      }
  }
  
  // ------------------------------------------------------------
  // Cache: wait plans per credentialId
  // ------------------------------------------------------------

  private val waitTimeCache: TrieMap[String, AsynchronousProcessWaitTime] = TrieMap.empty

  private def cacheWaitTime(credentialId: String, waitTime: AsynchronousProcessWaitTime): Unit =
    waitTimeCache.update(credentialId, waitTime)

  private def getCachedWaitTime(credentialId: String): Option[AsynchronousProcessWaitTime] =
    waitTimeCache.get(credentialId)

  private def clearWaitTime(credentialId: String): Unit =
    waitTimeCache.remove(credentialId)

  private def defaultWaitPlan: AsynchronousProcessWaitTime =
    AsynchronousProcessWaitTime(
      browserIntervalMs   = appConfig.cisDefaultBrowserIntervalMs,
      businessIntervalsMs = appConfig.cisDefaultBusinessIntervalsMs
    )

  // ------------------------------------------------------------
  // Helpers
  // ------------------------------------------------------------

  protected def sleep(ms: Long): Future[Unit] =
    val p = Promise[Unit]()
    actorSystem.scheduler.scheduleOnce(ms.millis)(p.success(()))
    p.future

  private def getStatus(credId: String)(implicit hc: HeaderCarrier): Future[ClientListStatus] =
    datacacheProxyConnector.getClientListDownloadStatus(credId, serviceName, grace)

  protected def logWaitPlan(business: Seq[Long], browserMs: Long): Unit =
    logger.info(s"""event="wait_plan" service="$serviceName" business_ms="${business.mkString(",")}" browser_ms=$browserMs""")

  private def logStatus(context: String, index: Option[Int], status: ClientListStatus): Unit =
    val indexString = index.map(i => s" index=$i").getOrElse("")
    logger.info(s"""event="status" service="$serviceName" context="$context"$indexString value="${status.toString}"""")

  // ------------------------------------------------------------
  // Shared polling logic (business + browser phases)
  // ------------------------------------------------------------

  private def processWithWaitPlan(
   credentialId: String,
   waitPlan: AsynchronousProcessWaitTime
  )(implicit hc: HeaderCarrier): Future[Unit] =
    val business = waitPlan.businessIntervalsMs
    logWaitPlan(business, waitPlan.browserIntervalMs)

    // BUSINESS PHASE
    if business.isEmpty then
      audit.clientListRetrievalFailed(credentialId, phase = "business", reason = Some("no-business-intervals"))
      clearWaitTime(credentialId)
      Future.failed(NoBusinessIntervalsException("No business intervals"))
    else
      def loop(index: Int): Future[Boolean] =
        if index >= business.length then
          Future.successful(false)
        else
          for
            _      <- sleep(business(index))
            status <- getStatus(credentialId)
            _       = logStatus("business", Some(index), status)
            result <- status match
              case Succeeded =>
                clearWaitTime(credentialId)
                Future.successful(true)

              case Failed =>
                audit.clientListRetrievalFailed(credentialId, phase = s"business#$index")
                clearWaitTime(credentialId)
                Future.failed(ClientListDownloadFailedException("Failed"))

              case InProgress =>
                loop(index + 1)

              case InitiateDownload =>
                if index < business.length - 1 then
                  loop(index + 1)
                else
                  audit.clientListRetrievalFailed(
                    credentialId,
                    phase  = s"business#$index",
                    reason = Some("initiate-on-final-business-interval")
                  )
                  clearWaitTime(credentialId)
                  Future.failed(SystemException("Initiate on final business interval"))
          yield result

      for
        businessSucceeded <- loop(0)
        outcome <-
          // BROWSER PHASE: only when we exhaust business intervals with last status = InProgress
          if businessSucceeded then
            Future.unit
          else
            for
              _      <- sleep(waitPlan.browserIntervalMs)
              status <- getStatus(credentialId)
              _       = logStatus("browser", None, status)
              finalResult <- status match
                case Succeeded =>
                  clearWaitTime(credentialId)
                  Future.unit

                case Failed =>
                  audit.clientListRetrievalFailed(credentialId, phase = "browser")
                  clearWaitTime(credentialId)
                  Future.failed(ClientListDownloadFailedException("Failed after browser interval"))

                case InitiateDownload =>
                  audit.clientListRetrievalFailed(credentialId, phase = "browser", reason = Some("initiate-after-browser"))
                  clearWaitTime(credentialId)
                  Future.failed(SystemException("Initiate after browser interval"))

                case InProgress =>
                  audit.clientListRetrievalInProgress(credentialId, phase = "browser")
                  clearWaitTime(credentialId)
                  Future.failed(ClientListDownloadInProgressException("Still in progress"))
            yield finalResult
      yield outcome

  // ------------------------------------------------------------
  // Entry point
  // ------------------------------------------------------------

  def process(credentialId: String)(implicit hc: HeaderCarrier): Future[ClientListStatus] =
    val underlying: Future[Unit] =
      getStatus(credentialId).flatMap {
        case Succeeded =>
          logStatus("initial", None, Succeeded)
          clearWaitTime(credentialId)
          Future.unit

        case Failed =>
          logStatus("initial", None, Failed)
          audit.clientListRetrievalFailed(credentialId, phase = "initial")
          clearWaitTime(credentialId)
          Future.failed(ClientListDownloadFailedException("Failed"))

        case InProgress =>
          logStatus("initial", None, InProgress)
          audit.clientListRetrievalInProgress(credentialId, phase = "initial")
          val waitPlan = getCachedWaitTime(credentialId).getOrElse(defaultWaitPlan)
          processWithWaitPlan(credentialId, waitPlan)

        case InitiateDownload =>
          logStatus("initial", None, InitiateDownload)

          for
            waitPlan <- clientExchangeProxyConnector.initiate(serviceName, credentialId)
            _         = cacheWaitTime(credentialId, waitPlan)
            outcome  <- processWithWaitPlan(credentialId, waitPlan)
          yield outcome
      }

    underlying
    .map(_ => Succeeded)
      .recover {
        case _: ClientListDownloadInProgressException => InProgress
        case _: ClientListDownloadFailedException     => Failed
        case _: SystemException                       => InitiateDownload
      }
}
