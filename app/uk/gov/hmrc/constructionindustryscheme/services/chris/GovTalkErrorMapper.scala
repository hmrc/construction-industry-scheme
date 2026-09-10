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

import uk.gov.hmrc.constructionindustryscheme.models.GovTalkError

object GovTalkErrorMapper {

  def mapVerificationPoll(error: GovTalkError): GovTalkError =
    if (error.errorNumber == "3001" || error.errorType == "business") {
      GovTalkError(error.errorNumber, "departmentalError", error.errorText, error.raisedBy)
    } else if (error.errorType == "fatal") {
      GovTalkError(error.errorNumber, "systemError", error.errorText, error.raisedBy)
    } else if (error.raisedBy.contains("Department")) {
      GovTalkError(error.errorNumber, "departmentalError", error.errorText, error.raisedBy)
    } else {
      GovTalkError(error.errorNumber, "systemError", error.errorText, error.raisedBy)
    }

  def map(error: GovTalkError): GovTalkError =
    (error.errorNumber, error.errorType) match {
      case ("3001", _) =>
        GovTalkError("3001", "departmentalError", error.errorText)

      case (_, "business") =>
        GovTalkError(error.errorNumber, "departmentalError", error.errorText)

      case (_, "fatal") =>
        GovTalkError(error.errorNumber, "systemError", error.errorText)

      case (_, _) if error.raisedBy.contains("Department") =>
        GovTalkError(error.errorNumber, "departmentalError", error.errorText)

      case _ =>
        GovTalkError(error.errorNumber, "systemError", error.errorText)
    }

  def fromHttpTimeout(status: Int = 500): GovTalkError =
    GovTalkError(status.toString, "timeOut", "timeOut")

  def fromInitialConnectionRefused(): GovTalkError =
    GovTalkError("xxxx", "timeOut", "timed out")

  def fromPollConnectionRefused(): GovTalkError =
    GovTalkError("xxxx", "timeOut", "timeOut")
}
