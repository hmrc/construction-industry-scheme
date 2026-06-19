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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.constructionindustryscheme.models.{ChrisPollJourney, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData

@Singleton
class MonthlyReturnFormPUpdateProcessor @Inject() extends FormPSubmissionUpdateProcessor {

  override val journey: ChrisPollJourney = ChrisPollJourney.MonthlyReturn

  // for now, the submission update remains FE-owned, so this is a no-op. Later FE SP call can be moved here
  override def handleInitialAccepted(
    session: ChrisSubmissionSessionData,
    response: SubmissionResult
  )(implicit hc: HeaderCarrier): Future[Unit] =
    Future.unit

  override def handlePollResponse(
    session: ChrisSubmissionSessionData,
    response: ChrisPollResponse
  )(implicit hc: HeaderCarrier): Future[Unit] =
    Future.unit
}
