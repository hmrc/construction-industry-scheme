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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class MonthlyReturnPollingProcessService @Inject() (
  monthlyReturnService: MonthlyReturnService,
  submissionService: SubmissionService
)(implicit ec: ExecutionContext)
    extends Logging {

  def process(
    monthlyReturnSubmissions: Seq[MonthlyReturnSubmissionToPoll]
  )(implicit hc: HeaderCarrier): Future[Seq[PollReportContent]] =
    Future.traverse(monthlyReturnSubmissions) { submission =>
      processSubmission(submission).recover { case NonFatal(exception) =>
        logger.error(
          s"[MonthlyReturnPollingProcessService][process] Failed for " +
            s"instanceId=${submission.instanceId}, " +
            s"submissionId=${submission.submissionId}",
          exception
        )

        toRecoverableErrorReportContent(submission)
      }
    }

  private def processSubmission(
    submission: MonthlyReturnSubmissionToPoll
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

      monthlyReturn = details.monthlyReturn.headOption.getOrElse(
                        throw new RuntimeException(
                          s"No monthly return found for instanceId=${submission.instanceId}"
                        )
                      )

      dbSubmission = details.submission.headOption
                       .getOrElse(
                         throw new RuntimeException(s"No submission found for instanceId=${submission.instanceId}")
                       )

      gatewayUrl <- submissionService.processMonthlyReturnGovTalkStatusCheck(
                      submission.instanceId,
                      submission.submissionId.toString
                    )

      pollResponse <- submissionService.pollSubmissionAndUpdateGovTalkStatus(
                        submission.submissionId.toString,
                        gatewayUrl,
                        ChrisPollJourney.MonthlyReturn
                      )

      updateReq = UpdateSubmissionRequest(
                    instanceId = submission.instanceId,
                    taxYear = monthlyReturn.taxYear,
                    taxMonth = monthlyReturn.taxMonth,
                    hmrcMarkGenerated = dbSubmission.hmrcMarkGenerated,
                    submittableStatus = pollResponse.status.toString,
                    amendment = monthlyReturn.amendment.getOrElse("N"),
                    hmrcMarkGgis = pollResponse.irMarkReceived,
                    submissionRequestDate = dbSubmission.submissionRequestDate,
                    acceptedTime = pollResponse.acceptedTime,
                    emailRecipient = dbSubmission.emailRecipient,
                    agentId = dbSubmission.agentId,
                    govTalkResponse = pollResponse.govTalkErrorStatus
                  )

      _ <- submissionService.updateSubmission(updateReq)

      _ <- sendEmailIfRequired(
             pollResponse.status,
             dbSubmission.emailRecipient,
             monthlyReturn.taxMonth,
             monthlyReturn.taxYear,
             submission.submissionId.toString
           )
    } yield toPollReportContent(
      submission = submission,
      dbSubmission = dbSubmission,
      pollResponse = pollResponse
    )
  }

  private def toPollReportContent(
    submission: MonthlyReturnSubmissionToPoll,
    dbSubmission: Submission,
    pollResponse: ChrisPollResponse
  ): PollReportContent =
    PollReportContent(
      user = "",
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
      user = "",
      submissionType = submission.submissionType,
      submissionId = submission.submissionId.toString,
      govTalkRequestStatus = submission.status,
      employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
      correlationId = "",
      agentId = submission.agentId.getOrElse("")
    )

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
