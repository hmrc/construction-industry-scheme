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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationResult
import java.time.LocalDateTime

class VerificationResultSpec extends SpecBase {

  "VerificationResult" - {
    "create an instance with the expected values" in {
      val result = VerificationResult(
        resourceRef = 13L,
        matched = Some("Y"),
        verified = Some("N"),
        verificationNumber = Some("V1000000007"),
        taxTreatment = "net",
        verifiedDate = Some(LocalDateTime.parse("2017-04-06T08:46:08.081"))
      )

      result.resourceRef mustBe 13L
      result.matched mustBe Some("Y")
      result.verified mustBe Some("N")
      result.verificationNumber mustBe Some("V1000000007")
      result.taxTreatment mustBe "net"
    }
  }
}
