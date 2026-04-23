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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.ContractorSchemeNewVerification

final class ContractorSchemeNewVerificationSpec extends AnyWordSpec with Matchers {

  "ContractorSchemeNewVerification JSON format" should {

    "read JSON into model (including missing optional fields)" in {
      val json = Json.parse(
        """
          |{
          |  "accountsOfficeReference": "123PA12345678"
          |}
          |""".stripMargin
      )

      val result = json.validate[ContractorSchemeNewVerification]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.accountsOfficeReference mustBe Some("123PA12345678")
      out.utr mustBe None
      out.name mustBe None
      out.emailAddress mustBe None
    }

    "read JSON into model when all fields are present" in {
      val json = Json.parse(
        """
          |{
          |  "accountsOfficeReference": "123PA12345678",
          |  "utr": "1234567890",
          |  "name": "ABC Construction Ltd",
          |  "emailAddress": "test@example.com"
          |}
          |""".stripMargin
      )

      val out = json.as[ContractorSchemeNewVerification]
      out.accountsOfficeReference mustBe Some("123PA12345678")
      out.utr mustBe Some("1234567890")
      out.name mustBe Some("ABC Construction Ltd")
      out.emailAddress mustBe Some("test@example.com")
    }

    "write model to JSON" in {
      val model = ContractorSchemeNewVerification(
        accountsOfficeReference = Some("123PA12345678"),
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd"),
        emailAddress = Some("test@example.com")
      )

      val json = Json.toJson(model)

      (json \ "accountsOfficeReference").as[String] mustBe "123PA12345678"
      (json \ "utr").as[String] mustBe "1234567890"
      (json \ "name").as[String] mustBe "ABC Construction Ltd"
      (json \ "emailAddress").as[String] mustBe "test@example.com"
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = ContractorSchemeNewVerification(
        accountsOfficeReference = None,
        utr = None,
        name = Some("Only Name"),
        emailAddress = None
      )

      val json = Json.toJson(model)
      json.validate[ContractorSchemeNewVerification] mustBe JsSuccess(model)
    }
  }
}
