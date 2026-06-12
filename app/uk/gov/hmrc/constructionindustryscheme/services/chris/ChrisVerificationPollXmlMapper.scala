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

import java.time.Instant
import scala.xml.*

object ChrisVerificationPollXmlMapper extends ChrisXmlMapper {

  def parse(xml: String, now: Instant = Instant.now()): Either[String, ChrisPollResponse] =
    parsePoll(xml, now)(derivePollStatus)

  /** Verification poll status mapping per F18. */
  private def derivePollStatus(
    qualifier: String,
    errOpt: Option[GovTalkError],
    doc: Elem
  ): SubmissionStatus =
    qualifier.toLowerCase match {
      case "acknowledgement" => ACCEPTED
      case "response"        => SUBMITTED
      case "error"           =>
        if (isIrmarkMismatch(doc)) {
          SUBMITTED_NO_RECEIPT
        } else {
          errOpt match {
            case Some(err) if err.errorNumber == "3001" && err.errorType.equalsIgnoreCase("business") =>
              DEPARTMENTAL_ERROR

            case Some(err) if err.errorNumber == "3000" && err.errorType.equalsIgnoreCase("fatal") =>
              DEPARTMENTAL_ERROR

            case _ => FATAL_ERROR
          }
        }
      case _                 => FATAL_ERROR
    }
}
