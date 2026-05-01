package models.requests

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAmendedMonthlyReturnRequest

class CreateAmendedMonthlyReturnRequestSpec extends SpecBase {

  "CreateAmendedMonthlyReturnRequest" - {

    "must serialise and deserialise correctly" in {
      val model = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      val json = Json.obj(
        "instanceId" -> "1",
        "taxYear"    -> 2025,
        "taxMonth"   -> 1,
        "version"    -> 0
      )

      Json.toJson(model) mustBe json
      json.as[CreateAmendedMonthlyReturnRequest] mustBe model
    }
  }
}
