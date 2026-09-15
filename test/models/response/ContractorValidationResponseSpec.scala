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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.ContractorScheme
import uk.gov.hmrc.constructionindustryscheme.models.response.ContractorValidationResponse

class ContractorValidationResponseSpec extends SpecBase {

  private val scheme = ContractorScheme(
    schemeId = 1,
    instanceId = "inst-001",
    accountsOfficeReference = "123PX00123456",
    taxOfficeNumber = "123",
    taxOfficeReference = "AB456",
    utr = Some("2234567890"),
    name = Some("ACME Ltd"),
    emailAddress = Some("test@example.com")
  )

  private val response = ContractorValidationResponse(
    utrValid = true,
    schemeNameValid = true,
    emailAddressValid = true,
    scheme = scheme
  )

  "ContractorValidationResponse JSON format" - {

    "serialises to JSON" in {
      val json = Json.toJson(response)

      (json \ "utrValid").as[Boolean] mustBe true
      (json \ "schemeNameValid").as[Boolean] mustBe true
      (json \ "emailAddressValid").as[Boolean] mustBe true
      (json \ "scheme" \ "instanceId").as[String] mustBe "inst-001"
    }

    "deserialises from JSON" in {
      val json = Json.toJson(response)
      json.as[ContractorValidationResponse] mustBe response
    }
  }
}
