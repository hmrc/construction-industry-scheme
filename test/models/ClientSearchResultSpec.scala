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
import uk.gov.hmrc.constructionindustryscheme.models.CisTaxpayerSearchResult
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

class ClientSearchResultSpec extends AnyWordSpec with Matchers {

  "ClientSearchResult (JSON)" should {

    "read and write a fully populated object with multiple clients" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "clients": [
          |    {
          |      "uniqueId": "client-1",
          |      "taxOfficeNumber": "111",
          |      "taxOfficeRef": "test111",
          |      "aoDistrict": "district1",
          |      "aoPayType": "type1",
          |      "aoCheckCode": "check1",
          |      "aoReference": "ref1",
          |      "validBusinessAddr": "Y",
          |      "correlation": "corr1",
          |      "ggAgentId": "agent1",
          |      "employerName1": "Test Company Ltd",
          |      "employerName2": "Test Company",
          |      "agentOwnRef": "own-ref-1",
          |      "schemeName": "Test Scheme"
          |    },
          |    {
          |      "uniqueId": "client-2",
          |      "taxOfficeNumber": "222",
          |      "taxOfficeRef": "test222",
          |      "aoDistrict": "district2",
          |      "aoPayType": "type2",
          |      "aoCheckCode": "check2",
          |      "aoReference": "ref2",
          |      "validBusinessAddr": "N",
          |      "correlation": "corr2",
          |      "ggAgentId": "agent2",
          |      "employerName1": "Another Company",
          |      "employerName2": "Another",
          |      "agentOwnRef": "own-ref-2",
          |      "schemeName": "Another Scheme"
          |    }
          |  ],
          |  "totalCount": 2,
          |  "clientNameStartingCharacters": ["T", "A"]
          |}
        """.stripMargin
      )

      val model = json.as[ClientSearchResult]
      model mustBe ClientSearchResult(
        clients = List(
          CisTaxpayerSearchResult(
            uniqueId = "client-1",
            taxOfficeNumber = "111",
            taxOfficeRef = "test111",
            aoDistrict = Some("district1"),
            aoPayType = Some("type1"),
            aoCheckCode = Some("check1"),
            aoReference = Some("ref1"),
            validBusinessAddr = Some("Y"),
            correlation = Some("corr1"),
            ggAgentId = Some("agent1"),
            employerName1 = Some("Test Company Ltd"),
            employerName2 = Some("Test Company"),
            agentOwnRef = Some("own-ref-1"),
            schemeName = Some("Test Scheme")
          ),
          CisTaxpayerSearchResult(
            uniqueId = "client-2",
            taxOfficeNumber = "222",
            taxOfficeRef = "test222",
            aoDistrict = Some("district2"),
            aoPayType = Some("type2"),
            aoCheckCode = Some("check2"),
            aoReference = Some("ref2"),
            validBusinessAddr = Some("N"),
            correlation = Some("corr2"),
            ggAgentId = Some("agent2"),
            employerName1 = Some("Another Company"),
            employerName2 = Some("Another"),
            agentOwnRef = Some("own-ref-2"),
            schemeName = Some("Another Scheme")
          )
        ),
        totalCount = 2,
        clientNameStartingCharacters = List("T", "A")
      )

      Json.toJson(model) mustBe json
    }

    "read and write an empty client list" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "clients": [],
          |  "totalCount": 0,
          |  "clientNameStartingCharacters": []
          |}
        """.stripMargin
      )

      val model = json.as[ClientSearchResult]
      model mustBe ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List.empty
      )

      Json.toJson(model) mustBe json
    }

    "read and write a single client with minimal fields" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "clients": [
          |    {
          |      "uniqueId": "u1",
          |      "taxOfficeNumber": "123",
          |      "taxOfficeRef": "AB12345"
          |    }
          |  ],
          |  "totalCount": 1,
          |  "clientNameStartingCharacters": ["A"]
          |}
        """.stripMargin
      )

      val model = json.as[ClientSearchResult]
      model mustBe ClientSearchResult(
        clients = List(
          CisTaxpayerSearchResult(
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
            schemeName = None
          )
        ),
        totalCount = 1,
        clientNameStartingCharacters = List("A")
      )

      Json.toJson(model) mustBe json
    }

    "fail to read when required fields are missing" in {
      val invalidJson: JsValue = Json.parse(
        """
          |{
          |  "clients": [],
          |  "totalCount": 0
          |}
        """.stripMargin
      )

      val result = invalidJson.validate[ClientSearchResult]
      result.isError mustBe true
    }

    "fail to read when totalCount is not a number" in {
      val invalidJson: JsValue = Json.parse(
        """
          |{
          |  "clients": [],
          |  "totalCount": "not-a-number",
          |  "clientNameStartingCharacters": []
          |}
        """.stripMargin
      )

      val result = invalidJson.validate[ClientSearchResult]
      result.isError mustBe true
    }

    "fail to read when clients is not an array" in {
      val invalidJson: JsValue = Json.parse(
        """
          |{
          |  "clients": "not-an-array",
          |  "totalCount": 0,
          |  "clientNameStartingCharacters": []
          |}
        """.stripMargin
      )

      val result = invalidJson.validate[ClientSearchResult]
      result.isError mustBe true
    }

    "read with multiple starting characters" in {
      val json: JsValue = Json.parse(
        """
          |{
          |  "clients": [],
          |  "totalCount": 0,
          |  "clientNameStartingCharacters": ["A", "B", "C", "D", "E"]
          |}
        """.stripMargin
      )

      val model = json.as[ClientSearchResult]
      model.clientNameStartingCharacters mustBe List("A", "B", "C", "D", "E")
    }
  }

  "ClientSearchResult model" should {

    "allow access to client count" in {
      val result = ClientSearchResult(
        clients = List(
          CisTaxpayerSearchResult(
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
            schemeName = None
          )
        ),
        totalCount = 1,
        clientNameStartingCharacters = List("A")
      )

      result.clients.size mustBe 1
      result.totalCount mustBe 1
    }

    "be equal when all fields match" in {
      val result1 = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List("A")
      )

      val result2 = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List("A")
      )

      result1 mustBe result2
    }

    "not be equal when totalCount differs" in {
      val result1 = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List.empty
      )

      val result2 = ClientSearchResult(
        clients = List.empty,
        totalCount = 1,
        clientNameStartingCharacters = List.empty
      )

      result1 must not be result2
    }
  }
}
