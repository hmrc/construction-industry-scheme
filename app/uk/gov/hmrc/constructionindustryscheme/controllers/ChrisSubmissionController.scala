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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.ChrisService

class ChrisSubmissionController @Inject()(
                                           authorise: AuthAction,
                                           chrisService: ChrisService,
                                           cc: ControllerComponents
                                         )(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  implicit val reads: Reads[ChrisSubmissionRequest] = Json.reads[ChrisSubmissionRequest]

  def submitNilMonthlyReturn: Action[JsValue] =
    authorise(parse.json).async { implicit request: AuthenticatedRequest[JsValue] =>
      request.body.validate[ChrisSubmissionRequest].fold(
        errs => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errs)))),
        chrisReq => {
          logger.info(s"Submitting Nil Monthly Return to ChRIS for UTR=${chrisReq.utr}")
          chrisService.submitNilMonthlyReturn(chrisReq, request).map { resp =>
            Ok(Json.obj(
              "success"  -> true,
              "status"   -> resp.status,
              "body"     -> resp.body
            ))
          }.recover { case ex =>
            logger.error("ChRIS submission failed", ex)
            InternalServerError(Json.obj(
              "success"   -> false,
              "message"   -> ex.getMessage
            ))
          }
        }
      )
    }
}
