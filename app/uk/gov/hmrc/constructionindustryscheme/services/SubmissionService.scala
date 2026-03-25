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

package uk.gov.hmrc.constructionindustryscheme.services

import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, EmailConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class SubmissionService @Inject() (
  chrisConnector: ChrisConnector,
  formpProxyConnector: FormpProxyConnector,
  emailConnector: EmailConnector,
  monthlyReturnService: MonthlyReturnService
)(implicit ec: ExecutionContext)
    extends Logging {
  def createSubmission(request: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[String] =
    formpProxyConnector.createSubmission(request)

  def updateSubmission(req: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateSubmission(req)

  def submitToChris(payload: BuiltSubmissionPayload)(implicit hc: HeaderCarrier): Future[SubmissionResult] =
    chrisConnector.submitEnvelope(payload.envelope, payload.correlationId)

  def pollSubmission(correlationId: String, pollUrl: String)(using HeaderCarrier): Future[ChrisPollResponse] =
    chrisConnector
      .pollSubmission(correlationId, pollUrl)
      .flatMap { response =>
        deleteChrisReourcesIfNeeded(response.status, correlationId, pollUrl)
          .map(_ => response)
      }

  def sendSuccessfulEmail(submissionId: String, request: SendSuccessEmailRequest)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    val emailPayload = NilMonthlyReturnOrgSuccessEmail(request.email, request.month, request.year)
    emailConnector.sendSuccessfulEmail(emailPayload).map(_ => ())
  }

  private def deleteChrisReourcesIfNeeded(
    status: SubmissionStatus,
    correlationId: String,
    pollUrl: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    status match {
      case SUBMITTED | SUBMITTED_NO_RECEIPT | DEPARTMENTAL_ERROR =>
        chrisConnector
          .deleteSubmission(correlationId, pollUrl)
          .recover { case NonFatal(ex) =>
            logger.warn(
              s"[SubmissionService] Failed to delete Chris resources for corrId=$correlationId url=$pollUrl",
              ex
            )
          }

      case _ =>
        Future.unit
    }

  def getGovTalkStatus(request: GetGovTalkStatusRequest)(implicit
    hc: HeaderCarrier
  ): Future[Option[GetGovTalkStatusResponse]] =
    formpProxyConnector.getGovTalkStatus(request)

  def createGovTalkStatusRecord(request: CreateGovTalkStatusRecordRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.createGovTalkStatusRecord(request)

  def updateGovTalkStatus(request: UpdateGovTalkStatusRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateGovTalkStatus(request)

  def initialiseGovTalkStatus(
    employerReference: EmployerReference,
    submissionId: String,
    correlationId: String,
    gatewayURL: String
  )(implicit hc: HeaderCarrier): Future[String] =
    monthlyReturnService.getCisTaxpayer(employerReference).flatMap { taxpayer =>
      val instanceId = taxpayer.uniqueId
      val getReq     = GetGovTalkStatusRequest(instanceId, submissionId)

      getGovTalkStatus(getReq).flatMap {
        case Some(_) =>
          Future.failed(new RuntimeException("govtalk status already exists"))

        case None =>
          val createReq = CreateGovTalkStatusRecordRequest(
            userIdentifier = instanceId,
            formResultID = submissionId,
            correlationID = correlationId,
            gatewayURL = gatewayURL
          )

          createGovTalkStatusRecord(createReq).map(_ => instanceId)
      }
    }

}
