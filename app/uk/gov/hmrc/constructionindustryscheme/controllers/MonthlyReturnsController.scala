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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, NilMonthlyReturnRequest, MonthlyReturn}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject

class MonthlyReturnsController @Inject()(
                                         authorise: AuthAction,
                                         service: MonthlyReturnService,
                                         val cc: ControllerComponents
                                       )(implicit ec: ExecutionContext) extends BackendController(cc) with Logging {

  def getCisTaxpayer: Action[AnyContent] = authorise.async { implicit request =>
    val enrolmentsOpt: Option[EmployerReference] =
      for {
        enrol                <- request.enrolments.getEnrolment("HMRC-CIS-ORG")
        taxOfficeNumber      <- enrol.getIdentifier("TaxOfficeNumber")
        taxOfficeReference   <- enrol.getIdentifier("TaxOfficeReference")
      } yield EmployerReference(taxOfficeNumber.value, taxOfficeReference.value)

    enrolmentsOpt match {
      case Some(enrolmentReference) =>
        service.getCisTaxpayer(enrolmentReference)
          .map(tp => Ok(Json.toJson(tp)))
          .recover {
            case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND =>
              NotFound(Json.obj("message" -> "CIS taxpayer not found"))
            case u: UpstreamErrorResponse =>
              Status(u.statusCode)(Json.obj("message" -> u.message))
            case t: Throwable =>
              logger.error("[getCisTaxpayer] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
          }

      case None =>
        Future.successful(BadRequest(Json.obj("message" -> "Missing CIS enrolment identifiers")))
    }
  }

  def getAllMonthlyReturns(cisId: Option[String]): Action[AnyContent] = authorise.async { implicit request =>
    cisId match {
      case Some(id) if id.trim.nonEmpty =>
        service.getAllMonthlyReturnsByCisId(id)
          .map(res => Ok(Json.toJson(res)))
          .recover {
            case u: UpstreamErrorResponse =>
              Status(u.statusCode)(Json.obj("message" -> u.message))
            case t: Throwable =>
              logger.error("[getMonthlyReturns] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
          }

      case _ =>
        Future.successful(BadRequest(Json.obj("message" -> "Missing 'cisId'")))
    }
  }

  def createNil(): Action[JsValue] = authorise.async(parse.json) { implicit request =>
    request.body.validate[NilMonthlyReturnRequest].fold(
      _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid payload"))),
      payload =>
        service.createNilMonthlyReturn(payload)
          .map(monthlyReturn => Ok(Json.toJson(monthlyReturn)))
          .recover { case u: UpstreamErrorResponse => Status(u.statusCode)(Json.obj("message" -> u.message)) }
    )
  }
}


