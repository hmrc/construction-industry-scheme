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

import play.api.Logging

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.*
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, ClientListStatus, EmployerReference}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class DatacacheProxyConnector @Inject()(
                                         http: HttpClientV2,
                                         config: ServicesConfig
                                       )(implicit ec: ExecutionContext) extends HttpReadsInstances with Logging {

  private val base = config.baseUrl("rds-datacache-proxy") + "/rds-datacache-proxy"

  def getCisTaxpayer(employerReference: EmployerReference)(implicit hc: HeaderCarrier): Future[CisTaxpayer] =
    http
      .post(url"$base/cis-taxpayer")
      .withBody(Json.toJson(employerReference))
      .execute[CisTaxpayer]


  def getClientListDownloadStatus(
   credentialId: String,
   serviceName: String,
   gracePeriodSeconds: Int
  )(implicit hc: HeaderCarrier): Future[ClientListStatus] = {
    val endpoint = url"$base/client-list-status?credentialId=$credentialId&serviceName=$serviceName&gracePeriod=$gracePeriodSeconds"

    http.get(endpoint)
      .execute[JsValue]
      .flatMap { json =>
        (json \ "status").asOpt[String] match {
          case Some(s) =>
            Future.successful(mapProxyStatus(s))
          case None =>
            logger.error(s"[DatacacheProxyConnector] invalid payload (missing 'status'): ${json.toString}")
            Future.failed(
              UpstreamErrorResponse("rds-datacache-proxy invalid payload", 502, 502)
            )        }
      }
  }

  private def mapProxyStatus(s: String): ClientListStatus = s match {
    case "InitiateDownload" => InitiateDownload
    case "InProgress" => InProgress
    case "Succeeded" => Succeeded
    case "Failed" => Failed
    case other =>
      logger.error(s"[DatacacheProxyConnector] unknown status '$other' from rds-datacache-proxy")
      throw new RuntimeException(s"Unknown status '$other' from rds-datacache-proxy")
  }
}
