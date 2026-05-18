package models.requests

import base.SpecBase
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.DeleteAllMonthlyReturnItemsRequest

class DeleteAllMonthlyReturnItemsRequestSpec extends SpecBase {

  "DeleteAllMonthlyReturnItemsRequest" - {

    "serialize and deserialize JSON" in {
      val model = DeleteAllMonthlyReturnItemsRequest(
        instanceId = "test-instance-id",
        taxYear = 2024,
        taxMonth = 5,
        amendment = "N"
      )

      val json = Json.obj(
        "instanceId" -> "test-instance-id",
        "taxYear"    -> 2024,
        "taxMonth"   -> 5,
        "amendment"  -> "N"
      )

      Json.toJson(model) mustBe json
      json.as[DeleteAllMonthlyReturnItemsRequest] mustBe model
    }
  }
}
