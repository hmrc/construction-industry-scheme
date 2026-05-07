/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.constructionindustryscheme.repositories.JourneyHandoffRepository

import javax.inject.*
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyHandoffController @Inject() (
  authorise: AuthAction,
  repo: JourneyHandoffRepository,
  cc: ControllerComponents
)(using ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def create(journeyType: String): Action[JsValue] =
    authorise.async(parse.json) { implicit request =>
      request.body.validate[JsObject] match {
        case JsSuccess(data, _) =>
          repo
            .create(request.credentialId, journeyType, data)
            .map { handoffId =>
              Created(Json.obj("id" -> handoffId))
            }
            .recover { case e =>
              logger.error(s"[JourneyHandOffController.create] error creating handoff data", e)
              InternalServerError(Json.obj("error" -> "Failed to create handoff data"))
            }

        case JsError(errors) =>
          logger.warn(s"[JourneyHandOffController.create] invalid JSON: $errors")
          Future.successful(BadRequest(Json.obj("error" -> "Invalid JSON")))
      }
    }

  def get(journeyType: String, id: String): Action[AnyContent] =
    authorise.async { implicit request =>
      repo
        .get(id, request.credentialId, journeyType)
        .map {
          case Some(handoff) => Ok(handoff.data)
          case None          => NotFound(Json.obj("error" -> "Handoff data not found"))
        }
        .recover { case e =>
          logger.error(s"[JourneyHandOffController.get] error retrieving handoff data", e)
          InternalServerError(Json.obj("error" -> "Failed to retrieve handoff data"))
        }
    }

  def delete(journeyType: String, id: String): Action[AnyContent] =
    authorise.async { implicit request =>
      repo
        .delete(id, request.credentialId, journeyType)
        .map { deleted =>
          if deleted then Ok else NotFound(Json.obj("error" -> "Handoff data not found"))
        }
        .recover { case e =>
          logger.error(s"[JourneyHandOffController.delete] error deleting handoff data", e)
          InternalServerError(Json.obj("error" -> "Failed to delete handoff data"))
        }
    }

}
