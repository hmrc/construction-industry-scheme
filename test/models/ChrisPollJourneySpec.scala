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

package models

import base.SpecBase
import org.mockito.Mockito.when
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.*

class ChrisPollJourneySpec extends SpecBase {

  private val mockAppConfig = mock[AppConfig]

  "ChrisPollJourney" - {

    "must define monthly return values" in {
      MonthlyReturn.logName mustBe "monthlyReturn"
      MonthlyReturn.govTalkClass mustBe "IR-CIS-CIS300MR"
    }

    "must define verification values" in {
      Verification.logName mustBe "verification"
      Verification.govTalkClass mustBe "IR-CIS-VERIFY"
    }

    "must return monthly return gateway URL" in {
      when(mockAppConfig.chrisGatewayUrl)
        .thenReturn("monthly-url")

      MonthlyReturn.gatewayUrl(mockAppConfig) mustBe "monthly-url"
    }

    "must return verification gateway URL" in {
      when(mockAppConfig.chrisVerificationGatewayUrl)
        .thenReturn("verification-url")

      Verification.gatewayUrl(mockAppConfig) mustBe "verification-url"
    }

  }
}
