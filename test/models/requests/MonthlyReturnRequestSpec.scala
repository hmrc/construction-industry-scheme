package models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.MonthlyReturnRequest

class MonthlyReturnRequestSpec extends AnyFreeSpec with Matchers {

  "MonthlyReturnRequest JSON format" - {

    "read from JSON correctly" in {
      val json = Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )

      val result = json.as[MonthlyReturnRequest]

      result mustBe MonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )
    }

    "write to JSON correctly" in {
      val model = MonthlyReturnRequest(
        instanceId = "abc-123",
        taxYear    = 2025,
        taxMonth   = 2
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "instanceId" -> "abc-123",
        "taxYear"    -> 2025,
        "taxMonth"   -> 2
      )
    }
  }
}
