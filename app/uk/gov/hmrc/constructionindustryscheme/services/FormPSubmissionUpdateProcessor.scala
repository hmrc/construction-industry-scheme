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

import uk.gov.hmrc.constructionindustryscheme.models.{ChrisPollJourney, GovTalkError, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future

trait FormPSubmissionUpdateProcessor {
  def journey: ChrisPollJourney

  def handleInitialAccepted(
    session: ChrisSubmissionSessionData,
    response: SubmissionResult
  )(implicit hc: HeaderCarrier): Future[Unit]

  /** Called when the initial ChRIS submission fails outright (5xx or a transport failure such as a connection reset),
    * so the submission never reaches an accepted/poll state. Lets the processor persist the terminal FATAL_ERROR along
    * with the GovTalk error detail, mirroring what handlePollResponse does for a non-success poll.
    */
  def handleInitialFailure(
    session: ChrisSubmissionSessionData,
    govTalkError: GovTalkError
  )(implicit hc: HeaderCarrier): Future[Unit]

  def handlePollResponse(
    session: ChrisSubmissionSessionData,
    response: ChrisPollResponse
  )(implicit hc: HeaderCarrier): Future[Unit]
}
