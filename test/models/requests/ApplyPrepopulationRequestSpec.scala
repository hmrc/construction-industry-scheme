/*
 * Copyright 2025 HM Revenue & Customs
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

package models.requests

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.{Company, SoleTrader}
import uk.gov.hmrc.constructionindustryscheme.models.requests.ApplyPrepopulationRequest

class ApplyPrepopulationRequestSpec extends AnyWordSpec with Matchers {

  "ApplyPrepopulationRequest.format" should {

    "round-trip JSON" in {
      val model = ApplyPrepopulationRequest(
        schemeId = 1000,
        instanceId = "CIS-123456",
        accountsOfficeReference = "123PA00123456",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB1234",
        utr = Some("1234567890"),
        name = "Test Ltd",
        emailAddress = Some("test@example.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = 1,
        prePopSuccessful = "Y",
        version = 2,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      Json.toJson(model).as[ApplyPrepopulationRequest] mustBe model
    }

    "read None for missing optional fields" in {
      val json = Json.parse(
        """
          |{
          |  "schemeId": 1000,
          |  "instanceId": "CIS-123456",
          |  "accountsOfficeReference": "123PA00123456",
          |  "taxOfficeNumber": "123",
          |  "taxOfficeReference": "AB1234",
          |  "name": "Test Ltd",
          |  "prePopCount": 1,
          |  "prePopSuccessful": "N",
          |  "version": 0,
          |  "subcontractorTypes": []
          |}
          |""".stripMargin
      )

      val model = json.as[ApplyPrepopulationRequest]

      model.utr mustBe None
      model.emailAddress mustBe None
      model.displayWelcomePage mustBe None
    }
  }
}
