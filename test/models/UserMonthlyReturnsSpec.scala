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

package models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.{MonthlyReturn, UserMonthlyReturns}

import java.time.LocalDateTime

class UserMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "UserMonthlyReturns (JSON)" should {

    "read and write a non-empty list" in {
      val mr      = MonthlyReturn(
        monthlyReturnId = 7L,
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = Some("Y"),
        status = Some("Open"),
        lastUpdate = Some(LocalDateTime.parse("2025-02-10T10:00:00"))
      )
      val wrapper = UserMonthlyReturns(Seq(mr))
      val json    = Json.obj("monthlyReturnList" -> Json.arr(Json.toJson(mr)))

      Json.toJson(wrapper) mustBe json
      json.as[UserMonthlyReturns] mustBe wrapper
    }

    "read and write empty" in {
      val json = Json.obj("monthlyReturnList" -> Json.arr())
      json.as[UserMonthlyReturns] mustBe UserMonthlyReturns.empty
      Json.toJson(UserMonthlyReturns.empty) mustBe json
    }
  }
}
