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
import uk.gov.hmrc.constructionindustryscheme.models.MonthlyReturn

import java.time.LocalDateTime

class MonthlyReturnSpec extends AnyWordSpec with Matchers {

  private val tsStr = "2025-08-31T12:34:56"
  private val ts = LocalDateTime.parse(tsStr)

  "MonthlyReturn (JSON)" should {

    "read and write a fully-populated object" in {
      val json: JsValue = Json.parse(
        s"""
           |{
           |  "monthlyReturnId": 999,
           |  "taxYear": 2025,
           |  "taxMonth": 4,
           |  "nilReturnIndicator": "Y",
           |  "decEmpStatusConsidered": "Y",
           |  "decAllSubsVerified": "Y",
           |  "decInformationCorrect": "Y",
           |  "decNoMoreSubPayments": "N",
           |  "decNilReturnNoPayments": "N",
           |  "status": "Open",
           |  "lastUpdate": "$tsStr",
           |  "amendment": "N",
           |  "supersededBy": 1001
           |}
        """.stripMargin
      )

      val model = json.as[MonthlyReturn]

      model mustBe MonthlyReturn(
        monthlyReturnId = 999L,
        taxYear = 2025,
        taxMonth = 4,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("N"),
        decNilReturnNoPayments = Some("N"),
        status = Some("Open"),
        lastUpdate = Some(ts),
        amendment = Some("N"),
        supersededBy = Some(1001L)
      )

      Json.toJson(model) mustBe json
    }

    "read only required fields and omit None fields on write" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "monthlyReturnId": 1,
          |  "taxYear": 2024,
          |  "taxMonth": 12
          |}
        """.stripMargin
      )

      val model = json.as[MonthlyReturn]

      model mustBe MonthlyReturn(
        monthlyReturnId = 1L,
        taxYear = 2024,
        taxMonth = 12
      )

      Json.toJson(model) mustBe json
    }
  }
}
