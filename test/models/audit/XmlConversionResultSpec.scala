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

package models.audit

import base.SpecBase
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.{JsNull, JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.models.audit.XmlConversionResult

class XmlConversionResultSpec extends SpecBase {

  "XmlConversionResult" - {

    "should create ConversionResult with all fields populated" in {
      val jsonValue = Json.obj("Envelope" -> Json.obj("Version" -> "2.0"))
      val result    = XmlConversionResult(success = true, json = Some(jsonValue), error = None)
      assert(result.success)
      assert(result.json.contains(jsonValue))
      assert(result.error.isEmpty)
    }

    "should create ConversionResult with only error" in {
      val result = XmlConversionResult(success = false, error = Some("Invalid XML"))
      assert(!result.success)
      assert(result.json.isEmpty)
      assert(result.error.contains("Invalid XML"))
    }

    "should serialize ConversionResult to JSON" in {
      val result = XmlConversionResult(success = true, json = Some(Json.obj("key" -> "value")), error = None)
      val json   = Json.toJson(result)
      assert((json \ "success").as[Boolean])
      assert((json \ "json" \ "key").as[String] == "value")
      assert((json \ "error").toOption.isEmpty)
    }

    "should deserialize JSON back to ConversionResult" in {
      val json   = Json.obj(
        "success" -> true,
        "json"    -> Json.obj("data" -> "test"),
        "error"   -> JsNull
      )
      val result = json.as[XmlConversionResult]
      assert(result.success)
      assert((result.json.get \ "data").as[String] == "test")
      assert(result.error.isEmpty)
    }

    "should handle missing optional fields gracefully" in {
      val json   = Json.obj("success" -> false)
      val result = json.as[XmlConversionResult]
      assert(!result.success)
      assert(result.json.isEmpty)
      assert(result.error.isEmpty)
    }

    "should maintain equality and copy semantics" in {
      val result1 = XmlConversionResult(success = true)
      val result2 = result1.copy(error = Some("Some error"))
      assert(result1 != result2)
      assert(result1.copy() == result1)
    }
  }
}
