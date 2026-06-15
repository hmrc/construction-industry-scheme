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
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{SendEmailRequest, SubcontractorVerificationEmail}

class SubcontractorVerificationEmailSpec extends SpecBase {

  "SubcontractorVerificationEmail" - {

    "build the correct request via the factory apply" in {
      val req = SubcontractorVerificationEmail(
        email = "test@test.com"
      )

      req.to mustBe List("test@test.com")
      req.templateId mustBe "dtr_subcontractor_verification"
      req.parameters mustBe Map.empty[String, String]
    }

    "write to the expected JSON structure" in {
      val req = SubcontractorVerificationEmail(
        email = "test@test.com"
      )

      val json = Json.toJson(req)

      (json \ "to").as[List[String]] mustBe List("test@test.com")
      (json \ "templateId").as[String] mustBe "dtr_subcontractor_verification"
      (json \ "parameters").as[Map[String, String]] mustBe Map.empty[String, String]
    }

    "read back from JSON" in {
      val json = Json.obj(
        "to"         -> List("test@test.com"),
        "templateId" -> "dtr_subcontractor_verification",
        "parameters" -> Json.obj()
      )

      json.validate[SubcontractorVerificationEmail] mustBe a[JsSuccess[_]]

      val req = json.as[SubcontractorVerificationEmail]

      req.to mustBe List("test@test.com")
      req.templateId mustBe "dtr_subcontractor_verification"
      req.parameters mustBe Map.empty[String, String]
    }

    "expose fields correctly when upcast to SendEmailRequest" in {
      val req: SendEmailRequest =
        SubcontractorVerificationEmail("test@test.com")

      req.to mustBe List("test@test.com")
      req.templateId mustBe "dtr_subcontractor_verification"
      req.parameters mustBe Map.empty[String, String]
    }
  }
}
