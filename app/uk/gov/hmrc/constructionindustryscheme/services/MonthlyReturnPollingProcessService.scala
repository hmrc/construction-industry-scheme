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

import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{GetMonthlyReturnForEditRequest, SendSuccessEmailRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, MonthlyReturnSubmissionToPoll}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{LocalDateTime, ZoneId}
import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MonthlyReturnPollingProcessService @Inject() (
  monthlyReturnService: MonthlyReturnService,
  submissionService: SubmissionService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val ukTimezone: ZoneId =
    ZoneId.of("Europe/London")

  private val alertDuration =
    24.hours.toMillis

  private val unavailableReportValue =
    "-"

  private val notPolledCorrelationId =
    "(not polled)"

  def process(
    monthlyReturnSubmissions: Seq[MonthlyReturnSubmissionToPoll],
    startTime: Long
  )(implicit hc: HeaderCarrier): Future[Seq[PollReportContent]] =
    Future.traverse(monthlyReturnSubmissions) { submission =>
      processSubmission(submission, startTime)
        .recover { case NonFatal(exception) =>
          logger.error(
            s"[MonthlyReturnPollingProcessService][process] Failed for " +
              s"instanceId=${submission.instanceId}, " +
              s"submissionId=${submission.submissionId}",
            exception
          )

          toFailedReportContent(submission)
        }
    }

  private def processSubmission(
    submission: MonthlyReturnSubmissionToPoll,
    startTime: Long
  )(implicit hc: HeaderCarrier): Future[PollReportContent] = {
    logger.info(
      s"[MonthlyReturnPollingProcessService][processSubmission] Processing in-flight return: " +
        s"instanceId=${submission.instanceId}, " +
        s"submissionId=${submission.submissionId}, " +
        s"taxYear=${submission.taxYear}, " +
        s"taxMonth=${submission.taxMonth}"
    )

    for {
      details <- monthlyReturnService.getMonthlyReturnForEdit(
                   GetMonthlyReturnForEditRequest(
                     submission.instanceId,
                     submission.taxYear,
                     submission.taxMonth,
                     isAmendment = Some(false)
                   )
                 )

      monthlyReturn =
        details.monthlyReturn.headOption
          .getOrElse(
            throw new RuntimeException(
              s"No monthly return found for instanceId=${submission.instanceId}"
            )
          )

      submissionDetails =
        details.submission.headOption
          .getOrElse(
            throw new RuntimeException(
              s"No submission found for instanceId=${submission.instanceId}"
            )
          )

      reportContent <-
        if (hasMatchingStatus(monthlyReturn, submissionDetails)) {
          pollAndUpdateSubmission(
            submission = submission,
            monthlyReturn = monthlyReturn,
            submissionDetails = submissionDetails,
            startTime = startTime
          )
        } else {
          logger.warn(
            s"[MonthlyReturnPollingProcessService][processSubmission] " +
              s"Skipping ChRIS poll because F1 status does not match DB status for " +
              s"instanceId=${submission.instanceId}, " +
              s"submissionId=${submission.submissionId}, " +
              s"monthlyReturnStatus=${monthlyReturn.status.getOrElse(unavailableReportValue)}, " +
              s"submissionStatus=${submissionDetails.status.getOrElse(unavailableReportValue)}"
          )

          Future.successful(
            toNotPolledReportContent(
              submission = submission,
              dbSubmission = submissionDetails
            )
          )
        }
    } yield reportContent
  }

  private def pollAndUpdateSubmission(
    submission: MonthlyReturnSubmissionToPoll,
    monthlyReturn: MonthlyReturn,
    submissionDetails: Submission,
    startTime: Long
  )(implicit hc: HeaderCarrier): Future[PollReportContent] =
    for {
      gatewayUrl <- submissionService.processMonthlyReturnGovTalkStatusCheck(
                      submission.instanceId,
                      submission.submissionId.toString
                    )

      pollResponse <- submissionService.pollSubmissionAndUpdateGovTalkStatus(
                        submission.submissionId.toString,
                        gatewayUrl,
                        ChrisPollJourney.MonthlyReturn
                      )

      reportContent =
        toPollReportContent(
          submission,
          submissionDetails,
          pollResponse
        )

      _ = logPollDurationIfRequired(
            startTime,
            submissionDetails.submissionRequestDate,
            pollResponse.status,
            submission.submissionId.toString
          )

      updateRequest =
        UpdateSubmissionRequest(
          instanceId = submission.instanceId,
          taxYear = monthlyReturn.taxYear,
          taxMonth = monthlyReturn.taxMonth,
          hmrcMarkGenerated = submissionDetails.hmrcMarkGenerated,
          submittableStatus = currentReturnStatus(pollResponse),
          amendment = monthlyReturn.amendment.getOrElse("N"),
          hmrcMarkGgis = pollResponse.irMarkReceived,
          submissionRequestDate = submissionDetails.submissionRequestDate,
          acceptedTime = pollResponse.acceptedTime,
          emailRecipient = submissionDetails.emailRecipient,
          agentId = submissionDetails.agentId,
          govTalkResponse = pollResponse.govTalkErrorStatus
        )

      _ <- submissionService
             .updateSubmission(updateRequest)
             .recover { case NonFatal(exception) =>
               logger.error(
                 s"[MonthlyReturnPollingProcessService] Failed to update submissionId=${submission.submissionId}",
                 exception
               )
             }

      _ <- sendEmailIfRequired(
             pollResponse.status,
             submissionDetails.emailRecipient,
             monthlyReturn.taxMonth,
             monthlyReturn.taxYear,
             submission.submissionId.toString
           ).recover { case NonFatal(exception) =>
             logger.error(
               s"[MonthlyReturnPollingProcessService] Failed to send email for submissionId=${submission.submissionId}",
               exception
             )
           }
    } yield reportContent

  private def hasMatchingStatus(
    monthlyReturn: MonthlyReturn,
    dbSubmission: Submission
  ): Boolean =
    (monthlyReturn.status, dbSubmission.status) match {
      case (Some(monthlyReturnStatus), Some(submissionStatus)) =>
        monthlyReturnStatus.equalsIgnoreCase(submissionStatus)

      case _ =>
        false
    }

  private def toPollReportContent(
    submission: MonthlyReturnSubmissionToPoll,
    dbSubmission: Submission,
    pollResponse: ChrisPollResponse
  ): PollReportContent =
    PollReportContent(
      user = submission.instanceId,
      submissionType = submission.submissionType,
      submissionId = submission.submissionId.toString,
      govTalkRequestStatus = reportValue(submission.status),
      currentReturnStatus = currentReturnStatus(pollResponse),
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = reportValue(pollResponse.correlationId),
      agentId = dbSubmission.agentId
        .orElse(submission.agentId)
        .getOrElse(unavailableReportValue)
    )

  private def toNotPolledReportContent(
    submission: MonthlyReturnSubmissionToPoll,
    dbSubmission: Submission
  ): PollReportContent =
    PollReportContent(
      user = submission.instanceId,
      submissionType = submission.submissionType,
      submissionId = submission.submissionId.toString,
      govTalkRequestStatus = reportValue(submission.status),
      currentReturnStatus = unavailableReportValue,
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = notPolledCorrelationId,
      agentId = dbSubmission.agentId
        .orElse(submission.agentId)
        .getOrElse(unavailableReportValue)
    )

  private def toFailedReportContent(
    submission: MonthlyReturnSubmissionToPoll
  ): PollReportContent =
    PollReportContent(
      user = submission.instanceId,
      submissionType = submission.submissionType,
      submissionId = submission.submissionId.toString,
      govTalkRequestStatus = reportValue(submission.status),
      currentReturnStatus = unavailableReportValue,
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = notPolledCorrelationId,
      agentId = submission.agentId.getOrElse(unavailableReportValue)
    )

  private def currentReturnStatus(
    pollResponse: ChrisPollResponse
  ): String =
    Option(pollResponse.status)
      .map(_.toString)
      .getOrElse(unavailableReportValue)

  private def reportValue(
    value: String
  ): String =
    Option(value)
      .filter(_.nonEmpty)
      .getOrElse(unavailableReportValue)

  private def logPollDurationIfRequired(
    startTime: Long,
    submissionRequestDate: Option[LocalDateTime],
    submissionStatus: SubmissionStatus,
    submissionId: String
  ): Unit =
    submissionRequestDate match {
      case Some(requestDate) =>
        val submissionTime =
          requestDate
            .atZone(ukTimezone)
            .toInstant
            .toEpochMilli

        if (startTime > submissionTime + alertDuration) {
          logger.warn(
            s"Submission in status $submissionStatus has been polling for more than " +
              s"${alertDuration / 1.hour.toMillis} hours: $submissionId"
          )
        }
      case None              =>
        logger.warn(
          s"Unable to check polling duration because submissionRequestDate is missing: $submissionId"
        )
    }

  private def sendEmailIfRequired(
    status: SubmissionStatus,
    emailRecipient: Option[String],
    taxMonth: Int,
    taxYear: Int,
    submissionId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    status match {
      case SUBMITTED | SUBMITTED_NO_RECEIPT | DEPARTMENTAL_ERROR =>
        emailRecipient match {
          case Some(email) =>
            submissionService.sendSuccessfulEmail(
              submissionId,
              SendSuccessEmailRequest(
                email,
                taxMonth.toString,
                taxYear.toString
              )
            )

          case None =>
            logger.warn(
              s"[MonthlyReturnPollingProcessService][sendEmailIfRequired] " +
                s"No emailRecipient for submissionId=$submissionId, skipping email"
            )

            Future.unit
        }

      case _ =>
        Future.unit
    }
}
