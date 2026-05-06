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

package models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.ContractorScheme
import uk.gov.hmrc.constructionindustryscheme.models.response.GetSubmittedMonthlyReturnsDataProxyResponse

class GetSubmittedMonthlyReturnsDataProxyResponseSpec extends AnyWordSpec with Matchers {

  "GetSubmittedMonthlyReturnsDataProxyResponse JSON format" should {

    "serialize and deserialize correctly" in {
      val model = GetSubmittedMonthlyReturnsDataProxyResponse(
        scheme = ContractorScheme(
          schemeId = 999,
          instanceId = "1",
          accountsOfficeReference = "111111111",
          taxOfficeNumber = "163",
          taxOfficeReference = "AB0063",
          utr = Some("1234567890"),
          name = Some("ABC Construction Ltd"),
          emailAddress = Some("test@test.com"),
          displayWelcomePage = Some("Y"),
          prePopCount = Some(5),
          prePopSuccessful = Some("Y"),
          version = Some(1)
        ),
        monthlyReturn = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val json = Json.toJson(model)
      json.as[GetSubmittedMonthlyReturnsDataProxyResponse] mustBe model
    }
  }
}
