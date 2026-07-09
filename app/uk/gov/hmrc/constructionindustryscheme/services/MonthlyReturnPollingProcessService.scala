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
import uk.gov.hmrc.constructionindustryscheme.models.requests.GetMonthlyReturnForEditRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.MonthlyReturnSubmissionToPoll
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
  )(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info(
      s"[MonthlyReturnPollingProcessService][processSubmission] " +
        s"instanceId=${submission.instanceId}, " +
        s"submissionId=${submission.submissionId}"
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

      // TODO: At the moment above call is not used. But We will require getMonthlyReturnForEdit in DTR-5744
      _       <- submissionService.processMonthlyReturnGovTalkStatusCheck(
                   submission.instanceId,
                   submission.submissionId.toString
                 )
    } yield ()
  }
}
