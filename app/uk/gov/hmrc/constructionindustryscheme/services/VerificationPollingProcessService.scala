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
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.Verification
import uk.gov.hmrc.constructionindustryscheme.models.{BatchChRISPollResult, PollReportContent}
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, VerificationSubmissionToPoll}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class VerificationPollingProcessService @Inject() (
  submissionService: SubmissionService
)(implicit ec: ExecutionContext)
    extends Logging {

  private val unavailableReportValue =
    "-"

  private val notPolledCorrelationId =
    "(not polled)"

  def process(
    verificationSubmissions: Seq[VerificationSubmissionToPoll]
  )(implicit hc: HeaderCarrier): Future[Seq[PollReportContent]] = {

    logger.info(
      s"[VerificationPollingProcessService][process] Calling F6 - Verification Polling Process for ${verificationSubmissions.size} submissions"
    )

    Future
      .traverse(verificationSubmissions) { submission =>
        val submissionId = submission.submissionId.toString

        (for {
          session <- submissionService.syncVerificationSessionForPolling(submission)

          batchPollResult <- submissionService.pollSubmissionAndUpdateGovTalkStatusForBatch(
                               submissionId = submissionId,
                               pollUrl = session.pollUrl,
                               journey = Verification
                             )

          reportContent =
            batchPollResult match {
              case BatchChRISPollResult.Completed(response) =>
                toPollReportContent(submission, response)

              case BatchChRISPollResult.PostProcessingFailed(response, exception) =>
                logger.error(
                  s"[VerificationPollingProcessService][process] Post-poll processing failed for verification submissionId=$submissionId, but returning poll report content.",
                  exception
                )

                toPollReportContent(submission, response)
            }
        } yield reportContent).recover { case NonFatal(ex) =>
          logger.error(
            s"[VerificationPollingProcessService][process] Failed for verification submission: " +
              s"instanceId=${submission.instanceId}, submissionId=${submission.submissionId}",
            ex
          )

          toFailedReportContent(submission)
        }
      }
  }

  private def toPollReportContent(
    submission: VerificationSubmissionToPoll,
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
      agentId = submission.agentId.getOrElse(unavailableReportValue)
    )

  private def toFailedReportContent(
    submission: VerificationSubmissionToPoll
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
}
