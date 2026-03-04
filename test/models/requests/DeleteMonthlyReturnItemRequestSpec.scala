package models.requests

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.DeleteMonthlyReturnItemRequest

class DeleteMonthlyReturnItemRequestSpec extends AnyWordSpec with Matchers {

  "DeleteMonthlyReturnItemRequest JSON format" should {

    "round-trip serialize and deserialize" in {
      val model = DeleteMonthlyReturnItemRequest(
        instanceId = "inst-123",
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 1001L
      )

      Json.fromJson[DeleteMonthlyReturnItemRequest](Json.toJson(model)).get mustBe model
    }
  }
}
