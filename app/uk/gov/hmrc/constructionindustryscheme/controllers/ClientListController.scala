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

package uk.gov.hmrc.constructionindustryscheme.controllers

import play.api.Logging

import javax.inject.*
import play.api.mvc.*
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ClientListController @Inject()(
  authorise: AuthAction,
  service: ClientListService,
  cc: ControllerComponents
)(using ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  private def resultJson(status: ClientListStatus): Result =
    status match {
      case Succeeded => Ok(Json.obj("result" -> "succeeded"))
      case InProgress => Ok(Json.obj("result" -> "in-progress"))
      case Failed => Ok(Json.obj("result" -> "failed"))
      case InitiateDownload => Ok(Json.obj("result" -> "initiate-download"))
    }

  def start: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    request.credentialId match
      case Some(credId) =>
        service
          .process(credId)
          .map(resultJson)
          .recover {
            case _: NoBusinessIntervalsException =>
              InternalServerError(Json.obj("result" -> "system-error"))
          }

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Missing credentialId"))
        )
  }

  def status: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    request.credentialId match
      case Some(credId) =>
        service
          .getStatus(credId)
          .map(resultJson)

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Missing credentialId"))
        )
  }

  def getAllClients: Action[AnyContent] = authorise.async { implicit request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val irAgentId = request.enrolments.getEnrolment("IR-PAYE-AGENT").flatMap(_.getIdentifier("IRAgentReference"))
    val credentialId = request.credentialId

    (irAgentId, credentialId) match {
      case (Some(agentId), Some(credId)) =>
        service.getClientList(agentId.value, credId).map((result: ClientSearchResult) => Ok(Json.toJson(result)))
      case (maybeAgentId, maybeCredId) =>
        logger.info(s"[ClientListController.getAllClients] authenticated request with missing agent enrollments - agentId: $maybeAgentId, credId: $maybeCredId")
        Future.successful(Forbidden(Json.obj("error" -> "credentialId and/or irAgentId are missing from session")))
    }
  }
}