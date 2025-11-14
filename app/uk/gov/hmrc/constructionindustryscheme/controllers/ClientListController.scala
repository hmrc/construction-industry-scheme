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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class ClientListController @Inject()(
  authorise: AuthAction,
  service: ClientListService,
  cc: ControllerComponents
)(using ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  def start: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier =
      HeaderCarrierConverter.fromRequest(request)

    request.credentialId match
      case Some(credId) =>
        service.process(credId).map { _ =>
          Ok(Json.obj("result" -> "succeeded"))
        }.recover {
          case _: ClientListDownloadInProgressException =>
            Ok(Json.obj("result" -> "in-progress"))
          case _: ClientListDownloadFailedException =>
            Ok(Json.obj("result" -> "failed"))
          case _: SystemException =>
            InternalServerError(Json.obj("result" -> "system-error"))
        }

      case None =>
        Future.successful(
          BadRequest(Json.obj("message" -> "Missing credentialId"))
        )
  }
}