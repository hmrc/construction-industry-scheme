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

sealed trait ChrisPollJourney {
  def logName: String
  def govTalkClass: String
}

object ChrisPollJourney {
  case object MonthlyReturn extends ChrisPollJourney {
    override val logName: String = "monthlyReturn"
    override val govTalkClass: String = "IR-CIS-CIS300MR"
  }

  case object Verification extends ChrisPollJourney {
    override val logName: String = "verification"
    override val govTalkClass: String = "IR-CIS-VERIFY"
  }
}
