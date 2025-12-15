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

package models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, CreateContractorSchemeParams, CreateSchemeResponse, UpdateContractorSchemeParams}

import java.time.Instant

class ContractorSchemeSpec extends AnyWordSpec with Matchers {

  "ContractorScheme JSON format" should {

    "round-trip correctly with and without optional fields" in {
      val model = ContractorScheme(
        schemeId = 1,
        instanceId = "CIS-123",
        accountsOfficeReference = "AOR",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Ltd"),
        lastUpdate = Some(Instant.parse("2025-01-01T00:00:00Z")),
        version = Some(2)
      )

      Json.toJson(model).as[ContractorScheme] mustBe model

      val minimalJson = Json.obj(
        "schemeId" -> 1,
        "instanceId" -> "CIS-123",
        "accountsOfficeReference" -> "AOR",
        "taxOfficeNumber" -> "163",
        "taxOfficeReference" -> "AB0063"
      )

      minimalJson.as[ContractorScheme].utr mustBe None
      minimalJson.as[ContractorScheme].version mustBe None
    }
  }

  "CreateContractorSchemeParams JSON format" should {

    "round-trip" in {
      val model = CreateContractorSchemeParams(
        instanceId = "CIS-123",
        accountsOfficeReference = "AOR",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        name = Some("ABC Ltd")
      )

      Json.toJson(model).as[CreateContractorSchemeParams] mustBe model
    }
  }

  "UpdateContractorSchemeParams JSON format" should {

    "round-trip" in {
      val model = UpdateContractorSchemeParams(
        schemeId = 10,
        instanceId = "CIS-123",
        accountsOfficeReference = "AOR",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        version = Some(1)
      )

      Json.toJson(model).as[UpdateContractorSchemeParams] mustBe model
    }
  }

  "CreateSchemeResponse JSON format" should {

    "round-trip" in {
      val model = CreateSchemeResponse(99)
      Json.toJson(model).as[CreateSchemeResponse] mustBe model
    }
  }
}
