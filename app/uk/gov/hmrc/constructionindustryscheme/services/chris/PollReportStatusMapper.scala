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

package uk.gov.hmrc.constructionindustryscheme.services.chris

import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse

/** Maps a ChRIS poll response onto the two status values the F8/F9 flow needs.
  *
  * These agree for every outcome except a recoverable error (3000, 2005, 1000), where the submission table must stay
  * STARTED so the batch poller retries, while the poll report must show FATAL_ERROR.
  */
object PollReportStatusMapper {

  val unavailable: String = "-"

  /** Status written to the submission table — drives retry behaviour. */
  def submissionTableStatus(pollResponse: ChrisPollResponse): String =
    Option(pollResponse.status)
      .map(_.toString)
      .getOrElse(unavailable)

  /** Status shown in the monthly poll report. */
  def reportStatus(pollResponse: ChrisPollResponse): String =
    pollResponse.govTalkErrorStatus match {
      case Some(_: GovTalkErrorStatus.RecoverableError) =>
        FATAL_ERROR.toString

      case _ if pollResponse.status == STARTED =>
        FATAL_ERROR.toString

      case _ =>
        submissionTableStatus(pollResponse)
    }
}