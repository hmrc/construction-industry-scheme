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

package utils

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.constructionindustryscheme.utils.DateTimeHelper

import java.time.YearMonth

class DateTimeHelperSpec extends AnyWordSpec with Matchers {

  "parseYearMonthFlexible" should {

    "parse YYYY-MM format correctly" in {
      val result = DateTimeHelper.parseYearMonthFlexible("2026-03")

      result shouldBe YearMonth.of(2026, 3)
    }

    "parse YYYY/MM format correctly" in {
      val result = DateTimeHelper.parseYearMonthFlexible("2026/03")

      result shouldBe YearMonth.of(2026, 3)
    }

    "reject single digit month format" in {
      val ex = intercept[IllegalArgumentException] {
        DateTimeHelper.parseYearMonthFlexible("2026-3")
      }

      ex.getMessage should include("Invalid monthYear")
    }

    "throw IllegalArgumentException for invalid format" in {
      val ex = intercept[IllegalArgumentException] {
        DateTimeHelper.parseYearMonthFlexible("03-2026")
      }

      ex.getMessage should include("Invalid monthYear")
    }

    "throw IllegalArgumentException for completely invalid input" in {
      val ex = intercept[IllegalArgumentException] {
        DateTimeHelper.parseYearMonthFlexible("invalid")
      }

      ex.getMessage should include("Invalid monthYear")
    }

    "throw IllegalArgumentException for empty string" in {
      val ex = intercept[IllegalArgumentException] {
        DateTimeHelper.parseYearMonthFlexible("")
      }

      ex.getMessage should include("Invalid monthYear")
    }
  }
}
