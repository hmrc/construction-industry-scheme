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
import play.api.libs.json.*
import play.api.mvc.*
import uk.gov.hmrc.constructionindustryscheme.actions.{AgentAction, AuthAction}
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.*
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

import javax.inject.*
import scala.concurrent.ExecutionContext

@Singleton
class ClientListController @Inject() (
  authorise: AuthAction,
  isAgent: AgentAction,
  service: ClientListService,
  cc: ControllerComponents
)(using ec: ExecutionContext)
    extends BackendController(cc)
    with Logging:

  def start: Action[AnyContent] = (authorise andThen isAgent).async { implicit request =>
    service
      .process(request.credentialId, request.agentId)
      .map(status => Ok(Json.obj("result" -> status.asString)))
      .recover { case _: NoBusinessIntervalsException =>
        InternalServerError(Json.obj("result" -> "system-error"))
      }
  }

  def status: Action[AnyContent] = (authorise andThen isAgent).async { implicit request =>
    service
      .getStatus(request.credentialId)
      .map(status => Ok(Json.obj("result" -> status.asString)))
  }

  def getAllClients: Action[AnyContent] = (authorise andThen isAgent).async { implicit request =>
    service
      .getClientList(request.agentId, request.credentialId)
      .map(result => Ok(Json.toJson(result)))
  }

  def hasClient(taxOfficeNumber: String, taxOfficeReference: String): Action[AnyContent] =
    (authorise andThen isAgent).async { implicit request =>
      service
        .hasClient(taxOfficeNumber, taxOfficeReference, request.agentId, request.credentialId)
        .map(hasClient => Ok(Json.obj("hasClient" -> hasClient)))
        .recover { case e: Exception =>
          logger.error(s"[ClientListController.checkClientExists] error checking client: ${e.getMessage}", e)
          InternalServerError(Json.obj("error" -> "Failed to check client"))
        }
    }
