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

import uk.gov.hmrc.constructionindustryscheme.models.GovTalkErrorStatus.*
import uk.gov.hmrc.constructionindustryscheme.models.*

object GovTalkErrorStatusClassifier {

  private val RecoverableCodes = Set("3000", "2005", "1000")
  private val ServerErrorRange = 500 to 505

  def fromXmlOutcome(status: SubmissionStatus, error: Option[GovTalkError]): GovTalkErrorStatus =
    (status, error.map(_.errorNumber)) match {
      case (DEPARTMENTAL_ERROR, _)                                                =>
        DepartmentalError(error.map(_.errorText).getOrElse(""))
      case (STARTED | FATAL_ERROR, Some(code)) if RecoverableCodes.contains(code) =>
        error.map(e => RecoverableError(e.errorNumber, e.errorText)).getOrElse(OtherStatus)
      case (FATAL_ERROR, Some(_))                                                 =>
        error.map(e => FatalError(e.errorNumber, e.errorText)).getOrElse(OtherStatus)
      case (FATAL_ERROR, None)                                                    =>
        OtherStatus
      case _                                                                      =>
        OtherStatus
    }

  def fromHttpStatus(status: Int): GovTalkErrorStatus =
    if (ServerErrorRange.contains(status)) ServerError(status)
    else OtherStatus

  val noResponse: GovTalkErrorStatus = NoResponse
}
