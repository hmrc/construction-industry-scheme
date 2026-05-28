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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.constructionindustryscheme.utils.DateUtils

import java.time.LocalDate

class DateUtilsSpec extends AnyWordSpec with Matchers {

  "DateUtils.calculateCutOffDate" should {

    "store the current date in yyyy-MM-dd format (AC1)" in {
      val today  = LocalDate.of(2026, 5, 11)
      val result = DateUtils.calculateCutOffDate(today)
      result.toString mustBe "2025-05-11"
    }

    "subtract 1 year from the current date and return it as the cut-off date (AC2)" in {
      val today  = LocalDate.of(2026, 5, 11)
      val result = DateUtils.calculateCutOffDate(today)
      result mustBe today.minusYears(1)
    }

    "handle a leap year input of 29 February by returning 28 February the prior year" in {
      val today  = LocalDate.of(2024, 2, 29)
      val result = DateUtils.calculateCutOffDate(today)
      result mustBe LocalDate.of(2023, 2, 28)
    }

    "return the date one year before for the first day of the year" in {
      val today  = LocalDate.of(2026, 1, 1)
      val result = DateUtils.calculateCutOffDate(today)
      result mustBe LocalDate.of(2025, 1, 1)
    }

    "return the date one year before for the last day of the year" in {
      val today  = LocalDate.of(2026, 12, 31)
      val result = DateUtils.calculateCutOffDate(today)
      result mustBe LocalDate.of(2025, 12, 31)
    }
  }
}
