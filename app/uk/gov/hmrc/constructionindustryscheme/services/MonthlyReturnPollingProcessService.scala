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

  private val ukTimezone: ZoneId = ZoneId.of("Europe/London")
  private val alertDuration      = 24.hours.toMillis

  def process(
    monthlyReturnSubmissions: Seq[MonthlyReturnSubmissionToPoll],
    startTime: Long
  )(implicit hc: HeaderCarrier): Future[Seq[PollReportContent]] =
    Future.traverse(monthlyReturnSubmissions) { sub =>
      processSubmission(sub, startTime)
        .recover { case NonFatal(ex) =>
          logger.error(
            s"[MonthlyReturnPollingProcessService][process] Failed for instanceId=${sub.instanceId}, submissionId=${sub.submissionId}",
            ex
          )
          toRecoverableErrorReportContent(sub)
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
      details      <- monthlyReturnService.getMonthlyReturnForEdit(
                        GetMonthlyReturnForEditRequest(
                          submission.instanceId,
                          submission.taxYear,
                          submission.taxMonth,
                          isAmendment = Some(false)
                        )
                      )
      monthlyReturn = details.monthlyReturn.headOption
                        .getOrElse(
                          throw new RuntimeException(
                            s"No monthly return found for instanceId=${submission.instanceId}"
                          )
                        )
      subDetails    = details.submission.headOption
                        .getOrElse(
                          throw new RuntimeException(
                            s"No submission found for instanceId=${submission.instanceId}"
                          )
                        )
      gatewayUrl   <- submissionService.processMonthlyReturnGovTalkStatusCheck(
                        submission.instanceId,
                        submission.submissionId.toString
                      )
      pollResponse <- submissionService.pollSubmissionAndUpdateGovTalkStatus(
                        submission.submissionId.toString,
                        gatewayUrl,
                        ChrisPollJourney.MonthlyReturn
                      )
      _             = logPollDurationIfRequired(
                        startTime = startTime,
                        submissionRequestDate = subDetails.submissionRequestDate,
                        submissionStatus = pollResponse.status,
                        submissionId = submission.submissionId.toString
                      )
      updateReq     = UpdateSubmissionRequest(
                        instanceId = submission.instanceId,
                        taxYear = monthlyReturn.taxYear,
                        taxMonth = monthlyReturn.taxMonth,
                        hmrcMarkGenerated = subDetails.hmrcMarkGenerated,
                        submittableStatus = pollResponse.status.toString,
                        amendment = monthlyReturn.amendment.getOrElse("N"),
                        hmrcMarkGgis = pollResponse.irMarkReceived,
                        submissionRequestDate = subDetails.submissionRequestDate,
                        acceptedTime = pollResponse.acceptedTime,
                        emailRecipient = subDetails.emailRecipient,
                        agentId = subDetails.agentId,
                        govTalkResponse = pollResponse.govTalkErrorStatus
                      )
      _            <- submissionService.updateSubmission(updateReq)
      _            <- sendEmailIfRequired(
                        pollResponse.status,
                        subDetails.emailRecipient,
                        monthlyReturn.taxMonth,
                        monthlyReturn.taxYear,
                        submission.submissionId.toString
                      )
    } yield toPollReportContent(
      submission = submission,
      dbSubmission = subDetails,
      pollResponse = pollResponse
    )
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
      govTalkRequestStatus = submission.status,
      currentReturnStatus = pollResponse.status.toString,
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = pollResponse.correlationId,
      agentId = dbSubmission.agentId
        .orElse(submission.agentId)
        .getOrElse("")
    )

  private def toRecoverableErrorReportContent(
    submission: MonthlyReturnSubmissionToPoll
  ): PollReportContent =
    PollReportContent.forRecoverableError(
      user = submission.instanceId,
      submissionType = submission.submissionType,
      submissionId = submission.submissionId.toString,
      govTalkRequestStatus = submission.status,
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = "",
      agentId = submission.agentId.getOrElse("")
    )

  private def logPollDurationIfRequired(
    startTime: Long,
    submissionRequestDate: Option[LocalDateTime],
    submissionStatus: SubmissionStatus,
    submissionId: String
  ): Unit =
    submissionRequestDate match {
      case Some(requestDate) =>
        val submissionTime = requestDate.atZone(ukTimezone).toInstant.toEpochMilli
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
              SendSuccessEmailRequest(email, taxMonth.toString, taxYear.toString)
            )
          case None        =>
            logger.warn(
              s"[MonthlyReturnPollingProcessService][sendEmailIfRequired] No emailRecipient for submissionId=$submissionId, skipping email"
            )
            Future.unit
        }
      case _                                                     =>
        Future.unit
    }
}
