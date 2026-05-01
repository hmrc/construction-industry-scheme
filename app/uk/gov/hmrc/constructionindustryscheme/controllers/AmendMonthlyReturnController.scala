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
import play.api.libs.json.Json
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.services.AmendMonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

class AmendMonthlyReturnController @Inject() (
  authorise: AuthAction,
  service: AmendMonthlyReturnService,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with Logging {
  
  def createAmendedMonthlyReturn: Action[CreateAmendedMonthlyReturnRequest] =
    authorise.async(parse.json[CreateAmendedMonthlyReturnRequest]) { implicit request =>
      service
        .createAmendedMonthlyReturn(request.body)
        .map(_ => Created)
        .recover {
          case u: UpstreamErrorResponse =>
            logger.error("[createAmendedMonthlyReturn] failed", u)
            Status(u.statusCode)(Json.obj("message" -> u.message))
          
          case NonFatal(t) =>
            logger.error("[createAmendedMonthlyReturn] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
