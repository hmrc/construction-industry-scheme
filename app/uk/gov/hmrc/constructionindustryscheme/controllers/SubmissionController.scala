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


import javax.inject.Inject
import play.api.mvc.*
import play.api.libs.json.*

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED as AcceptedStatus, DEPARTMENTAL_ERROR as DepartmentalErrorStatus, FATAL_ERROR as FatalErrorStatus, PENDING as PendingStatus, SUBMITTED as SubmittedStatus, SUBMITTED_NO_RECEIPT as SubmittedNoReceiptStatus}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisSubmissionRequest, CreateAndTrackSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.SubmissionService
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeBuilder

class SubmissionController @Inject()(
                                           authorise: AuthAction,
                                           submissionService: SubmissionService,
                                           cc: ControllerComponents
                                         )(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  implicit val reads: Reads[ChrisSubmissionRequest] = Json.reads[ChrisSubmissionRequest]

  def createAndTrackSubmission: Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body.validate[CreateAndTrackSubmissionRequest].fold(
        errs => Future.successful(BadRequest(JsError.toJson(errs))),
        csr =>
          submissionService
            .createAndTrackSubmission(csr)
            .map(id => Created(Json.obj("submissionId" -> id)))
            .recover { case ex =>
              logger.error("[create] formp-proxy create failed", ex)
              BadGateway(Json.obj("message" -> "create-submission-failed"))
            }
      )
    }

  def submitToChris(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body.validate[ChrisSubmissionRequest].fold(
        errs => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errs)))),
        csr => {
          logger.info(s"Submitting Nil Monthly Return to ChRIS for UTR=${csr.utr}")
          val correlationId = java.util.UUID.randomUUID().toString.replace("-", "").toUpperCase
          val payload = ChrisEnvelopeBuilder.buildPayload(csr, request, correlationId)

          submissionService.submitToChris(payload).map { res =>
            res.status match {
              case AcceptedStatus =>
                Accepted(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "ACCEPTED",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "correlationId" -> res.meta.correlationId,
                  "nextPollInSeconds" -> res.meta.responseEndPoint.pollIntervalSeconds,
                  "gatewayTimestamp" -> res.meta.gatewayTimestamp
                ))
              case PendingStatus =>
                Results.Accepted(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "PENDING",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "correlationId" -> res.meta.correlationId,
                  "nextPollInSeconds" -> res.meta.responseEndPoint.pollIntervalSeconds,
                  "gatewayTimestamp" -> res.meta.gatewayTimestamp
                ))
              case SubmittedStatus =>
                Results.Ok(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "SUBMITTED",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "correlationId" -> res.meta.correlationId,
                  "gatewayTimestamp" -> res.meta.gatewayTimestamp
                ))
              case SubmittedNoReceiptStatus =>
                Results.Ok(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "SUBMITTED_NO_RECEIPT",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "correlationId" -> res.meta.correlationId,
                  "gatewayTimestamp" -> res.meta.gatewayTimestamp
                ))
              case DepartmentalErrorStatus =>
                Results.Ok(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "DEPARTMENTAL_ERROR",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "error" -> res.meta.error.map(e =>
                    Json.obj("number" -> e.errorNumber, "type" -> e.errorType, "text" -> e.errorText)
                  ).getOrElse(Json.obj("text" -> "departmental error"))
                ))
              case FatalErrorStatus =>
                Results.Ok(Json.obj(
                  "submissionId" -> submissionId,
                  "status" -> "FATAL_ERROR",
                  "hmrcMarkGenerated" -> payload.irMark,
                  "error" -> res.meta.error.map(e =>
                    Json.obj("number" -> e.errorNumber, "type" -> e.errorType, "text" -> e.errorText)
                  ).getOrElse(Json.obj("text" -> "fatal"))
                ))
            }
          }.recover { case ex =>
            logger.error("[submitToChris] upstream failure", ex)
            Results.BadGateway(Json.obj(
              "submissionId" -> submissionId,
              "status" -> "FATAL_ERROR",
              "hmrcMarkGenerated" -> payload.irMark,
              "error" -> "upstream-failure"
            ))
          }
        }
      )
    }

  def updateSubmission(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit req =>
      req.body.validate[UpdateSubmissionRequest].fold(
        e => Future.successful(BadRequest(JsError.toJson(e))),
        upd => {
          submissionService.updateSubmission(upd).map { _ =>
            NoContent
          }.recover { case ex =>
            logger.error("[updateSubmission] formp-proxy update failed", ex)
            BadGateway(Json.obj("submissionId" -> submissionId, "message" -> "update-submission-failed"))
          }
        }
      )
    }

}
