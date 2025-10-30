/*
 * Copyright 2025 HM Revenue & Customs
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

package connectors

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.connectors.ChrisConnector

final class ChrisConnectorSpec extends AnyFreeSpec with Matchers {

  "pickUrl" - {
    "returns base when switch is disabled" in {
      ChrisConnector.pickUrl("http://base", enabled = false, None) mustBe "http://base"
      ChrisConnector.pickUrl("http://base", enabled = false, Some("http://override")) mustBe "http://base"
    }

    "returns base when enabled but no override present" in {
      ChrisConnector.pickUrl("http://base", enabled = true, None) mustBe "http://base"
    }

    "returns base when enabled but override is empty" in {
      ChrisConnector.pickUrl("http://base", enabled = true, Some("")) mustBe "http://base"
    }

    "returns override when enabled and non-empty override provided" in {
      ChrisConnector.pickUrl("http://base", enabled = true, Some("http://override")) mustBe "http://override"
    }
  }
}
