/*
 * Copyright 2025 HM Revenue & Customs
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

object ChrisSubmissionXmlMapper extends ChrisXmlMapper {

  def parse(xml: String): Either[String, SubmissionResult] =
    parseSubmission(xml)(deriveInitialStatus)

  /** Stage 1 (initial submit) status mapping – ACK or FATAL only. */
  private def deriveInitialStatus(qualifier: String, errOpt: Option[GovTalkError]): SubmissionStatus =
    qualifier.toLowerCase match {
      case "acknowledgement" => ACCEPTED
      case "error"           =>
        errOpt match {
          case Some(err)
              if err.errorNumber == "3000" &&
                err.errorType.equalsIgnoreCase("fatal") =>
            FATAL_ERROR

          case Some(err) if Set("3000", "2005", "1000").contains(err.errorNumber) =>
            STARTED

          case _ =>
            FATAL_ERROR
        }
      case _                 => FATAL_ERROR
    }
}
