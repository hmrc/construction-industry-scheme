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

import play.api.http.Status.NOT_FOUND
import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FormpProxyConnector @Inject()(
                                     http: HttpClientV2,
                                     config: ServicesConfig
                                   )(implicit ec: ExecutionContext) extends HttpReadsInstances {

  private val base = config.baseUrl("formp-proxy") + "/formp-proxy"

  def getMonthlyReturns(instanceId: String)(implicit hc: HeaderCarrier): Future[UserMonthlyReturns] =
    http
      .post(url"$base/monthly-returns")
      .withBody(Json.obj("instanceId" -> instanceId))
      .execute[UserMonthlyReturns]

  def createSubmission(request: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[String] =
    http
      .post(url"$base/submissions/create")
      .withBody(Json.toJson(request))
      .execute[CreateSubmissionResponse]
      .map(_.submissionId)

  def updateSubmission(req: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$base/submissions/update")
      .withBody(Json.toJson(req))
      .execute[HttpResponse]
      .flatMap { resp =>
        if (resp.status / 100 == 2) Future.unit
        else Future.failed(UpstreamErrorResponse(resp.body, resp.status, resp.status))
      }

  def createNilMonthlyReturn(req: NilMonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[CreateNilMonthlyReturnResponse] =
    http.post(url"$base/monthly-return/nil/create")
      .withBody(
        Json.obj(
          "instanceId" -> req.instanceId,
          "taxYear" -> req.taxYear,
          "taxMonth" -> req.taxMonth,
          "decInformationCorrect" -> req.decInformationCorrect,
          "decNilReturnNoPayments" -> req.decNilReturnNoPayments
        )
      )
      .execute[CreateNilMonthlyReturnResponse]

  def getSchemeEmail(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    http.post(url"$base/scheme/email")
      .withBody(Json.obj("instanceId" -> instanceId))
      .execute[Option[String]]

  def getContractorScheme(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[ContractorScheme]] =
    http
      .get(url"$base/scheme/$instanceId")
      .execute[ContractorScheme]
      .map(Some(_))
      .recoverWith {
        case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND =>
          Future.successful(None)
      }

  def createContractorScheme(req: CreateContractorSchemeParams)(implicit hc: HeaderCarrier): Future[Int] =
    http
      .post(url"$base/scheme")
      .withBody(Json.toJson(req))
      .execute[CreateSchemeResponse]
      .map(_.schemeId)

  def updateContractorScheme(req: UpdateContractorSchemeParams)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$base/scheme/update")
      .withBody(Json.toJson(req))
      .execute[HttpResponse]
      .flatMap { resp =>
        if (resp.status / 100 == 2) Future.unit
        else Future.failed(UpstreamErrorResponse(resp.body, resp.status, resp.status))
      }
}
