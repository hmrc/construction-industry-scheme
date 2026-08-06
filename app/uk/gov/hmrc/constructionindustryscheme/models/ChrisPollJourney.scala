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

package uk.gov.hmrc.constructionindustryscheme.models

import uk.gov.hmrc.constructionindustryscheme.config.AppConfig

sealed trait ChrisPollJourney {
  def logName: String
  def govTalkClass: String

  def gatewayUrl(appConfig: AppConfig): String
}

object ChrisPollJourney {

  case object MonthlyReturn extends ChrisPollJourney {
    override val logName      = "monthlyReturn"
    override val govTalkClass = "IR-CIS-CIS300MR"

    override def gatewayUrl(appConfig: AppConfig): String =
      appConfig.chrisGatewayUrl
  }

  case object Verification extends ChrisPollJourney {
    override val logName      = "verification"
    override val govTalkClass = "IR-CIS-VERIFY"

    override def gatewayUrl(appConfig: AppConfig): String =
      appConfig.chrisVerificationGatewayUrl
  }
}
