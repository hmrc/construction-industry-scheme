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

package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, MonthlyReturn, UnsubmittedMonthlyReturns}

class UnsubmittedMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "UnsubmittedMonthlyReturns JSON format" should {

    "round-trip to/from JSON" in {
      val model = UnsubmittedMonthlyReturns(
        scheme = ContractorScheme(
          schemeId = 1,
          instanceId = "abc-123",
          accountsOfficeReference = "123PA00123456",
          taxOfficeNumber = "163",
          taxOfficeReference = "AB0063"
        ),
        monthlyReturn = Seq(
          MonthlyReturn(monthlyReturnId = 66666L, taxYear = 2025, taxMonth = 1)
        )
      )

      val json = Json.toJson(model)
      json.as[UnsubmittedMonthlyReturns] mustBe model
    }
  }
}
