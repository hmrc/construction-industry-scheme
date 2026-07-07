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
import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class BatchPollerService @Inject() (
  submissionService: SubmissionService,
  generatePollReportService: GeneratePollReportService,
  monthlyReturnPollingProcessService: MonthlyReturnPollingProcessService
)(implicit ec: ExecutionContext)
    extends Logging {

  def run()(implicit hc: HeaderCarrier): Future[Unit] = {

    logger.info(
      "[BatchPollerService][run] Calling F1 - Get Submissions To Poll"
    )

    submissionService
      .getSubmissionsToPoll()
      .flatMap { submissions =>
        logger.info(
          s"[BatchPollerService][run] GetBatchPollSubmissions returned " +
            s"verificationSubmissions=${submissions.verificationSubmissions.size}, " +
            s"monthlyReturnSubmissions=${submissions.monthlyReturnSubmissions.size}"
        )

        if (
          submissions.verificationSubmissions.isEmpty &&
          submissions.monthlyReturnSubmissions.isEmpty
        ) {
          generatePollReportService.generatePollReport(
            Seq.empty[PollReportContent]
          )
        } else if (submissions.monthlyReturnSubmissions.nonEmpty) {
          monthlyReturnPollingProcessService.process(
            submissions.monthlyReturnSubmissions
          )
        } else {
          /*
           * F2 and F6 will return PollReportContent rows.
           * After both complete, invoke:
           *
           * generatePollReportService.generatePollReport(
           *   verificationRows ++ monthlyReturnRows
           * )
           */
          Future.unit
        }
      }
      .recover { case NonFatal(exception) =>
        logger.error(
          "[BatchPollerService][run] GetBatchPollSubmissions failed",
          exception
        )
      }
  }
}
