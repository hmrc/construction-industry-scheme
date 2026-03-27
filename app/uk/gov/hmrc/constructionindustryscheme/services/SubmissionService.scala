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
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime, ZoneOffset}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.Try

@Singleton
class SubmissionService @Inject() (
  chrisConnector: ChrisConnector,
  formpProxyConnector: FormpProxyConnector,
  emailConnector: EmailConnector,
  monthlyReturnService: MonthlyReturnService,
  chrisSubmissionSessionStore: ChrisSubmissionSessionStore
)(implicit ec: ExecutionContext)
    extends Logging {
  def createSubmission(request: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[String] =
    formpProxyConnector.createSubmission(request)

  def updateSubmission(req: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateSubmission(req)

  def submitToChris(payload: BuiltSubmissionPayload)(implicit hc: HeaderCarrier): Future[SubmissionResult] =
    chrisConnector.submitEnvelope(payload.envelope, payload.correlationId)

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

  def updateGovTalkStatusCorrelationId(request: UpdateGovTalkStatusCorrelationIdRequest)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    formpProxyConnector.updateGovTalkStatusCorrelationId(request)

  def updateGovTalkStatusStatistics(request: UpdateGovTalkStatusStatisticsRequest)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    formpProxyConnector.updateGovTalkStatusStatistics(request)

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

  def processInitialChrisAck(
    employerReference: EmployerReference,
    submissionId: String,
    expectedCorrelationId: String,
    actualCorrelationId: String,
    pollInterval: Int,
    pollUrl: String,
    gatewayURL: String,
    lastMessageDate: Instant = Instant.now
  )(implicit hc: HeaderCarrier): Future[Unit] =
    validateCorrelationId(expectedCorrelationId, actualCorrelationId) match {
      case Left(reason) =>
        Future.failed(new RuntimeException(reason))

      case Right(_) =>
        for {
          instanceId <- initialiseGovTalkStatus(employerReference, submissionId, expectedCorrelationId, gatewayURL)
          _          <- chrisSubmissionSessionStore.saveInitialAck(
                          submissionId,
                          instanceId,
                          expectedCorrelationId,
                          pollInterval,
                          pollUrl,
                          lastMessageDate
                        )
          _          <- runGovTalkStatusUpdateSteps(
                          instanceId,
                          submissionId,
                          expectedCorrelationId,
                          lastMessageDate,
                          0,
                          pollInterval,
                          gatewayURL
                        )
        } yield ()
    }

  def processInitialChrisFailure(
    employerReference: EmployerReference,
    submissionId: String,
    correlationId: String,
    gatewayURL: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      instanceId <- initialiseGovTalkStatus(employerReference, submissionId, correlationId, gatewayURL)
      _          <- updateGovTalkStatus(UpdateGovTalkStatusRequest(instanceId, submissionId, None, "dataRequest"))
    } yield ()

  def pollSubmissionAndUpdateGovTalkStatus(
    submissionId: String,
    pollUrl: String
  )(implicit hc: HeaderCarrier): Future[ChrisPollResponse] =
    for {
      session            <- getChrisSubmissionSession(submissionId)
      _                  <- fetchAndStoreGovTalkStatus(session.instanceId, submissionId)
      result             <- chrisConnector.pollSubmission(session.correlationId, pollUrl)
      _                  <- validateCorrelationId(session.correlationId, result.correlationId) match {
                              case Right(_)     => Future.unit
                              case Left(reason) => Future.failed(new RuntimeException(reason))
                            }
      _                  <- deleteChrisReourcesIfNeeded(response.status, correlationId, pollUrl)
      nextLastMessageDate = result.lastMessageDate
                              .flatMap(ts => Try(Instant.parse(ts)).toOption)
                              .getOrElse(session.lastMessageDate)
      nextPollUrl         = result.pollUrl.getOrElse(session.pollUrl)
      nextPollInterval    = result.pollInterval.getOrElse(session.pollInterval)
      _                  <- chrisSubmissionSessionStore.updateAfterPoll(
                              submissionId,
                              result.correlationId,
                              nextLastMessageDate,
                              nextPollInterval,
                              nextPollUrl
                            )
      updatedSession     <- getChrisSubmissionSession(submissionId)
      _                  <- runGovTalkStatusUpdateSteps(
                              updatedSession.instanceId,
                              submissionId,
                              updatedSession.correlationId,
                              nextLastMessageDate,
                              updatedSession.numPolls,
                              nextPollInterval,
                              nextPollUrl
                            )
      _                  <- fetchAndStoreGovTalkStatus(session.instanceId, submissionId)
    } yield result

  private def fetchAndStoreGovTalkStatus(instanceId: String, submissionId: String)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    getGovTalkStatus(GetGovTalkStatusRequest(instanceId, submissionId)).flatMap {
      case Some(status) =>
        chrisSubmissionSessionStore.saveGovTalkStatus(submissionId, status)

      case None =>
        Future.failed(
          new RuntimeException(s"No GovTalk status found for instanceId: $instanceId, submissionId: $submissionId")
        )
    }

  private def validateCorrelationId(expectedRaw: String, actualRaw: String): Either[String, Unit] = {
    val expected = expectedRaw.trim
    val actual   = actualRaw.trim

    if (expected.isEmpty || actual.isEmpty) Left(s"CorrelationId is empty")
    else if (expected != actual) Left(s"CorrelationId mismatch: expected '$expected' but got '$actual'")
    else Right(())
  }

  private def runGovTalkStatusUpdateSteps(
    instanceId: String,
    submissionId: String,
    correlationId: String,
    lastMessageDate: Instant,
    numPolls: Int,
    pollInterval: Int,
    gatewayURL: String,
    protocolStatus: String = "dataPoll"
  )(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      _ <- updateGovTalkStatusCorrelationId(
             UpdateGovTalkStatusCorrelationIdRequest(
               instanceId,
               submissionId,
               correlationId,
               pollInterval,
               gatewayURL
             )
           )
      _ <- updateGovTalkStatusStatistics(
             UpdateGovTalkStatusStatisticsRequest(
               instanceId,
               submissionId,
               toLocalDateTime(lastMessageDate),
               numPolls,
               pollInterval,
               gatewayURL
             )
           )
      _ <- updateGovTalkStatus(
             UpdateGovTalkStatusRequest(
               instanceId,
               submissionId,
               None,
               protocolStatus
             )
           )
    } yield ()

  private def getChrisSubmissionSession(submissionId: String): Future[ChrisSubmissionSessionData] =
    chrisSubmissionSessionStore.get(submissionId).map {
      case Some(session) => session
      case None          => throw new RuntimeException(s"No session found for submissionId: $submissionId")
    }

  private def toLocalDateTime(i: Instant): LocalDateTime =
    LocalDateTime.ofInstant(i, ZoneOffset.UTC)

}
