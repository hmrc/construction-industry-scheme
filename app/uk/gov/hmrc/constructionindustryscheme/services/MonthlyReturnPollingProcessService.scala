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
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetMonthlyReturnForEditResponse, MonthlyReturnSubmissionToPoll}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MonthlyReturnPollingProcessService @Inject() (
  monthlyReturnService: MonthlyReturnService,
  submissionService: SubmissionService
)(implicit
  ec: ExecutionContext
) extends Logging {

  def process(
    monthlyReturnSubmissions: Seq[MonthlyReturnSubmissionToPoll]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    Future
      .traverse(monthlyReturnSubmissions) { sub =>
        processSubmission(sub).recover { case ex =>
          logger.error(
            s"[MonthlyReturnPollingProcessService][process] Failed for instanceId=${sub.instanceId}, submissionId=${sub.submissionId}",
            ex
          )
        }
      }
      .map(_ => ())

  private def processSubmission(
    submission: MonthlyReturnSubmissionToPoll
  )(implicit hc: HeaderCarrier): Future[Unit] =
    logger.info(
      s"[MonthlyReturnPollingProcessService][processSubmission] " +
        s"instanceId=${submission.instanceId}, " +
        s"submissionId=${submission.submissionId}"
    )
    for {
      details      <- monthlyReturnService.getMonthlyReturnForEdit(
                        GetMonthlyReturnForEditRequest(
                          submission.instanceId,
                          submission.taxYear.toInt,
                          submission.taxMonth.toInt,
                          isAmendment = Some(submission.amendment == "Y")
                        )
                      )
      monthlyReturn = details.monthlyReturn.headOption
                        .getOrElse(
                          throw new RuntimeException(
                            s"No monthly return found for instanceId=${submission.instanceId}"
                          )
                        )
      sub           = details.submission.headOption
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
      updateReq     = UpdateSubmissionRequest(
                        instanceId = submission.instanceId,
                        taxYear = monthlyReturn.taxYear,
                        taxMonth = monthlyReturn.taxMonth,
                        hmrcMarkGenerated = sub.hmrcMarkGenerated,
                        submittableStatus = pollResponse.status.toString,
                        amendment = monthlyReturn.amendment.getOrElse("N"),
                        hmrcMarkGgis = pollResponse.irMarkReceived,
                        submissionRequestDate = sub.submissionRequestDate,
                        acceptedTime = pollResponse.acceptedTime,
                        emailRecipient = sub.emailRecipient,
                        agentId = sub.agentId,
                        govTalkResponse = pollResponse.govTalkErrorStatus
                      )
      _            <- submissionService.updateSubmission(updateReq)
      _            <- sendEmailIfRequired(
                        pollResponse.status,
                        sub.emailRecipient,
                        monthlyReturn.taxMonth,
                        monthlyReturn.taxYear,
                        submission.submissionId.toString
                      )
    } yield ()

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
