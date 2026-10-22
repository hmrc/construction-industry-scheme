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
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.utils.Normalise

class NormaliseSpec extends AnyWordSpec with Matchers {

  "Normalise.nonBlank" should {
    "return trimmed value when non-empty" in {
      Normalise.nonBlank(Some("  abc  ")) mustBe Some("abc")
    }

    "return None for empty or whitespace strings" in {
      Normalise.nonBlank(Some("")) mustBe None
      Normalise.nonBlank(Some("   ")) mustBe None
    }

    "return None when input is None" in {
      Normalise.nonBlank(None) mustBe None
    }
  }

  "Normalise.isBlank" should {
    "return true for None or blank values" in {
      Normalise.isBlank(None) mustBe true
      Normalise.isBlank(Some("")) mustBe true
      Normalise.isBlank(Some("   ")) mustBe true
    }

    "return false for non-blank values" in {
      Normalise.isBlank(Some("abc")) mustBe false
    }
  }

  "Normalise.isYes" should {
    "return true for yes in any case with spaces" in {
      Normalise.isYes("yes") mustBe true
      Normalise.isYes("YES") mustBe true
      Normalise.isYes("  Yes  ") mustBe true
    }

    "return false for non-yes values or null" in {
      Normalise.isYes("no") mustBe false
      Normalise.isYes("y") mustBe false
      Normalise.isYes(null) mustBe false
    }
  }

  "Normalise.normaliseCompact" should {
    "remove all whitespace and uppercase" in {
      Normalise.normaliseCompact("  v12 34\t567890a ") mustBe "V1234567890A"
    }

    "leave an already compact uppercase value unchanged" in {
      Normalise.normaliseCompact("AA123456A") mustBe "AA123456A"
    }

    "return an empty string for whitespace only" in {
      Normalise.normaliseCompact("   ") mustBe ""
    }
  }

  "Normalise.normaliseNino" should {
    "remove whitespace and uppercase a 9 character NINO" in {
      Normalise.normaliseNino(" aa 12 34 56 a ") mustBe "AA123456A"
    }

    "leave an already normalised NINO unchanged" in {
      Normalise.normaliseNino("AA123456A") mustBe "AA123456A"
    }

    "pad an 8 character NINO with a trailing space" in {
      Normalise.normaliseNino("aa123456") mustBe "AA123456 "
    }

    "not pad again when the trailing space was already present" in {
      Normalise.normaliseNino("AA123456 ") mustBe "AA123456 "
    }

    "not pad a value that is not 8 characters once compacted" in {
      Normalise.normaliseNino("AA12345") mustBe "AA12345"
      Normalise.normaliseNino("AA1234567A") mustBe "AA1234567A"
    }
  }
}
