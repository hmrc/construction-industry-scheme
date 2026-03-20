package uk.gov.hmrc.constructionindustryscheme.utils

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

object DateTimeHelper {
  val monthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
  val yearFormatter: DateTimeFormatter  = DateTimeFormatter.ofPattern("uuuu", Locale.UK)

  def parseYearMonthFlexible(monthYear: String): YearMonth =
    Try(YearMonth.parse(monthYear))
      .orElse(Try(YearMonth.parse(monthYear.replace('/', '-'))))
      .getOrElse(throw new IllegalArgumentException(s"Invalid monthYear: $monthYear (expected YYYY-MM or YYYY/MM)"))

}
