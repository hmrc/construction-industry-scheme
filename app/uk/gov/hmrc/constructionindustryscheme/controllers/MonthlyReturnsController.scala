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

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.ExecutionContext

class MonthlyReturnsController @Inject()(
                                         authorise: AuthAction,
                                         service: MonthlyReturnService,
                                         val cc: ControllerComponents
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) {


  def retrieveMonthlyReturns(firstRecordNumber: Option[Int], maxRecords: Option[Int]): Action[AnyContent] =
    authorise.async {
      implicit request =>
        service.retrieveMonthlyReturns(maxRecords.getOrElse(0)).map { response =>
          Ok(Json.toJson(response))
        }
    }

}


