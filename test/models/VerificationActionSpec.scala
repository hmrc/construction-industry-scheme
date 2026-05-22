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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.constructionindustryscheme.models.*

class VerificationActionSpec extends AnyWordSpec with Matchers {

  "VerificationAction.toXmlValue" should {

    "return 'verify' for Verify" in {
      val result = VerificationAction.toXmlValue(Verify)

      result mustBe "verify"
    }

    "return 'match' for Match" in {
      val result = VerificationAction.toXmlValue(Match)

      result mustBe "match"
    }
  }

  "VerificationAction" should {

    "support pattern matching correctly" in {
      val action: VerificationAction = Verify

      val result = action match {
        case Verify => "ok"
        case Match  => "not-ok"
      }

      result mustBe "ok"
    }

    "be usable in equality checks" in {
      Verify mustBe Verify
      Match mustBe Match
      Verify must not be Match
    }
  }
}
