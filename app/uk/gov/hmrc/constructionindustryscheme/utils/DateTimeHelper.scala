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

package uk.gov.hmrc.constructionindustryscheme.utils

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

object DateTimeHelper {
  def monthFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM").withLocale(locale)

  def yearFormatter(locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern("uuuu").withLocale(locale)

  def parseYearMonthFlexible(monthYear: String): YearMonth =
    Try(YearMonth.parse(monthYear))
      .orElse(Try(YearMonth.parse(monthYear.replace('/', '-'))))
      .getOrElse(throw new IllegalArgumentException(s"Invalid monthYear: $monthYear (expected YYYY-MM or YYYY/MM)"))

}
