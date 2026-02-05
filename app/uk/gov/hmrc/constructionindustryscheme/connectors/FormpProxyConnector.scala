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

import play.api.http.Status.{NOT_FOUND, NO_CONTENT}

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class FormpProxyConnector @Inject() (
  http: HttpClientV2,
  config: ServicesConfig
)(implicit ec: ExecutionContext)
    extends HttpReadsInstances {

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

  def createNilMonthlyReturn(
    req: NilMonthlyReturnRequest
  )(implicit hc: HeaderCarrier): Future[CreateNilMonthlyReturnResponse] =
    http
      .post(url"$base/monthly-return/nil/create")
      .withBody(
        Json.obj(
          "instanceId"             -> req.instanceId,
          "taxYear"                -> req.taxYear,
          "taxMonth"               -> req.taxMonth,
          "decInformationCorrect"  -> req.decInformationCorrect,
          "decNilReturnNoPayments" -> req.decNilReturnNoPayments
        )
      )
      .execute[CreateNilMonthlyReturnResponse]

  def createMonthlyReturn(req: MonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$base/cis/monthly-return/standard/create")
      .withBody(Json.toJson(req))
      .execute[HttpResponse]
      .map { response =>
        if (response.status == 201) ()
        else throw UpstreamErrorResponse(response.body, response.status, response.status)
      }

  def getSchemeEmail(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    http
      .post(url"$base/scheme/email")
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

  def updateSchemeVersion(req: UpdateSchemeVersionRequest)(implicit hc: HeaderCarrier): Future[Int] =
    http
      .post(url"$base/scheme/version-update")
      .withBody(Json.toJson(req))
      .execute[JsValue]
      .map(json => (json \ "version").as[Int])

  def createSubcontractor(req: CreateSubcontractorRequest)(implicit hc: HeaderCarrier): Future[Int] =
    http
      .post(url"$base/cis/subcontractor/create")
      .withBody(Json.toJson(req))
      .execute[JsValue]
      .map(json => (json \ "subbieResourceRef").as[Int])

  def applyPrepopulation(req: ApplyPrepopulationRequest)(implicit hc: HeaderCarrier): Future[Int] =
    http
      .post(url"$base/scheme/prepopulate")
      .withBody(Json.toJson(req))
      .execute[JsValue]
      .map(json => (json \ "version").as[Int])

  def getUnsubmittedMonthlyReturns(instanceId: String)(implicit hc: HeaderCarrier): Future[UnsubmittedMonthlyReturns] =
    http
      .post(url"$base/cis/retrieve-unsubmitted-monthly-returns")
      .withBody(Json.obj("instanceId" -> instanceId))
      .execute[UnsubmittedMonthlyReturns]

  def updateSubcontractor(request: UpdateSubcontractorRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$base/cis/subcontractor/update")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == NO_CONTENT) {
          Future.successful(())
        } else {
          Future.failed(
            new RuntimeException(
              s"Update subcontractor failed, returned ${response.status}"
            )
          )
        }
      }

  def getMonthlyReturnForEdit(
    request: GetMonthlyReturnForEditRequest
  )(implicit hc: HeaderCarrier): Future[GetMonthlyReturnForEditResponse] =
    http
      .post(url"$base/cis/monthly-return-edit")
      .withBody(Json.toJson(request))
      .execute[GetMonthlyReturnForEditResponse]

  def getSubcontractorUTRs(cisId: String)(implicit hc: HeaderCarrier): Future[Seq[String]] = {

    implicit val readsSubcontractorUTRsOnly: Reads[Seq[String]] =
      (JsPath \ "subcontractors")
        .read(
          Reads.seq((JsPath \ "utr").readNullable[String])
        )
        .map(_.flatten)

    http
      .get(url"$base/cis/subcontractors/$cisId")
      .execute[Seq[String]]
  }

  def syncMonthlyReturnItems(request: SyncMonthlyReturnItemsRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .post(url"$base/cis/monthly-return-item/sync")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        if (response.status == 204) Future.unit
        else Future.failed(UpstreamErrorResponse(response.body, response.status, response.status))
      }

}
