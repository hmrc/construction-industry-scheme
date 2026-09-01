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

package models.requests

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateContractorSchemeRequest

class UpdateContractorSchemeRequestSpec extends SpecBase {

  "UpdateContractorSchemeRequest JSON format" - {

    "serialises to JSON" in {
      val request = updateContractorSchemeRequest

      Json.toJson(request) mustBe Json.obj(
        "schemeId"                -> 123,
        "instanceId"              -> "abc-123",
        "taxOfficeNumber"         -> "163",
        "taxOfficeReference"      -> "AB0063",
        "accountsOfficeReference" -> "123PA00123456",
        "prePopCount"             -> 1,
        "prePopSuccessful"        -> "Y",
        "uniqueTaxReference"      -> "1234567890",
        "name"                    -> "ABC Construction Ltd",
        "emailAddress"            -> "test@example.com",
        "version"                 -> 2
      )
    }

    "deserialises from JSON" in {
      val json = Json.obj(
        "schemeId"                -> 123,
        "instanceId"              -> "abc-123",
        "taxOfficeNumber"         -> "163",
        "taxOfficeReference"      -> "AB0063",
        "accountsOfficeReference" -> "123PA00123456",
        "prePopCount"             -> 1,
        "prePopSuccessful"        -> "Y",
        "uniqueTaxReference"      -> "1234567890",
        "name"                    -> "ABC Construction Ltd",
        "emailAddress"            -> "test@example.com",
        "version"                 -> 2
      )

      json.as[UpdateContractorSchemeRequest] mustBe updateContractorSchemeRequest
    }
  }

  private def updateContractorSchemeRequest: UpdateContractorSchemeRequest =
    UpdateContractorSchemeRequest(
      schemeId = 123,
      instanceId = "abc-123",
      taxOfficeNumber = "163",
      taxOfficeReference = "AB0063",
      accountsOfficeReference = "123PA00123456",
      prePopCount = 1,
      prePopSuccessful = "Y",
      uniqueTaxReference = "1234567890",
      name = "ABC Construction Ltd",
      emailAddress = "test@example.com",
      version = 2
    )
}
