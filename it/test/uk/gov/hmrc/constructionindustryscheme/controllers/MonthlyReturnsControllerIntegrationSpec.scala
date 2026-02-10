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

package uk.gov.hmrc.constructionindustryscheme.controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class MonthlyReturnsControllerIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val taxpayerUrl = s"$base/taxpayer"
  private val monthlyReturnsUrl = s"$base/monthly-returns"
  private val createNilUrl = s"$base/monthly-returns/nil/create"

  "GET /cis/taxpayer" should {

    "return 200 with {CisTaxpayer} when authorised and datacache proxy succeeds" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
          .withRequestBody(equalToJson(
            """{"taxOfficeNumber":"111","taxOfficeReference":"test111"}""",
            true, true
          ))
          .willReturn(aResponse().withStatus(200).withBody(
            """{
              |  "uniqueId": "123",
              |  "taxOfficeNumber": "111",
              |  "taxOfficeRef": "test111",
              |  "employerName1": "TEST LTD"
              |}""".stripMargin
          ))
      )

      val resp = getJson(
        taxpayerUrl,
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe OK
      (resp.json \ "uniqueId").as[String] mustBe "123"
      (resp.json \ "taxOfficeNumber").as[String] mustBe "111"
      (resp.json \ "taxOfficeRef").as[String] mustBe "test111"
      (resp.json \ "employerName1").asOpt[String] mustBe Some("TEST LTD")

      verify(postRequestedFor(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer")))
    }

    "return 404 when contractor not found on datacache proxy" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .willReturn(aResponse().withStatus(404))
      )

      val resp = getJson(
        taxpayerUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe NOT_FOUND
    }

    "return 401 when there is no active session" in {
      AuthStub.noActiveSession()

      val resp = getJson(taxpayerUrl)

      resp.status mustBe UNAUTHORIZED
    }

    "bubble up 5xx from datacache proxy (e.g. 502)" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val resp = getJson(
        taxpayerUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String].toLowerCase must include ("bad gateway")
    }
  }

  "GET /cis/monthly-returns?cisId=..." should {

    "return 200 with wrapper when authorised and formp proxy succeeds" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId":"123"}""", true, true))
          .willReturn(aResponse().withStatus(200).withBody(
            """{
              | "monthlyReturnList": [
              |   { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
              |   { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
              | ]
              |}""".stripMargin))
      )

      val resp = getJsonWithQuery(
        monthlyReturnsUrl,
        "cisId", "123",
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe OK
      (resp.json \ "monthlyReturnList").as[Seq[JsValue]].size mustBe 2

      verify(
        postRequestedFor(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId":"123"}""", true, true))
      )
    }

    "return 400 when cisId query param is missing" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      val resp = getJson(
        monthlyReturnsUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_REQUEST
      (resp.json \ "message").as[String].toLowerCase must include ("missing 'cisid'")
      verify(0, postRequestedFor(urlPathEqualTo("/formp-proxy/monthly-returns")))
    }

    "bubble up 500 if formp proxy fails" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId":"123"}""", true, true))
          .willReturn(aResponse().withStatus(500).withBody("FormP error"))
      )

      val resp = getJsonWithQuery(
        monthlyReturnsUrl,
        "cisId", "123",
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe INTERNAL_SERVER_ERROR
      (resp.json \ "message").as[String].toLowerCase must include ("formp error")
    }
  }

  "POST /cis/monthly-returns/nil/create" should {

    "return 200 with MonthlyReturn when FormP succeeds" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId": "123"}"""))
          .willReturn(aResponse().withStatus(200).withBody("""{"monthlyReturnList": [], "schemeVersion": 1}"""))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/nil/create"))
          .withRequestBody(equalToJson(
            """{
              |  "instanceId": "123",
              |  "taxYear": 2024,
              |  "taxMonth": 10,
              |  "decNilReturnNoPayments": "Y",
              |  "decInformationCorrect": "Y"
              |}""".stripMargin,
            true, true
          ))
          .willReturn(aResponse().withStatus(200).withBody("""{ "status": "STARTED" }"""))
      )

      val resp = postJson(
        createNilUrl,
        Json.parse(
          """{
            |  "instanceId": "123",
            |  "taxYear": 2024,
            |  "taxMonth": 10,
            |  "decNilReturnNoPayments": "Y",
            |  "decInformationCorrect": "Y"
            |}""".stripMargin
        ),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe CREATED
      (resp.json \ "status").as[String] mustBe "STARTED"

      verify(postRequestedFor(urlPathEqualTo("/formp-proxy/cis/monthly-return/nil/create")))
    }

    "bubble up error when FormP fails" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "111", taxOfficeReference = "test111")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId": "123"}"""))
          .willReturn(aResponse().withStatus(200).withBody("""{"monthlyReturnList": [], "schemeVersion": 1}"""))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/nil/create"))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val resp = postJson(
        createNilUrl,
        Json.parse(
          """{
            |  "instanceId": "123",
            |  "taxYear": 2024,
            |  "taxMonth": 10,
            |  "decNilReturnNoPayments": "Y",
            |  "decInformationCorrect": "confirmed"
            |}""".stripMargin
        ),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String].toLowerCase must include ("bad gateway")
    }
      }
  }