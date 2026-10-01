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
import uk.gov.hmrc.constructionindustryscheme.utils.AcceptedTimeParser

import java.time.Instant

class AcceptedTimeParserSpec extends AnyWordSpec with Matchers {

  "AcceptedTimeParser.parse" should {

    "parse yyyy-MM-dd'T'HH:mm:ss" in {
      AcceptedTimeParser.parse("2017-04-06T08:46:08") mustBe Some(Instant.parse("2017-04-06T08:46:08Z"))
    }

    "parse yyyy-MM-dd'T'HH:mm:ss with optional fractional seconds" in {
      AcceptedTimeParser.parse("2017-04-06T08:46:08.081") mustBe Some(Instant.parse("2017-04-06T08:46:08.081Z"))
      AcceptedTimeParser.parse("2017-04-06T08:46:08.0") mustBe Some(Instant.parse("2017-04-06T08:46:08.000Z"))
      AcceptedTimeParser.parse("2026-04-06T09:50:08.000") mustBe Some(Instant.parse("2026-04-06T09:50:08Z"))
    }

    "parse values with a timezone offset" in {
      AcceptedTimeParser.parse("2017-04-06T08:46:08.081Z") mustBe Some(Instant.parse("2017-04-06T08:46:08.081Z"))
      AcceptedTimeParser.parse("2026-04-20T21:49:19.702Z") mustBe Some(Instant.parse("2026-04-20T21:49:19.702Z"))
    }

    "return None for invalid values" in {
      AcceptedTimeParser.parse("not-a-datetime") mustBe None
      AcceptedTimeParser.parse("2017-04-06 08:46:08.081") mustBe None
      AcceptedTimeParser.parse("") mustBe None
      AcceptedTimeParser.parse("   ") mustBe None
    }
  }
}
