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
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorController @Inject() (
  authorise: AuthAction,
  subcontractorService: SubcontractorService,
  cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def createAndUpdateSubcontractor(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[CreateAndUpdateSubcontractorRequest]
        .fold(
          errs => Future.successful(BadRequest(JsError.toJson(errs))),
          request =>
            subcontractorService
              .createAndUpdateSubcontractor(request)
              .map(resp => NoContent)
              .recover { case ex =>
                logger.error("[createAndUpdateSubcontractor] formp-proxy create failed", ex)
                BadGateway(Json.obj("message" -> "create-and-update-subcontractor-failed"))
              }
        )
    }
}
