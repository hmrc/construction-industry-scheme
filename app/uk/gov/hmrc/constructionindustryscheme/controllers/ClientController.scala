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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ClientController @Inject()(
  authorise: AuthAction,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getClientListDownloadStatus:Action[AnyContent] = authorise.async { implicit request =>

    val ref = request.enrolments
      .getEnrolment("IR-PAYE-AGENT")
      .flatMap(_.getIdentifier("IRAgentReference"))
      .map(_.value)

    ref match {
      case Some("Failed")           => Future.successful(Ok(Json.obj("status" -> "Failed")))
      case Some("InitiateDownload") => Future.successful(Ok(Json.obj("status" -> "InitiateDownload")))
      case Some("InProgress")       => Future.successful(Ok(Json.obj("status" -> "InProgress")))
      case _                        => Future.successful(Ok(Json.obj("status" -> "Success")))
    }
  }

}
