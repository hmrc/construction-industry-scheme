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
import uk.gov.hmrc.constructionindustryscheme.models.requests.{DeleteMonthlyReturnItemRequest, GetMonthlyReturnForEditRequest, MonthlyReturnRequest, SelectedSubcontractorsRequest}
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, NilMonthlyReturnRequest}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.ClientListService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.Inject
import scala.util.control.NonFatal

class MonthlyReturnsController @Inject() (
  authorise: AuthAction,
  service: MonthlyReturnService,
  clientListService: ClientListService,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getCisClientTaxpayer(taxOfficeNumber: String, taxOfficeReference: String): Action[AnyContent] = authorise.async {
    implicit request =>
      given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

      val agentIdOpt = request.enrolments.getEnrolment("IR-PAYE-AGENT").flatMap(_.getIdentifier("IRAgentReference"))

      (request.credentialId, agentIdOpt) match {
        case (Some(credId), Some(agentIdEnrolment)) =>
          clientListService
            .hasClient(taxOfficeNumber, taxOfficeReference, agentIdEnrolment.value, credId)
            .flatMap { hasClient =>
              if (hasClient) {
                service
                  .getCisTaxpayer(EmployerReference(taxOfficeNumber, taxOfficeReference))
                  .map(tp => Ok(Json.toJson(tp)))
                  .recover {
                    case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND =>
                      NotFound(Json.obj("message" -> "CIS taxpayer not found"))
                    case u: UpstreamErrorResponse                              =>
                      Status(u.statusCode)(Json.obj("message" -> u.message))
                    case t: Throwable                                          =>
                      logger.error("[getCisClientTaxpayer] failed", t)
                      InternalServerError(Json.obj("message" -> "Unexpected error"))
                  }
              } else {
                Future.successful(Forbidden(Json.obj("error" -> "Client not found")))
              }
            }
            .recover { case e: Exception =>
              logger.error(s"[getCisClientTaxpayer] error checking client: ${e.getMessage}", e)
              InternalServerError(Json.obj("error" -> "Failed to check client"))
            }

        case (None, _) =>
          Future.successful(
            Forbidden(Json.obj("error" -> "credentialId is missing from session"))
          )

        case (_, None) =>
          Future.successful(
            Forbidden(Json.obj("error" -> "IR-PAYE-AGENT enrolment with IRAgentReference is missing"))
          )
      }
  }

  def getCisTaxpayer: Action[AnyContent] = authorise.async { implicit request =>
    val enrolmentsOpt: Option[EmployerReference] =
      for {
        enrol              <- request.enrolments.getEnrolment("HMRC-CIS-ORG")
        taxOfficeNumber    <- enrol.getIdentifier("TaxOfficeNumber")
        taxOfficeReference <- enrol.getIdentifier("TaxOfficeReference")
      } yield EmployerReference(taxOfficeNumber.value, taxOfficeReference.value)

    enrolmentsOpt match {
      case Some(enrolmentReference) =>
        service
          .getCisTaxpayer(enrolmentReference)
          .map(tp => Ok(Json.toJson(tp)))
          .recover {
            case u: UpstreamErrorResponse if u.statusCode == NOT_FOUND =>
              NotFound(Json.obj("message" -> "CIS taxpayer not found"))
            case u: UpstreamErrorResponse                              =>
              Status(u.statusCode)(Json.obj("message" -> u.message))
            case t: Throwable                                          =>
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
        service
          .getAllMonthlyReturnsByCisId(id)
          .map(res => Ok(Json.toJson(res)))
          .recover {
            case u: UpstreamErrorResponse =>
              Status(u.statusCode)(Json.obj("message" -> u.message))
            case t: Throwable             =>
              logger.error("[getMonthlyReturns] failed", t)
              InternalServerError(Json.obj("message" -> "Unexpected error"))
          }

      case _ =>
        Future.successful(BadRequest(Json.obj("message" -> "Missing 'cisId'")))
    }
  }

  def getUnsubmittedMonthlyReturns(cisId: String): Action[AnyContent] = authorise.async { implicit request =>
    val id = cisId.trim
    if (id.isEmpty) {
      Future.successful(BadRequest(Json.obj("message" -> "Missing 'cisId'")))
    } else {
      service
        .getUnsubmittedMonthlyReturns(id)
        .map(res => Ok(Json.toJson(res)))
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[getUnsubmittedMonthlyReturns] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }
  }

  def createNil(): Action[JsValue] = authorise.async(parse.json) { implicit request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    request.body
      .validate[NilMonthlyReturnRequest]
      .fold(
        _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid payload"))),
        payload =>
          service
            .createNilMonthlyReturn(payload)
            .map(monthlyReturn => Created(Json.toJson(monthlyReturn)))
            .recover { case u: UpstreamErrorResponse => Status(u.statusCode)(Json.obj("message" -> u.message)) }
      )
  }

  def updateNil(): Action[JsValue] = authorise.async(parse.json) { implicit request =>
    given HeaderCarrier = HeaderCarrierConverter.fromRequest(request)
    request.body
      .validate[NilMonthlyReturnRequest]
      .fold(
        _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid payload"))),
        payload =>
          service
            .updateNilMonthlyReturn(payload)
            .map(_ => NoContent)
            .recover { case u: UpstreamErrorResponse => Status(u.statusCode)(Json.obj("message" -> u.message)) }
      )
  }

  def createMonthlyReturn: Action[MonthlyReturnRequest] =
    authorise.async(parse.json[MonthlyReturnRequest]) { implicit request =>
      service
        .createMonthlyReturn(request.body)
        .map(_ => Created)
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[createMonthlyReturn] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def getSchemeEmail(instanceId: String): Action[AnyContent] = authorise.async { implicit request =>
    service
      .getSchemeEmail(instanceId)
      .map(email => Ok(Json.obj("email" -> email)))
      .recover {
        case u: UpstreamErrorResponse =>
          Status(u.statusCode)(Json.obj("message" -> u.message))
        case t: Throwable             =>
          logger.error("[getSchemeEmail] failed", t)
          InternalServerError(Json.obj("message" -> "Unexpected error"))
      }
  }

  def getMonthlyReturnForEdit: Action[GetMonthlyReturnForEditRequest] =
    authorise.async(parse.json[GetMonthlyReturnForEditRequest]) { implicit request =>
      service
        .getMonthlyReturnForEdit(request.body)
        .map(payload => Ok(Json.toJson(payload)))
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[getMonthlyReturnForEdit] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def syncSelectedSubcontractors: Action[SelectedSubcontractorsRequest] =
    authorise.async(parse.json[SelectedSubcontractorsRequest]) { implicit request =>
      service
        .syncMonthlyReturnItems(request.body)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse =>
            logger.error("[syncSelectedSubcontractors] formp-proxy sync failed", u)
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[syncSelectedSubcontractors] formp-proxy sync failed", t)
            BadGateway(Json.obj("message" -> "sync-selected-subcontractors-failed"))
        }
    }

  def deleteMonthlyReturnItem: Action[DeleteMonthlyReturnItemRequest] =
    authorise.async(parse.json[DeleteMonthlyReturnItemRequest]) { implicit request =>
      service
        .deleteMonthlyReturnItem(request.body)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse =>
            logger.error("[deleteMonthlyReturnItem] formp-proxy delete failed", u)
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[deleteMonthlyReturnItem] formp-proxy delete failed", t)
            BadGateway(Json.obj("message" -> "delete-monthly-return-item-failed"))
        }
    }

}
