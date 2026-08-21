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
import uk.gov.hmrc.constructionindustryscheme.models.SubcontractorLastVerification

class SubcontractorLastVerificationSpec extends SpecBase {
  "SubcontractorLastVerification" - {
    "serialize to JSON correctly" in {
      val subcontractor = SubcontractorLastVerification(
        subcontractorId = 1L,
        subbieResourceRef = Some(10L),
        subcontractorType = Some("soletrader"),
        utr = Some("1111111111")
      )
      val json          = Json.toJson(subcontractor)
      (json \ "subcontractorId").as[Long] mustBe 1L
      (json \ "subbieResourceRef").as[Long] mustBe 10L
      (json \ "subcontractorType").as[String] mustBe "soletrader"
      (json \ "utr").as[String] mustBe "1111111111"
    }

    "deserialize from JSON correctly" in {
      val json   = Json.parse(
        """|{
            | "subcontractorId": 1,
            | "subbieResourceRef": 10,
            | "subcontractorType": "soletrader",
            | "utr": "1111111111"
          |}""".stripMargin
      )
      val result = json.as[SubcontractorLastVerification]
      result.subcontractorId mustBe 1L
      result.subbieResourceRef mustBe Some(10L)
      result.subcontractorType mustBe Some("soletrader")
      result.utr mustBe Some("1111111111")
    }
    "round-trip serialize and deserialize correctly" in {
      val subcontractor = SubcontractorLastVerification(
        subcontractorId = 1L,
        subbieResourceRef = Some(10L),
        subcontractorType = Some("soletrader"),
        utr = Some("1111111111")
      )
      val json          = Json.toJson(subcontractor)
      val result        = json.as[SubcontractorLastVerification]
      result mustBe subcontractor
    }
  }
}
