package models.requests

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.DeleteMonthlyReturnItemProxyRequest

class DeleteMonthlyReturnItemProxyRequestSpec extends AnyWordSpec with Matchers {

  "DeleteMonthlyReturnItemProxyRequest JSON format" should {

    "round-trip serialize and deserialize" in {
      val model = DeleteMonthlyReturnItemProxyRequest(
        instanceId = "inst-123",
        taxYear = 2025,
        taxMonth = 1,
        amendment = "A",
        resourceReference = 999L
      )

      Json.fromJson[DeleteMonthlyReturnItemProxyRequest](Json.toJson(model)).get mustBe model
    }
  }
}

