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

package models

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.*

class ClientListStatusSpec extends SpecBase {

  "ClientListStatus" - {

    "contain exactly the four expected states" in {
      val all: Set[ClientListStatus] =
        Set(InitiateDownload, InProgress, Succeeded, Failed)

      all.size mustBe 4
      all must contain (InitiateDownload)
      all must contain (InProgress)
      all must contain (Succeeded)
      all must contain (Failed)
    }

    "support pattern matching over all cases" in {
      def describe(status: ClientListStatus): String =
        status match {
          case InitiateDownload => "initiate"
          case InProgress       => "in-progress"
          case Succeeded        => "succeeded"
          case Failed           => "failed"
        }

      describe(InitiateDownload) mustBe "initiate"
      describe(InProgress)       mustBe "in-progress"
      describe(Succeeded)        mustBe "succeeded"
      describe(Failed)           mustBe "failed"
    }
  }
}
