package models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.{UnsubmittedMonthlyReturnsResponse, UnsubmittedMonthlyReturnsRow}

import java.time.LocalDateTime

class UnsubmittedMonthlyReturnsResponseSpec extends AnyWordSpec with Matchers {

  "UnsubmittedMonthlyReturnsRow JSON format" should {

    "round-trip to/from JSON" in {
      val row = UnsubmittedMonthlyReturnsRow(
        taxYear = 2025,
        taxMonth = 1,
        returnType = "Nil",
        status = "PENDING",
        lastUpdate = Some(LocalDateTime.parse("2025-01-01T00:00:00"))
      )

      val json = Json.toJson(row)
      json.as[UnsubmittedMonthlyReturnsRow] mustBe row
    }
  }

  "UnsubmittedMonthlyReturnsResponse JSON format" should {

    "round-trip to/from JSON" in {
      val model = UnsubmittedMonthlyReturnsResponse(
        unsubmittedCisReturns = Seq(
          UnsubmittedMonthlyReturnsRow(
            taxYear = 2025,
            taxMonth = 2,
            returnType = "Standard",
            status = "STARTED",
            lastUpdate = None
          )
        )
      )

      val json = Json.toJson(model)
      json.as[UnsubmittedMonthlyReturnsResponse] mustBe model
    }
  }
}
