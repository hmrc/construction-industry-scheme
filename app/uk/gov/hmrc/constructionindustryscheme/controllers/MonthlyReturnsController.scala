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
import uk.gov.hmrc.constructionindustryscheme.actions.{AgentAction, AuthAction}
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, NilMonthlyReturnRequest}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.ClientListService
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class MonthlyReturnsController @Inject() (
  authorise: AuthAction,
  isAgent: AgentAction,
  service: MonthlyReturnService,
  clientListService: ClientListService,
  val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  def getCisClientTaxpayer(taxOfficeNumber: String, taxOfficeReference: String): Action[AnyContent] =
    (authorise andThen isAgent).async { implicit request =>
      clientListService
        .hasClient(taxOfficeNumber, taxOfficeReference, request.agentId, request.credentialId)
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
          .getAllMonthlyReturnsByCisId(id.trim)
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

  def getSubmittedMonthlyReturns(cisId: String): Action[AnyContent] = authorise.async { implicit request =>
    val id = cisId.trim
    if (id.isEmpty) {
      Future.successful(BadRequest(Json.obj("message" -> "Missing 'cisId'")))
    } else {
      service
        .getSubmittedMonthlyReturns(id)
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

  def updateMonthlyReturn(): Action[JsValue] = authorise.async(parse.json) { implicit request =>
    request.body
      .validate[UpdateMonthlyReturnRequest]
      .fold(
        _ => Future.successful(BadRequest(Json.obj("message" -> "Invalid payload"))),
        payload =>
          service
            .updateMonthlyReturn(payload)
            .map(_ => NoContent)
            .recover {
              case u: UpstreamErrorResponse =>
                Status(u.statusCode)(Json.obj("message" -> u.message))
              case NonFatal(t)              =>
                logger.error("[updateMonthlyReturn] failed", t)
                InternalServerError(Json.obj("message" -> "Unexpected error"))
            }
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

  // TODO no check that the email belongs to the requesting user?
  def getSchemeEmail(instanceId: String): Action[AnyContent] = authorise.async { implicit request =>
    service
      .getSchemeEmail(instanceId)
      .map(email => Ok(Json.obj("email" -> email)))
      .recover {
        case u: UpstreamErrorResponse =>
          Status(u.statusCode)(Json.obj("message" -> u.message))
        case NonFatal(t)              =>
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

  def deleteMonthlyReturnItem(): Action[DeleteMonthlyReturnItemRequest] =
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

  def updateMonthlyReturnItem(): Action[UpdateMonthlyReturnItemRequest] =
    authorise.async(parse.json[UpdateMonthlyReturnItemRequest]) { implicit request =>
      service
        .updateMonthlyReturnItem(request.body)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[updateMonthlyReturnItem] failed", t)
            InternalServerError(Json.obj("message" -> "Unexpected error"))
        }
    }

  def deleteUnsubmittedMonthlyReturn: Action[DeleteUnsubmittedMonthlyReturnRequest] =
    authorise.async(parse.json[DeleteUnsubmittedMonthlyReturnRequest]) { implicit request =>
      service
        .deleteUnsubmittedMonthlyReturn(request.body)
        .map(_ => NoContent)
        .recover {
          case u: UpstreamErrorResponse =>
            logger.error("[deleteUnsubmittedMonthlyReturn] formp-proxy unsubmitted monthly return delete failed", u)
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[deleteUnsubmittedMonthlyReturn] formp-proxy unsubmitted monthly return delete failed", t)
            BadGateway(Json.obj("message" -> "delete-unsubmitted-monthly-return-failed"))
        }
    }

  def getSubmittedMonthlyReturnsData: Action[GetSubmittedMonthlyReturnsDataRequest] =
    authorise.async(parse.json[GetSubmittedMonthlyReturnsDataRequest]) { implicit request =>
      service
        .getSubmittedMonthlyReturnsData(request.body)
        .map(res => Ok(Json.toJson(res)))
        .recover {
          case u: UpstreamErrorResponse =>
            Status(u.statusCode)(Json.obj("message" -> u.message))
          case NonFatal(t)              =>
            logger.error("[getUnsubmittedMonthlyReturns] formp-proxy get submitted monthly returns data failed", t)
            BadGateway(Json.obj("message" -> "get-submitted-monthly-returns-data-failed"))
        }
    }
}
