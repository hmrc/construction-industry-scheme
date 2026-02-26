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
