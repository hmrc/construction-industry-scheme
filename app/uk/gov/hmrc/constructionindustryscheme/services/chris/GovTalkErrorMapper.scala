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

  def map(error: GovTalkError): GovTalkError =
    (error.errorNumber, error.errorType.toLowerCase) match {
      case ("3001", "business") =>
        GovTalkError("3001", "departmentalError", error.errorText)

      case ("3000", "fatal") =>
        GovTalkError("3001", "departmentalError", error.errorText)

      case _ =>
        GovTalkError(error.errorNumber, "systemError", error.errorText)
    }

  def fromHttpTimeout(statusCode: Int): GovTalkError =
    GovTalkError("500", "timeOut", "timeOut")

  lazy val fromConnectionRefused: GovTalkError =
    GovTalkError("500", "timeOut", "timed out")
}
