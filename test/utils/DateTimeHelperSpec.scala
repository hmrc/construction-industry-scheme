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
