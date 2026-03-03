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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateMonthlyReturnItemRequest

class UpdateMonthlyReturnItemRequestSpec extends AnyWordSpec with Matchers {

  "UpdateMonthlyReturnItemRequest JSON format" should {

    "round-trip successfully (writes then reads)" in {
      val model = UpdateMonthlyReturnItemRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 123L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      val json = Json.toJson(model)

      json.validate[UpdateMonthlyReturnItemRequest] mustBe JsSuccess(model)
    }

    "read from JSON correctly" in {
      val json = Json.parse("""
        {
          "instanceId": "abc-123",
          "taxYear": 2025,
          "taxMonth": 1,
          "subcontractorId": 123,
          "subcontractorName": "Tyne Test Ltd",
          "totalPayments": "1200",
          "costOfMaterials": "500",
          "totalDeducted": "240"
        }
      """)

      val expected = UpdateMonthlyReturnItemRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 123L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      json.as[UpdateMonthlyReturnItemRequest] mustBe expected
    }
  }
}
