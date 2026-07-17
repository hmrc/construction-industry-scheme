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
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class BatchPollerService @Inject() (
  submissionService: SubmissionService,
  verificationPollingProcessService: VerificationPollingProcessService,
  monthlyReturnPollingProcessService: MonthlyReturnPollingProcessService,
  generatePollReportService: GeneratePollReportService
)(implicit ec: ExecutionContext)
    extends Logging {

  def run()(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info("[BatchPollerService][run] Calling F1 - Get Submissions To Poll")
    submissionService
      .getSubmissionsToPoll()
      .flatMap { submissions =>
        val verificationSubmissions  = submissions.verificationSubmissions
        val monthlyReturnSubmissions = submissions.monthlyReturnSubmissions

        logger.info(
          s"[BatchPollerService][run] GetBatchPollSubmissions returned " +
            s"verificationSubmissions=${verificationSubmissions.size}, " +
            s"monthlyReturnSubmissions=${monthlyReturnSubmissions.size}"
        )

        if (verificationSubmissions.isEmpty && monthlyReturnSubmissions.isEmpty) {
          generatePollReportService.generatePollReport()
        } else {
          val processes: Seq[Future[Unit]] = Seq(
            Option.when(verificationSubmissions.nonEmpty) {
              runPollingProcess("Verification Polling Process") {
                verificationPollingProcessService.process(verificationSubmissions)
              }
            },
            Option.when(monthlyReturnSubmissions.nonEmpty) {
              runPollingProcess("Monthly Return Polling Process") {
                monthlyReturnPollingProcessService.process(monthlyReturnSubmissions)
              }
            }
          ).flatten

          Future.sequence(processes).map(_ => ())
        }
      }
      .recover { case NonFatal(exception) =>
        logger.error(
          "[BatchPollerService][run] Failed to get submission to poll",
          exception
        )
      }
  }

  private def runPollingProcess(processName: String)(process: Future[Unit]): Future[Unit] =
    process.recover { case NonFatal(exception) =>
      logger.error(s"[BatchPollerService][run] $processName failed", exception)
    }
}
