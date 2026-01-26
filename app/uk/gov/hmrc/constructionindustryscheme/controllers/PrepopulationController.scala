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
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.services.PrepopulationService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import scala.util.control.NonFatal
import scala.concurrent.ExecutionContext
import javax.inject.Inject

class PrepopulationController @Inject() (
  authorise: AuthAction,
  service: PrepopulationService,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def prepopulateContractorKnownFacts(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    instanceId: String
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val employerRef = EmployerReference(
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference
      )

      service
        .prepopulateContractorKnownFacts(instanceId, employerRef)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND =>
            PreconditionFailed(Json.obj("message" -> "CIS taxpayer not found"))
          case u: UpstreamErrorResponse                              =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)                                           =>
            logger.error("[prepopulateContractorKnownFacts] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def prepopulateContractorAndSubcontractors(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    instanceId: String
  ): Action[AnyContent] =
    authorise.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val employerRef = EmployerReference(
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference
      )

      service
        .prepopulateContractorAndSubcontractors(instanceId, employerRef)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[prepopulateContractorAndSubcontractors] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getContractorScheme(instanceId: String): Action[AnyContent] =
    authorise.async { implicit request =>
      implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      service
        .getContractorScheme(instanceId)
        .map {
          case Some(scheme) => Ok(Json.toJson(scheme))
          case None         => NotFound(Json.obj("message" -> "Scheme not found"))
        }
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[getContractorScheme] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
}
