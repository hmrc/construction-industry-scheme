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

package models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.{UnsubmittedMonthlyReturnsResponse, UnsubmittedMonthlyReturnsRow}

import java.time.LocalDateTime

class UnsubmittedMonthlyReturnsResponseSpec extends AnyWordSpec with Matchers {

  "UnsubmittedMonthlyReturnsRow JSON format" should {

    "round-trip to/from JSON" in {
      val row = UnsubmittedMonthlyReturnsRow(
        taxYear = 2025,
        taxMonth = 1,
        returnType = "Nil",
        status = "PENDING",
        lastUpdate = Some(LocalDateTime.parse("2025-01-01T00:00:00"))
      )

      val json = Json.toJson(row)
      json.as[UnsubmittedMonthlyReturnsRow] mustBe row
    }
  }

  "UnsubmittedMonthlyReturnsResponse JSON format" should {

    "round-trip to/from JSON" in {
      val model = UnsubmittedMonthlyReturnsResponse(
        unsubmittedCisReturns = Seq(
          UnsubmittedMonthlyReturnsRow(
            taxYear = 2025,
            taxMonth = 2,
            returnType = "Standard",
            status = "STARTED",
            lastUpdate = None
          )
        )
      )

      val json = Json.toJson(model)
      json.as[UnsubmittedMonthlyReturnsResponse] mustBe model
    }
  }
}
