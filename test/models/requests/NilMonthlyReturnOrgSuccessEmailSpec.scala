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
import org.scalatest.matchers.should.Matchers.*
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{NilMonthlyReturnOrgSuccessEmail, SendEmailRequest}

class NilMonthlyReturnOrgSuccessEmailSpec extends AnyWordSpec {

  "NilMonthlyReturnOrgSuccessEmail" should {

    "build the correct request via the factory apply" in {
      val req = NilMonthlyReturnOrgSuccessEmail(
        email = "user@example.com",
        month = "October",
        year = "2025"
      )

      req.to         shouldBe List("user@example.com")
      req.templateId shouldBe "dtr_cis_nil_monthly_return_org_success"
      req.parameters shouldBe Map("month" -> "October", "year" -> "2025")
    }

    "write to the expected JSON structure" in {
      val req = NilMonthlyReturnOrgSuccessEmail(
        email = "user@example.com",
        month = "October",
        year = "2025"
      )

      val json = Json.toJson(req)
      (json \ "to").as[List[String]]             shouldBe List("user@example.com")
      (json \ "templateId").as[String]           shouldBe "dtr_cis_nil_monthly_return_org_success"
      (json \ "parameters" \ "month").as[String] shouldBe "October"
      (json \ "parameters" \ "year").as[String]  shouldBe "2025"
    }

    "read back from JSON" in {
      val json = Json.obj(
        "to"         -> List("user@example.com"),
        "templateId" -> "dtr_cis_nil_monthly_return_org_success",
        "parameters" -> Json.obj("month" -> "October", "year" -> "2025")
      )

      json.validate[NilMonthlyReturnOrgSuccessEmail] shouldBe a[JsSuccess[_]]

      val req = json.as[NilMonthlyReturnOrgSuccessEmail]
      req.to         shouldBe List("user@example.com")
      req.templateId shouldBe "dtr_cis_nil_monthly_return_org_success"
      req.parameters shouldBe Map("month" -> "October", "year" -> "2025")
    }

    "expose fields correctly when upcast to SendEmailRequest" in {
      val req: SendEmailRequest =
        NilMonthlyReturnOrgSuccessEmail("user@example.com", "October", "2025")

      req.to                  shouldBe List("user@example.com")
      req.templateId          shouldBe "dtr_cis_nil_monthly_return_org_success"
      req.parameters("month") shouldBe "October"
      req.parameters("year")  shouldBe "2025"
    }
  }
}
