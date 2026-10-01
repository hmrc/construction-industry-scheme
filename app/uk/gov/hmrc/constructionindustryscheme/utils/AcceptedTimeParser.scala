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

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}
import scala.util.Try

object AcceptedTimeParser {

  def parse(value: String): Option[Instant] =
    Option(value).map(_.trim).filter(_.nonEmpty).flatMap { trimmed =>
      Try(LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC))
        .orElse(Try(OffsetDateTime.parse(trimmed).toInstant))
        .orElse(Try(Instant.parse(trimmed)))
        .toOption
    }
}
