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
import play.api.http.Status.NOT_FOUND

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.*
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, ClientListStatus, EmployerReference, PrePopContractorBody, PrePopContractorResponse, PrePopSubcontractor, PrePopSubcontractorsResponse, PrepopKnownFacts}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

@Singleton
class DatacacheProxyConnector @Inject() (
  http: HttpClientV2,
  config: ServicesConfig
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances
    with Logging {

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
    val endpoint =
      url"$base/cis/client-list-status?credentialId=$credentialId&serviceName=$serviceName&gracePeriod=$gracePeriodSeconds"

    http
      .get(endpoint)
      .execute[JsValue]
      .flatMap { json =>
        (json \ "status").asOpt[String] match {
          case Some(s) =>
            Future.successful(mapProxyStatus(s))
          case None    =>
            logger.error(s"[DatacacheProxyConnector] invalid payload (missing 'status'): ${json.toString}")
            Future.failed(
              UpstreamErrorResponse("rds-datacache-proxy invalid payload", 502, 502)
            )
        }
      }
  }

  def hasClient(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    agentId: String,
    credentialId: String
  )(implicit hc: HeaderCarrier): Future[Boolean] = {
    val endpoint =
      url"$base/cis/has-client?credentialId=$credentialId&irAgentId=$agentId&taxOfficeNumber=$taxOfficeNumber&taxOfficeReference=$taxOfficeReference"

    http
      .get(endpoint)
      .execute[JsValue]
      .flatMap { json =>
        (json \ "hasClient").validate[Boolean] match {
          case JsSuccess(hasClient, _) =>
            Future.successful(hasClient)
          case JsError(errors)         =>
            logger.error(s"[DatacacheProxyConnector] invalid payload (missing 'hasClient'): ${json.toString}")
            Future.failed(
              UpstreamErrorResponse("rds-datacache-proxy invalid payload", 502, 502)
            )
        }
      }
  }

  private def mapProxyStatus(s: String): ClientListStatus = s match {
    case "InitiateDownload" => InitiateDownload
    case "InProgress"       => InProgress
    case "Succeeded"        => Succeeded
    case "Failed"           => Failed
    case other              =>
      logger.error(s"[DatacacheProxyConnector] unknown status '$other' from rds-datacache-proxy")
      throw new RuntimeException(s"Unknown status '$other' from rds-datacache-proxy")
  }

  def getClientList(irAgentId: String, credentialId: String)(using HeaderCarrier): Future[ClientSearchResult] = {
    val endpoint = url"$base/cis/client-list?&irAgentId=$irAgentId&credentialId=$credentialId"

    http
      .get(endpoint)
      .execute[ClientSearchResult]
  }

  def getSchemePrepopByKnownFacts(
    knownFacts: PrepopKnownFacts
  )(implicit hc: HeaderCarrier): Future[Option[PrePopContractorBody]] =
    http
      .post(url"$base/cis/prepop-contractor")
      .withBody(Json.toJson(knownFacts))
      .execute[PrePopContractorResponse]
      .map(resp => Some(resp.prePopContractor))
      .recoverWith {
        case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND => Future.successful(None)
      }

  def getSubcontractorsPrepopByKnownFacts(
    knownFacts: PrepopKnownFacts
  )(implicit hc: HeaderCarrier): Future[Seq[PrePopSubcontractor]] =
    http
      .post(url"$base/cis/prepop-subcontractor")
      .withBody(Json.toJson(knownFacts))
      .execute[PrePopSubcontractorsResponse]
      .map(_.prePopSubcontractors.subcontractors)
      .recoverWith {
        case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND => Future.successful(Seq.empty)
      }

}
