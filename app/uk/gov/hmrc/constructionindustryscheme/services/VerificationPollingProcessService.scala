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
import uk.gov.hmrc.constructionindustryscheme.models.response.VerificationSubmissionToPoll
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class VerificationPollingProcessService @Inject() (
  submissionService: SubmissionService
)(implicit ec: ExecutionContext)
    extends Logging {

  def process(
    verificationSubmissions: Seq[VerificationSubmissionToPoll]
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    logger.info(
      s"[VerificationPollingProcessService][process] Calling F6 - Verification Polling Process for ${verificationSubmissions.size} submissions"
    )
    logger.info(
      s"[VerificationPollingProcessService][process] Calling F7 - Get Submission With Verification Batch for ${verificationSubmissions.size} submissions"
    )

    Future
      .traverse(verificationSubmissions) { submission =>
        submissionService
          .getSubmissionWithVerificationBatch(submission)
          .map(_ => ())
      }
      .map(_ => ())
  }
}
