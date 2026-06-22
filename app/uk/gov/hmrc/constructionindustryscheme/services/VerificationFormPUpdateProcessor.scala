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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ProcessVerificationResponseFromChrisRequest, UpdateVerificationSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, StoredVerificationContext}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class VerificationFormPUpdateProcessor @Inject() (
  formpProxyConnector: FormpProxyConnector,
  verificationResultMapper: VerificationResultMapper
)(implicit ex: ExecutionContext)
    extends FormPSubmissionUpdateProcessor {

  override val journey: ChrisPollJourney = ChrisPollJourney.Verification

  override def handleInitialAccepted(
    session: ChrisSubmissionSessionData,
    response: SubmissionResult
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val ctx =
      session.verificationContext.getOrElse(
        throw new IllegalStateException(
          s"Verification context is missing for submissionId: ${session.submissionId}"
        )
      )

    formpProxyConnector.updateVerificationSubmission(
      UpdateVerificationSubmissionRequest(
        instanceId = session.instanceId,
        verificationBatchResourceRef = ctx.verificationBatchResourceRef,
        submittableStatus = response.status.toString,
        submissionRequestDate = Some(ctx.submissionRequestDate),
        hmrcMarkGenerated = Some(ctx.hmrcMarkGenerated),
        govtalkErrorCode = response.meta.error.map(_.errorNumber),
        govtalkErrorType = response.meta.error.map(_.errorType),
        govtalkErrorMessage = response.meta.error.map(_.errorText)
      )
    )
  }

  override def handlePollResponse(
    session: ChrisSubmissionSessionData,
    response: ChrisPollResponse
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    val ctx =
      session.verificationContext.getOrElse(
        throw new IllegalStateException(
          s"Verification context is missing for submissionId: ${session.submissionId}"
        )
      )

    if (isVerificationSuccess(response)) handleSuccess(session, ctx, response)
    else handleNonSuccess(session, ctx, response)
  }

  private def handleSuccess(
    session: ChrisSubmissionSessionData,
    ctx: StoredVerificationContext,
    response: ChrisPollResponse
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    val result =
      for {
        acceptedTime <- requiredField(response.acceptedTime, "acceptedTime")
        verifiedDate <- parseDateTime(acceptedTime, "acceptedTime")
      } yield (acceptedTime, verifiedDate)

    result match {
      case Failure(error) =>
        Future.failed(error)

      case Success((acceptedTime, verifiedDate)) =>
        for {
          mappedResults <- verificationResultMapper.mapAll(
                             chrisResults = response.cisResponseSubcontractors,
                             context = ctx,
                             verifiedDate = verifiedDate
                           )
          _             <- formpProxyConnector.processVerificationResponseFromChris(
                             ProcessVerificationResponseFromChrisRequest(
                               instanceId = session.instanceId,
                               verificationBatchResourceRef = ctx.verificationBatchResourceRef,
                               acceptedTime = acceptedTime,
                               submissionStatus = response.status.toString,
                               irMarkReceived = response.irMarkReceived,
                               verificationResults = mappedResults
                             )
                           )
        } yield ()
    }
  }

  private def handleNonSuccess(
    session: ChrisSubmissionSessionData,
    ctx: StoredVerificationContext,
    response: ChrisPollResponse
  )(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateVerificationSubmission(
      UpdateVerificationSubmissionRequest(
        instanceId = session.instanceId,
        verificationBatchResourceRef = ctx.verificationBatchResourceRef,
        submittableStatus = response.status.toString,
        submissionRequestDate = Some(ctx.submissionRequestDate),
        hmrcMarkGenerated = Some(ctx.hmrcMarkGenerated),
        govtalkErrorCode = response.error.flatMap(error => (error \ "errorNumber").asOpt[String]),
        govtalkErrorType = response.error.flatMap(error => (error \ "errorType").asOpt[String]),
        govtalkErrorMessage = response.error.flatMap(error => (error \ "errorText").asOpt[String])
      )
    )

  private def isVerificationSuccess(response: ChrisPollResponse): Boolean =
    response.status == SUBMITTED &&
      response.cisResponseSubcontractors.nonEmpty

  private def requiredField(
    value: Option[String],
    fieldName: String
  ): Try[String] =
    value.map(_.trim).filter(_.nonEmpty) match {
      case Some(value) => Success(value)
      case None        => Failure(new RuntimeException(s"Missing required field: $fieldName"))
    }

  private def parseDateTime(
    value: String,
    fieldName: String
  ): Try[LocalDateTime] =
    Try(LocalDateTime.parse(value)).recoverWith { case ex =>
      Failure(new RuntimeException(s"Invalid date format for field: $fieldName", ex))
    }
}
