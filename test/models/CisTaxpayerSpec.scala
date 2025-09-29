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
import uk.gov.hmrc.constructionindustryscheme.models.CisTaxpayer

class CisTaxpayerSpec extends AnyWordSpec with Matchers {

  "CisTaxpayer (JSON)" should {

    "read and write a fully populated object" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "uniqueId": "abc-123",
          |  "taxOfficeNumber": "123",
          |  "taxOfficeRef": "AB12345",
          |  "aoDistrict": "999",
          |  "aoPayType": "CIS",
          |  "aoCheckCode": "66",
          |  "aoReference": "999/CIS/66",
          |  "validBusinessAddr": "Y",
          |  "correlation": "corr-1",
          |  "ggAgentId": "AGENT-1",
          |  "employerName1": "TEST LTD",
          |  "employerName2": "GROUP",
          |  "agentOwnRef": "A-REF",
          |  "schemeName": "My Scheme",
          |  "utr": "1234567890",
          |  "enrolledSig": "sig"
          |}
        """.stripMargin
      )

      val model = json.as[CisTaxpayer]
      model mustBe CisTaxpayer(
        uniqueId = "abc-123",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB12345",
        aoDistrict = Some("999"),
        aoPayType = Some("CIS"),
        aoCheckCode = Some("66"),
        aoReference = Some("999/CIS/66"),
        validBusinessAddr = Some("Y"),
        correlation = Some("corr-1"),
        ggAgentId = Some("AGENT-1"),
        employerName1 = Some("TEST LTD"),
        employerName2 = Some("GROUP"),
        agentOwnRef = Some("A-REF"),
        schemeName = Some("My Scheme"),
        utr = Some("1234567890"),
        enrolledSig = Some("sig")
      )

      Json.toJson(model) mustBe json
    }

    "read only required fields and omit Nones on write" in {
      val minimalJson: JsValue = Json.parse(
        """
          |{
          |  "uniqueId": "u1",
          |  "taxOfficeNumber": "123",
          |  "taxOfficeRef": "AB12345"
          |}
        """.stripMargin
      )

      val model = minimalJson.as[CisTaxpayer]
      model mustBe CisTaxpayer(
        uniqueId = "u1",
        taxOfficeNumber = "123",
        taxOfficeRef = "AB12345",
        aoDistrict = None,
        aoPayType = None,
        aoCheckCode = None,
        aoReference = None,
        validBusinessAddr = None,
        correlation = None,
        ggAgentId = None,
        employerName1 = None,
        employerName2 = None,
        agentOwnRef = None,
        schemeName = None,
        utr = None,
        enrolledSig = None
      )

      Json.toJson(model) mustBe minimalJson
    }
  }
}