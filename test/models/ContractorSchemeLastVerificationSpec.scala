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

package models

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.ContractorSchemeLastVerification

class ContractorSchemeLastVerificationSpec extends SpecBase {
  "ContractorSchemeLastVerification" - {
    "serialize to JSON correctly" in {
      val scheme = ContractorSchemeLastVerification(
        name = Some("ABC Construction Ltd"),
        utr = Some("1111111111"),
        accountsOfficeReference = Some("123PA00123456"),
        emailAddress = Some("ops@example.com")
      )
      val json   = Json.toJson(scheme)
      (json \ "name").as[String] mustBe "ABC Construction Ltd"
      (json \ "utr").as[String] mustBe "1111111111"
      (json \ "accountsOfficeReference").as[String] mustBe "123PA00123456"
      (json \ "emailAddress").as[String] mustBe "ops@example.com"
    }

    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """ |{
            | "name": "ABC Construction Ltd",
            | "utr": "1111111111",
            | "accountsOfficeReference": "123PA00123456",
            | "emailAddress": "ops@example.com"
            |}
          |""".stripMargin
      )
      val result = json.as[ContractorSchemeLastVerification]
      result.name mustBe Some("ABC Construction Ltd")
      result.utr mustBe Some("1111111111")
      result.accountsOfficeReference mustBe Some("123PA00123456")
      result.emailAddress mustBe Some("ops@example.com")
    }

    "round-trip serialize and deserialize correctly" in {
      val scheme = ContractorSchemeLastVerification(
        name = Some("ABC Construction Ltd"),
        utr = Some("1111111111"),
        accountsOfficeReference = Some("123PA00123456"),
        emailAddress = Some("ops@example.com")
      )
      val json   = Json.toJson(scheme)
      val result = json.as[ContractorSchemeLastVerification]
      result mustBe scheme
    }
  }
}
