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
import play.api.libs.json.*
import play.api.mvc.*
import play.api.mvc.Results.*
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.*
import uk.gov.hmrc.constructionindustryscheme.models.audit.{AuditResponseReceivedModel, XmlConversionResult}
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED as AcceptedStatus, ChRISSubmission, ChrisPollJourney, ChrisSubmissionContext, CisVerificationSubmission, DEPARTMENTAL_ERROR as DepartmentalErrorStatus, EmployerReference, FATAL_ERROR as FatalErrorStatus, GovTalkErrorStatus, MonthlyReturnSubmissionContext, STARTED as StartedStatus, SUBMITTED as SubmittedStatus, SUBMITTED_NO_RECEIPT as SubmittedNoReceiptStatus, SubmissionResult, VerificationSubmissionContextBuilder}
import uk.gov.hmrc.constructionindustryscheme.services.{AuditService, SubmissionService}
import uk.gov.hmrc.constructionindustryscheme.services.chris.GovTalkErrorStatusClassifier
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.constructionindustryscheme.utils.{UriHelper, XmlToJsonConvertor, XmlValidator}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.*
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, RedirectUrl}

import java.time.{Clock, Instant, LocalDateTime}
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

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
          errors => Future.successful(BadRequest(JsError.toJson(errors))),
          submission =>
            submissionService
              .createSubmission(submission)
              .map(id => Created(Json.obj("submissionId" -> id)))
              .recover { case ex =>
                logger.error("[create] formp-proxy create failed", ex)
                BadGateway(Json.obj("message" -> "create-submission-failed"))
              }
        )
    }

  def submitToChris(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[ChrisSubmissionRequest]
        .fold(
          errors => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors)))),
          submission => handleSubmitToChris(submissionId, submission)
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

  def resetGovTalk(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit req =>
      req.body
        .validate[ResetGovTalkStatusRequest]
        .fold(
          e => Future.successful(BadRequest(JsError.toJson(e))),
          request =>
            submissionService
              .resetGovTalkStatus(request)
              .map(_ => NoContent)
              .recover { case ex =>
                logger.error(s"[resetGovTalk] formp-proxy reset failed submissionId=$submissionId", ex)
                BadGateway(Json.obj("submissionId" -> submissionId, "message" -> "reset-govtalk-failed"))
              }
        )
    }

  private lazy val redirectUrlPolicy = AbsoluteWithHostnameFromAllowlist(appConfig.chrisHost.toSet)

  def pollSubmission(pollUrl: RedirectUrl, submissionId: String): Action[AnyContent] =
    pollChris(pollUrl, submissionId, MonthlyReturn)

  def pollVerificationSubmission(pollUrl: RedirectUrl, submissionId: String): Action[AnyContent] =
    pollChris(pollUrl, submissionId, Verification)

  private def pollChris(
    pollUrl: RedirectUrl,
    submissionId: String,
    journey: ChrisPollJourney
  ): Action[AnyContent] =
    authorise.async { implicit req =>
      pollUrl.getEither(redirectUrlPolicy) match {
        case Right(safeUrl) =>
          val overridePollUrl: String = (
            if (appConfig.useOverridePollResponseEndPoint) {
              UriHelper.replaceHostIgnoringUserInfoAndPort(safeUrl.url, appConfig.overridePollResponseEndPoint)
            } else None
          ).getOrElse(safeUrl.url)

          logger.info(
            s"[${journey.logName} poll] useOverridePollResponseEndPoint: ${appConfig.useOverridePollResponseEndPoint}"
          )
          logger.info(s"[${journey.logName} poll] safeUrl.url: ${safeUrl.url}")
          logger.info(s"[${journey.logName} poll] overridePollUrl: $overridePollUrl")

          submissionService
            .pollSubmissionAndUpdateGovTalkStatus(submissionId, overridePollUrl, journey)
            .map { resp =>
              Ok(
                Json.obj(
                  "status"             -> resp.status.toString,
                  "pollUrl"            -> resp.pollUrl,
                  "correlationId"      -> resp.correlationId,
                  "intervalSeconds"    -> resp.pollInterval,
                  "error"              -> resp.error,
                  "irMarkReceived"     -> resp.irMarkReceived,
                  "lastMessageDate"    -> resp.lastMessageDate,
                  "acceptedTime"       -> resp.acceptedTime,
                  "govTalkErrorStatus" -> resp.govTalkErrorStatus
                )
              )
            }

        case Left(value) =>
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

  def createMonthlyNilReturnRequestJson(payload: ChRISSubmission): JsValue =
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

  private def renderChrisResponse(submissionId: String, irMark: String, res: SubmissionResult): Result = {

    val gatewayTimestamp: String = res.meta.gatewayTimestamp match {
      case Some(s) if s.trim.nonEmpty => s.trim
      case _                          => Instant.now(clock).toString
    }

    val base = Json.obj(
      "submissionId"       -> submissionId,
      "hmrcMarkGenerated"  -> irMark,
      "correlationId"      -> res.meta.correlationId,
      "gatewayTimestamp"   -> gatewayTimestamp,
      "acceptedTime"       -> res.meta.acceptedTime,
      "govTalkErrorStatus" -> res.govTalkErrorStatus
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

    res.status match {
      case AcceptedStatus           => Results.Accepted(withPoll(withStatus("ACCEPTED")))
      case SubmittedStatus          => Results.Ok(withStatus("SUBMITTED"))
      case SubmittedNoReceiptStatus => Results.Ok(withStatus("SUBMITTED_NO_RECEIPT"))
      case StartedStatus            => Results.Ok(withStatus("STARTED") ++ errorObj("recoverable error"))
      case DepartmentalErrorStatus  => Results.Ok(withStatus("DEPARTMENTAL_ERROR") ++ errorObj("departmental error"))
      case FatalErrorStatus         => Results.Ok(withStatus("FATAL_ERROR") ++ errorObj("fatal error"))
    }
  }

  private def renderSubmissionResponse(submissionId: String, payload: ChRISSubmission)(
    res: SubmissionResult
  )(implicit hc: HeaderCarrier): Result = {
    val monthlyNilReturnResponse =
      AuditResponseReceivedModel(res.status.toString, createMonthlyNilReturnResponseJson(res))
    auditService.monthlyNilReturnResponseEvent(monthlyNilReturnResponse)

    renderChrisResponse(submissionId, payload.irMark, res)
  }

  private def handleSubmitToChris(submissionId: String, csr: ChrisSubmissionRequest)(implicit
    req: AuthenticatedRequest[JsValue]
  ): Future[Result] = {
    val payload               = ChRISSubmission.buildPayload(csr, req)
    val submissionRequestDate = LocalDateTime.now(clock)
    val monthlyReturnContext  =
      MonthlyReturnSubmissionContext(
        hmrcMarkGenerated = payload.irMark,
        submissionRequestDate = submissionRequestDate
      )

    auditService.monthlyNilReturnRequestEvent(createMonthlyNilReturnRequestJson(payload))

    xmlValidator.validate(payload.irEnvelope, appConfig.cisReturnSchema) match {
      case Failure(e) =>
        logger.error(
          s"ChRIS monthly return XML validation failed, but continuing with ChRIS submission for correlationId=${payload.correlationId}: ${e.getMessage}",
          e
        )

      case Success(_) =>
        logger.info(
          s"ChRIS monthly return XML validation successful. Sending ChRIS submission for correlationId=${payload.correlationId}."
        )
    }

    val employerRef = EmployerReference(csr.clientTaxOfficeNumber, csr.clientTaxOfficeRef)

    submissionService
      .submitToChris(payload)
      .flatMap(res =>
        handleInitialChrisAck(
          submissionId,
          employerRef,
          payload.irMark,
          payload.correlationId,
          res,
          MonthlyReturn,
          monthlyReturnContext,
          r => renderSubmissionResponse(submissionId, payload)(r),
          errorLabel = "",
          isResubmission = csr.isResubmission
        )
      )
      .recoverWith { case NonFatal(ex) =>
        handleInitialChrisFailure(
          submissionId,
          employerRef,
          payload.irMark,
          payload.correlationId,
          ex,
          errorLabel = "",
          startedErrorText = "Chris failure"
        )
      }
  }

  private def handleInitialChrisAck(
    submissionId: String,
    employerRef: EmployerReference,
    irMark: String,
    correlationId: String,
    res: SubmissionResult,
    journey: ChrisPollJourney,
    context: ChrisSubmissionContext,
    render: SubmissionResult => Result,
    errorLabel: String,
    isResubmission: Boolean = false
  )(implicit hc: HeaderCarrier): Future[Result] =
    submissionService
      .processInitialChrisAck(
        employerReference = employerRef,
        submissionId = submissionId,
        expectedCorrelationId = correlationId,
        actualCorrelationId = res.meta.correlationId,
        pollInterval = res.meta.responseEndPoint.pollIntervalSeconds,
        pollUrl = res.meta.responseEndPoint.url,
        gatewayURL = appConfig.chrisGatewayUrl,
        lastMessageDate = chrisResponseTimestamp(res),
        journey = journey,
        context = context,
        response = res,
        isResubmission = isResubmission
      )
      .map(_ => render(res))
      .recover { case ex =>
        logger.error(s"Failed to handle initial ChRIS$errorLabel response", ex)
        BadGateway(
          withError(
            baseSubmissionResponseJson(submissionId, irMark, correlationId, "FATAL_ERROR"),
            ex.getMessage
          )
        )
      }

  private def handleInitialChrisFailure(
    submissionId: String,
    employerRef: EmployerReference,
    irMark: String,
    correlationId: String,
    ex: Throwable,
    errorLabel: String,
    startedErrorText: String
  )(implicit hc: HeaderCarrier): Future[Result] = {
    logger.error(
      s"Received 5xx/Exception from ChRIS$errorLabel, treating as RESUBMIT for submissionId=$submissionId",
      ex
    )

    val classified: GovTalkErrorStatus = classifyChrisFailure(ex)

    submissionService
      .processInitialChrisFailure(
        employerRef,
        submissionId,
        correlationId,
        appConfig.chrisGatewayUrl
      )
      .map { _ =>
        Ok(
          withError(
            baseSubmissionResponseJson(submissionId, irMark, correlationId, "STARTED", Some(classified)),
            startedErrorText
          )
        )
      }
      .recover { case e =>
        logger.error(s"Failed to initialise/update GovTalk status after ChRIS$errorLabel 5xx", e)
        InternalServerError(
          withError(
            baseSubmissionResponseJson(submissionId, irMark, correlationId, "FATAL_ERROR", Some(classified)),
            "GovTalk status already exists"
          )
        )
      }
  }

  private def classifyChrisFailure(ex: Throwable): GovTalkErrorStatus = ex match {
    case err: UpstreamErrorResponse if err.statusCode >= 500 && err.statusCode < 600 =>
      GovTalkErrorStatusClassifier.fromHttpStatus(err.statusCode)
    case _                                                                           =>
      GovTalkErrorStatusClassifier.noResponse
  }

  private def baseSubmissionResponseJson(
    submissionId: String,
    irMark: String,
    correlationId: String,
    status: String,
    govTalkErrorStatus: Option[GovTalkErrorStatus] = None,
    gatewayTimestamp: String = Instant.now(clock).toString
  ): JsObject =
    Json.obj(
      "submissionId"       -> submissionId,
      "hmrcMarkGenerated"  -> irMark,
      "correlationId"      -> correlationId,
      "gatewayTimestamp"   -> gatewayTimestamp,
      "status"             -> status,
      "govTalkErrorStatus" -> govTalkErrorStatus
    )

  private def withError(json: JsObject, text: String): JsObject =
    json ++ Json.obj("error" -> Json.obj("text" -> text))

  private def chrisResponseTimestamp(res: SubmissionResult): Instant =
    res.meta.gatewayTimestamp
      .flatMap(ts => Try(Instant.parse(ts)).toOption)
      .getOrElse(Instant.now(clock))

  def submitVerificationToChris(submissionId: String): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body
        .validate[ChrisVerificationRequest]
        .fold(
          errors => Future.successful(BadRequest(Json.obj("message" -> JsError.toJson(errors)))),
          verificationRequest => handleSubmitVerificationToChris(submissionId, verificationRequest)
        )
    }

  private def handleSubmitVerificationToChris(
    submissionId: String,
    cvr: ChrisVerificationRequest
  )(implicit req: AuthenticatedRequest[JsValue]): Future[Result] = {
    val payload = CisVerificationSubmission.buildPayload(cvr, req.enrolments)

    VerificationSubmissionContextBuilder
      .build(
        request = cvr,
        hmrcMarkGenerated = payload.irMark,
        submissionRequestDate = LocalDateTime.now(clock)
      ) match {
      case Left(error) =>
        Future.successful(BadRequest(Json.obj("message" -> error)))

      case Right(verificationContext) =>
        xmlValidator.validate(payload.irEnvelope, appConfig.cisVerificationSchema) match {
          case Failure(e) =>
            logger.error(
              s"Chris verification XML validation failed, but continuing with ChRIS submission for correlationId=${payload.correlationId}: ${e.getMessage}",
              e
            )

          case Success(_) =>
            logger.info(
              s"Chris verification XML validation successful. Sending ChRIS verification submission for correlationId=${payload.correlationId}."
            )
            // todo: for testing purposes,  remove before marging
            logger.info(s"full chris xml envelope:${payload.envelope}")
        }

        val employerRef = EmployerReference(cvr.clientTaxOfficeNumber, cvr.clientTaxOfficeRef)

        submissionService
          .submitVerificationToChris(payload)
          .flatMap(res =>
            handleInitialChrisAck(
              submissionId,
              employerRef,
              payload.irMark,
              payload.correlationId,
              res,
              Verification,
              verificationContext,
              r => renderChrisResponse(submissionId, payload.irMark, r),
              errorLabel = " verification"
            )
          )
          .recoverWith { case NonFatal(ex) =>
            handleInitialChrisFailure(
              submissionId,
              employerRef,
              payload.irMark,
              payload.correlationId,
              ex,
              errorLabel = " verification",
              startedErrorText = "Chris verification failure"
            )
          }
    }
  }
}
