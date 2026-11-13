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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.{CisResponseSubcontractor, EmployerReference, VerificationActionIndicator}
import uk.gov.hmrc.constructionindustryscheme.models.response.GetAllMonthlyReturnDetailsResponse

class JsonFormatsSpec extends AnyFreeSpec with Matchers {

  "CisResponseSubcontractor JSON format" - {
    "round-trip a fully populated value" in {
      val model = CisResponseSubcontractor(
        Some("1234567890"),
        Some("0987654321"),
        Some("Trading"),
        Some("John"),
        Some("A"),
        Some("Smith"),
        Some("AB123456C"),
        Some("Y"),
        Some("net"),
        Some("V1")
      )
      Json.toJson(model).validate[CisResponseSubcontractor] mustBe JsSuccess(model)
    }

    "read an empty object as all None" in {
      Json.obj().as[CisResponseSubcontractor] mustBe
        CisResponseSubcontractor(None, None, None, None, None, None, None, None, None, None)
    }

    "fail when a field has the wrong type" in {
      Json.obj("utr" -> 1).validate[CisResponseSubcontractor].isError mustBe true
    }
  }

  "VerificationActionIndicator JSON format" - {
    "round-trip" in {
      val model = VerificationActionIndicator(5L, "V")
      Json.toJson(model) mustBe Json.obj("verificationResourceRef" -> 5, "actionIndicator" -> "V")
      Json.toJson(model).as[VerificationActionIndicator] mustBe model
    }

    "fail when a field is missing" in {
      Json.obj("verificationResourceRef" -> 5).validate[VerificationActionIndicator].isError mustBe true
    }
  }

  "EmployerReference JSON format" - {
    "round-trip" in {
      val model = EmployerReference("123", "AB456")
      Json.toJson(model) mustBe Json.obj("taxOfficeNumber" -> "123", "taxOfficeReference" -> "AB456")
      Json.toJson(model).as[EmployerReference] mustBe model
    }

    "fail when a field is missing" in {
      Json.obj("taxOfficeNumber" -> "123").validate[EmployerReference].isError mustBe true
    }
  }

  "GetAllMonthlyReturnDetailsResponse JSON format" - {
    "round-trip with empty collections" in {
      val model = GetAllMonthlyReturnDetailsResponse(Nil, Nil, Nil, Nil, Nil)
      Json.toJson(model).as[GetAllMonthlyReturnDetailsResponse] mustBe model
    }

    "fail when a collection is missing" in {
      Json.obj("scheme" -> Json.arr()).validate[GetAllMonthlyReturnDetailsResponse].isError mustBe true
    }
  }
}
