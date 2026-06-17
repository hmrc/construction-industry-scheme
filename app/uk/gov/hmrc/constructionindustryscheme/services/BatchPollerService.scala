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

@Singleton
class BatchPollerService @Inject() (
  submissionService: SubmissionService,
  monthlyReturnPollingProcessService: MonthlyReturnPollingProcessService
)(implicit ec: ExecutionContext)
    extends Logging {

  def run()(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info("[BatchPollerService][run] Calling F1 - Get Submissions To Poll")
    submissionService
      .getSubmissionsToPoll()
      .flatMap { submissions =>
        logger.info(
          s"[BatchPollerService][run] GetBatchPollSubmissions returned " +
            s"verificationSubmissions=${submissions.verificationSubmissions.size}, " +
            s"monthlyReturnSubmissions=${submissions.monthlyReturnSubmissions.size}"
        )

        // TODO:
        // Future tickets:
        // - If both lists are empty, call F8 - Generate Poll Report
        // - If verificationSubmissions is non-empty, call F6 - Verification Polling Process
        if (submissions.monthlyReturnSubmissions.nonEmpty) {
          monthlyReturnPollingProcessService.process(
            submissions.monthlyReturnSubmissions
          )
        } else {
          Future.unit
        }
      }
      .recover { case exception =>
        logger.error(
          "[BatchPollerService][run] GetBatchPollSubmissions failed",
          exception
        )
      }
  }
}
