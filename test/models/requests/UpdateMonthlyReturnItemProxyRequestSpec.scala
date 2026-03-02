package models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateMonthlyReturnItemProxyRequest

class UpdateMonthlyReturnItemProxyRequestSpec extends AnyWordSpec with Matchers {

  "UpdateMonthlyReturnItemProxyRequest JSON format" should {

    "round-trip (writes then reads) successfully" in {
      val model = UpdateMonthlyReturnItemProxyRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        itemResourceReference = 123L,
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240",
        subcontractorName = "Tyne Test Ltd",
        verificationNumber = "V123456"
      )

      val json = Json.toJson(model)
      json.validate[UpdateMonthlyReturnItemProxyRequest] mustBe JsSuccess(model)
    }

    "read from JSON" in {
      val json = Json.parse("""
        {
          "instanceId": "abc-123",
          "taxYear": 2025,
          "taxMonth": 1,
          "amendment": "N",
          "itemResourceReference": 123,
          "totalPayments": "1200",
          "costOfMaterials": "500",
          "totalDeducted": "240",
          "subcontractorName": "Tyne Test Ltd",
          "verificationNumber": "V123456"
        }
      """)

      val expected = UpdateMonthlyReturnItemProxyRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        itemResourceReference = 123L,
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240",
        subcontractorName = "Tyne Test Ltd",
        verificationNumber = "V123456"
      )

      json.as[UpdateMonthlyReturnItemProxyRequest] mustBe expected
    }
  }
}
