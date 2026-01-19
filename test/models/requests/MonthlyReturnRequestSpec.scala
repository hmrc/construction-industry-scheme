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

package models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.MonthlyReturnRequest

class MonthlyReturnRequestSpec extends AnyFreeSpec with Matchers {

  "MonthlyReturnRequest JSON format" - {

    "read from JSON correctly" in {
      val json = Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )

      val result = json.as[MonthlyReturnRequest]

      result mustBe MonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )
    }

    "write to JSON correctly" in {
      val model = MonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )
    }
  }
}
