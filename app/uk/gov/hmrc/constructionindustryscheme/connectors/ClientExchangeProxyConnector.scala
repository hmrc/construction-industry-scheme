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

package uk.gov.hmrc.constructionindustryscheme.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.constructionindustryscheme.models.AsynchronousProcessWaitTime
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.WaitTimeXmlMapper
import play.api.Logging

@Singleton
class ClientExchangeProxyConnector @Inject()(
                                              httpClient: HttpClientV2,
                                              servicesConfig: ServicesConfig
                                            )(implicit ec: ExecutionContext) extends Logging {

  private val base = servicesConfig.baseUrl("client-exchange-proxy")
  private val pathPrefix = servicesConfig.getConfString(
    "client-exchange-proxy.pathPrefix", "client-exchange-proxy"
  )
  logger.info(s"[ClientExchangeProxyConnector] resolved base=$base pathPrefix=$pathPrefix")

  def initiate(service: String, credentialId: String)
              (implicit hc: HeaderCarrier): Future[AsynchronousProcessWaitTime] = {
    val endpoint = url"$base/$pathPrefix/$service/$credentialId/agent/clientlist"
    httpClient
      .get(endpoint)
      .execute[HttpResponse]
      .map { response =>
        if (is2xx(response.status)) {
          WaitTimeXmlMapper.parse(response.body).fold(
            err => {
              logger.error(s"[ClientExchangeProxyConnector] parse error: $err; body: ${response.body}")
              throw UpstreamErrorResponse(s"client-exchange-proxy parse error: $err", 502, 502)
            },
            ok  => ok
          )
        } else {
          logger.error(s"[ClientExchangeProxyConnector] non-2xx ${response.status}; body: ${response.body}")
          throw UpstreamErrorResponse(s"client-exchange-proxy HTTP ${response.status}", response.status, response.status)
        }
      }
  }

  private def is2xx(s: Int): Boolean = s >= 200 && s < 300
}