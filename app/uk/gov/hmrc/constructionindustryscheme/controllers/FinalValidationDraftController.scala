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
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationDraft
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.services.{FinalValidationDraftNotReadyException, FinalValidationDraftService}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class FinalValidationDraftController @Inject() (
  authorise: AuthAction,
  finalValidationDraftService: FinalValidationDraftService,
  cc: ControllerComponents
)(using ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {

  def create(): Action[CreateFinalValidationDraftRequest] =
    authorise.async(parse.json[CreateFinalValidationDraftRequest]) { implicit request =>
      finalValidationDraftService
        .create(request.credentialId, request.body)
        .map { draftId =>
          Created(
            Json.obj("draftId" -> draftId)
          )
        }
        .recover {
          case NonFatal(error) =>
            logger.error("[FinalValidationDraftController.create] failed to create Final Validation draft", error)
            InternalServerError(Json.obj("error" -> "Failed to create Final Validation draft"))
        }
    }

  def get(instanceId: String, draftId: String): Action[AnyContent] =
    authorise.async { implicit request =>

      finalValidationDraftService
        .get(draftId, request.credentialId, instanceId)
        .map { draft =>
          Ok(
            Json.toJson(draft)
          )
        }
        .recover {
          case _: NoSuchElementException =>
            NotFound(
              Json.obj("error" -> "Final Validation draft not found")
            )

          case NonFatal(error) =>
            logger.error(s"[FinalValidationDraftController.get] failed to retrieve draft $draftId", error)
            InternalServerError(
              Json.obj("error" -> "Failed to retrieve Final Validation draft")
            )
        }
    }

  def updateCorrection(
    instanceId: String,
    draftId: String
  ): Action[FinalValidationCorrectionRequest] =
    authorise.async(parse.json[FinalValidationCorrectionRequest]) { implicit request =>

      finalValidationDraftService
        .updateCorrection(
          draftId,
          request.credentialId,
          instanceId,
          request.body
        )
        .map { updated =>
          Ok(
            Json.toJson(updated)
          )
        }
        .recover {
          case _: NoSuchElementException =>
            NotFound(
              Json.obj("error" -> "Final Validation draft not found")
            )

          case NonFatal(error) =>
            logger.error(
              s"[FinalValidationDraftController.updateCorrection] failed to update draft $draftId",
              error
            )
            InternalServerError(
              Json.obj("error" -> "Failed to update Final Validation correction")
            )
        }
    }

  def updateReadiness(
    instanceId: String,
    draftId: String
  ): Action[UpdateFinalValidationReadinessRequest] =
    authorise.async(parse.json[UpdateFinalValidationReadinessRequest]) { implicit request =>

      finalValidationDraftService
        .updateReadiness(
          draftId,
          request.credentialId,
          instanceId,
          request.body
        )
        .map { updated =>
          Ok(
            Json.toJson(updated)
          )
        }
        .recover {
          case _: NoSuchElementException =>
            NotFound(
              Json.obj("error" -> "Final Validation draft not found")
            )

          case NonFatal(error) =>
            logger.error(
              s"[FinalValidationDraftController.updateReadiness] failed to update draft $draftId",
              error
            )
            InternalServerError(
              Json.obj("error" -> "Failed to update Final Validation readiness")
            )
        }
    }

  def commit(
    instanceId: String,
    draftId: String
  ): Action[AnyContent] =
    authorise.async { implicit request =>

      finalValidationDraftService
        .commit(
          draftId,
          request.credentialId,
          instanceId
        )
        .map { _ =>
          NoContent
        }
        .recover {
          case _: NoSuchElementException =>
            NotFound(
              Json.obj("error" -> "Final Validation draft not found")
            )

          case _: FinalValidationDraftNotReadyException =>
            Conflict(
              Json.obj("error" -> "Final Validation draft is not ready to commit")
            )

          case NonFatal(error) =>
            logger.error(
              s"[FinalValidationDraftController.commit] failed to commit draft $draftId",
              error
            )
            InternalServerError(
              Json.obj("error" -> "Failed to commit Final Validation draft")
            )
        }
    }

}
