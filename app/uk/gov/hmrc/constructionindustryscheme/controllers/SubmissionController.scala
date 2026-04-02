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
import play.api.mvc.Results.*

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED as AcceptedStatus, BuiltSubmissionPayload, DEPARTMENTAL_ERROR as DepartmentalErrorStatus, EmployerReference, FATAL_ERROR as FatalErrorStatus, STARTED as StartedStatus, SUBMITTED as SubmittedStatus, SUBMITTED_NO_RECEIPT as SubmittedNoReceiptStatus, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.services.{AuditService, SubmissionService}
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisSubmissionEnvelopeBuilder
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.constructionindustryscheme.models.audit.{AuditResponseReceivedModel, XmlConversionResult}
import uk.gov.hmrc.constructionindustryscheme.utils.{UriHelper, XmlToJsonConvertor, XmlValidator}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*

import java.time.{Clock, Instant}
import java.util.UUID
import scala.util.{Failure, Success, Try}
import scala.util.control.NonFatal

class SubmissionController @Inject() (
  authorise: AuthAction,
  submissionService: SubmissionService,
  auditService: AuditService,
  xmlValidator: XmlValidator,
  cc: ControllerComponents,
  appConfig: AppConfig,
  clock: Clock
)(implicit ec: ExecutionContext)
    extends BackendController(cc)
    with Logging {

  implicit val reads: Reads[ChrisSubmissionRequest] = Json.reads[ChrisSubmissionRequest]

  def createSubmission: Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[CreateSubmissionRequest]
        .fold(
          errs => Future.successful(BadRequest(JsError.toJson(errs))),
          csr =>
            submissionService
              .createSubmission(csr)
              .map(id => Created(Json.obj("submissionId" -> id)))
              .recover { case ex =>
                logger.error("[create] formp-proxy create failed", ex)
                BadGateway(Json.obj("message" -> "create-submission-failed"))
              }
        )
    }

  def submitToChris(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit req =>
      req.body
        .validate[ChrisSubmissionRequest]
        .fold(
          errs => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errs)))),
          csr => handleSubmitToChris(submissionId, csr)
        )
    }

  def updateSubmission(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit req =>
      req.body
        .validate[UpdateSubmissionRequest]
        .fold(
          e => Future.successful(BadRequest(JsError.toJson(e))),
          upd =>
            submissionService
              .updateSubmission(upd)
              .map { _ =>
                NoContent
              }
              .recover { case ex =>
                logger.error("[updateSubmission] formp-proxy update failed", ex)
                BadGateway(Json.obj("submissionId" -> submissionId, "message" -> "update-submission-failed"))
              }
        )
    }

  private lazy val redirectUrlPolicy = AbsoluteWithHostnameFromAllowlist(appConfig.chrisHost.toSet)

  def pollSubmission(pollUrl: RedirectUrl, submissionId: String): Action[AnyContent] =
    authorise.async { implicit req =>
      pollUrl.getEither(redirectUrlPolicy) match {
        case Right(safeUrl) =>
          val overridePollUrl: String = if (appConfig.useOverridePollResponseEndPoint) {
            UriHelper.replaceHostIgnoringUserInfoAndPort(safeUrl.url, appConfig.overridePollResponseEndPoint) match {
              case Some(x) => x
              case _       => safeUrl.url
            }
          } else safeUrl.url
          logger.info(s"useOverridePollResponseEndPoint: $appConfig.useOverridePollResponseEndPoint")
          logger.info(s"safeUrl.url: $safeUrl.url")
          logger.info(s"overridePollUrl: $overridePollUrl")

          submissionService
            .pollSubmissionAndUpdateGovTalkStatus(submissionId, overridePollUrl)
            .map {
              case ChrisPollResponse(
                    status,
                    correlationId,
                    overridePollUrl,
                    interval,
                    error,
                    irMarkReceived,
                    lastMessageDate
                  ) =>
                Ok(
                  Json.obj(
                    "status"          -> status.toString,
                    "pollUrl"         -> overridePollUrl,
                    "intervalSeconds" -> interval,
                    "error"           -> error,
                    "irMarkReceived"  -> irMarkReceived,
                    "lastMessageDate" -> lastMessageDate
                  )
                )
            }
        case Left(value)    =>
          logger.warn(s"could not poll the pollUrl provided as the host is not recognised: $value ")
          Future.successful(BadRequest(Json.obj("error" -> "pollUrl does not have the right host")))
      }

    }

  def sendSuccessfulEmail(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit req =>
      req.body
        .validate[SendSuccessEmailRequest]
        .fold(
          errs => Future.successful(BadRequest(JsError.toJson(errs))),
          params =>
            submissionService
              .sendSuccessfulEmail(submissionId, params)
              .map(_ => Accepted)
              .recover { case ex =>
                logger.error(s"[sendSuccessfulEmail] failed submissionId=$submissionId", ex)
                BadGateway(Json.obj("message" -> "send-success-email-failed"))
              }
        )
    }

  def createMonthlyNilReturnRequestJson(payload: BuiltSubmissionPayload): JsValue =
    XmlToJsonConvertor.convertXmlToJson(payload.envelope.toString) match {
      case XmlConversionResult(true, Some(json), _)   => json
      case XmlConversionResult(false, _, Some(error)) => Json.obj("error" -> error)
      case _                                          => Json.obj("error" -> "unexpected conversion failure")
    }

  def createMonthlyNilReturnResponseJson(res: SubmissionResult): JsValue =
    XmlToJsonConvertor.convertXmlToJson(res.rawXml) match {
      case XmlConversionResult(true, Some(json), _) => json
      case _                                        => Json.toJson(res.rawXml)
    }

  private def renderSubmissionResponse(submissionId: String, payload: BuiltSubmissionPayload)(
    res: SubmissionResult
  ): Result = {

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val gatewayTimestamp: String = res.meta.gatewayTimestamp match {
      case Some(s) if s.trim.nonEmpty => s.trim
      case _                          => Instant.now(clock).toString
    }

    val base = Json.obj(
      "submissionId"      -> submissionId,
      "hmrcMarkGenerated" -> payload.irMark,
      "correlationId"     -> res.meta.correlationId,
      "gatewayTimestamp"  -> gatewayTimestamp
    )

    def withStatus(s: String): JsObject = base ++ Json.obj("status" -> s)

    def withPoll(o: JsObject): JsObject = {
      val endpoint = res.meta.responseEndPoint
      o ++ Json.obj(
        "responseEndPoint" -> Json.obj(
          "url"                 -> endpoint.url,
          "pollIntervalSeconds" -> endpoint.pollIntervalSeconds
        )
      )
    }

    def errorObj(defaultText: String): JsObject =
      Json.obj(
        "error" ->
          res.meta.error
            .map(e => Json.obj("number" -> e.errorNumber, "type" -> e.errorType, "text" -> e.errorText))
            .getOrElse(Json.obj("text" -> defaultText))
      )

    val monthlyNilReturnResponseJson: JsValue = createMonthlyNilReturnResponseJson(res)

    val monthlyNilReturnResponse = AuditResponseReceivedModel(res.status.toString, monthlyNilReturnResponseJson)
    auditService.monthlyNilReturnResponseEvent(monthlyNilReturnResponse)

    res.status match {
      case AcceptedStatus           => Results.Accepted(withPoll(withStatus("ACCEPTED")))
      case SubmittedStatus          => Results.Ok(withStatus("SUBMITTED"))
      case SubmittedNoReceiptStatus => Results.Ok(withStatus("SUBMITTED_NO_RECEIPT"))
      case StartedStatus            => Results.Ok(withStatus("STARTED") ++ errorObj("recoverable error"))
      case DepartmentalErrorStatus  => Results.Ok(withStatus("DEPARTMENTAL_ERROR") ++ errorObj("departmental error"))
      case FatalErrorStatus         => Results.Ok(withStatus("FATAL_ERROR") ++ errorObj("fatal error"))
    }
  }

  private def handleSubmitToChris(submissionId: String, csr: ChrisSubmissionRequest)(implicit
    req: AuthenticatedRequest[JsValue]
  ): Future[Result] = {
    val correlationId = UUID.randomUUID().toString.replace("-", "").toUpperCase
    val payload       = ChrisSubmissionEnvelopeBuilder.buildPayload(csr, req, correlationId)

    auditService.monthlyNilReturnRequestEvent(createMonthlyNilReturnRequestJson(payload))

    xmlValidator.validate(payload.irEnvelope) match {
      case Failure(e) =>
        logger.error(s"ChRIS XML validation failed: ${e.getMessage}", e)
        Future.failed(new RuntimeException(s"XML validation failed: ${e.getMessage}", e))

      case Success(_) =>
        logger.info(s"ChRIS XML validation successful. Sending ChRIS submission for a correlationId = $correlationId.")
        submissionService
          .submitToChris(payload)
          .flatMap(res => handleChrisResponse(submissionId, csr, correlationId, payload, res))
          .recoverWith { case NonFatal(ex) =>
            handleChrisFailure(submissionId, csr, correlationId, payload, ex)
          }
    }
  }

  private def handleChrisResponse(
    submissionId: String,
    csr: ChrisSubmissionRequest,
    correlationId: String,
    payload: BuiltSubmissionPayload,
    res: SubmissionResult
  )(implicit hc: HeaderCarrier): Future[Result] =
    submissionService
      .processInitialChrisAck(
        EmployerReference(csr.clientTaxOfficeNumber, csr.clientTaxOfficeRef),
        submissionId,
        correlationId,
        res.meta.correlationId,
        res.meta.responseEndPoint.pollIntervalSeconds,
        res.meta.responseEndPoint.url,
        appConfig.chrisGatewayUrl,
        chrisResponseTimestamp(res)
      )
      .map(_ => renderSubmissionResponse(submissionId, payload)(res))
      .recover { case ex =>
        logger.error(s"Failed to handle initial ChRIS response", ex)
        BadGateway(
          withError(
            baseSubmissionResponseJson(submissionId, payload, correlationId, "FATAL_ERROR"),
            ex.getMessage
          )
        )
      }

  private def handleChrisFailure(
    submissionId: String,
    csr: ChrisSubmissionRequest,
    correlationId: String,
    payload: BuiltSubmissionPayload,
    ex: Throwable
  )(implicit hc: HeaderCarrier): Future[Result] = {
    logger.error(s"Received 5xx/Exception from ChRIS, treating as RESUBMIT for submissionId=$submissionId", ex)

    submissionService
      .processInitialChrisFailure(
        EmployerReference(csr.clientTaxOfficeNumber, csr.clientTaxOfficeRef),
        submissionId,
        correlationId,
        appConfig.chrisGatewayUrl
      )
      .map { _ =>
        Ok(
          withError(baseSubmissionResponseJson(submissionId, payload, correlationId, "STARTED"), "Chris failure")
        )
      }
      .recover { case ex =>
        logger.error(s"Failed to initialise/update GovTalk status after 5xx", ex)
        InternalServerError(
          withError(
            baseSubmissionResponseJson(submissionId, payload, correlationId, "FATAL_ERROR"),
            "GovTalk status already exists"
          )
        )
      }
  }

  private def baseSubmissionResponseJson(
    submissionId: String,
    payload: BuiltSubmissionPayload,
    correlationId: String,
    status: String,
    gatewayTimestamp: String = Instant.now(clock).toString
  ): JsObject =
    Json.obj(
      "submissionId"      -> submissionId,
      "hmrcMarkGenerated" -> payload.irMark,
      "correlationId"     -> correlationId,
      "gatewayTimestamp"  -> gatewayTimestamp,
      "status"            -> status
    )

  private def withError(json: JsObject, text: String): JsObject =
    json ++ Json.obj("error" -> Json.obj("text" -> text))

  private def chrisResponseTimestamp(res: SubmissionResult): Instant =
    res.meta.gatewayTimestamp
      .flatMap(ts => Try(Instant.parse(ts)).toOption)
      .getOrElse(Instant.now(clock))

}
