package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, MonthlyReturn, UnsubmittedMonthlyReturns}

class UnsubmittedMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "UnsubmittedMonthlyReturns JSON format" should {

    "round-trip to/from JSON" in {
      val model = UnsubmittedMonthlyReturns(
        scheme = ContractorScheme(
          schemeId = 1,
          instanceId = "abc-123",
          accountsOfficeReference = "123PA00123456",
          taxOfficeNumber = "163",
          taxOfficeReference = "AB0063"
        ),
        monthlyReturn = Seq(
          MonthlyReturn(monthlyReturnId = 66666L, taxYear = 2025, taxMonth = 1)
        )
      )

      val json = Json.toJson(model)
      json.as[UnsubmittedMonthlyReturns] mustBe model
    }
  }
}
