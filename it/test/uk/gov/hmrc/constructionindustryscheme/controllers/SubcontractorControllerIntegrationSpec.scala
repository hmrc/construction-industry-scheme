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
import play.api.http.Status.*
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class SubcontractorControllerIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val createAndUpdateSubcontractorUrl = s"$base/subcontractor/create-and-update"
  private val getUtrUrl                       = s"$base/subcontractors/utr/123"

  "POST /cis/subcontractor/create-and-update" should {

    "return 204 when FormP succeeds" in {
      AuthStub.authorisedWithCisEnrolment()

      val payload =
        Json.parse(
          """{
            |  "cisId": "1234567890",
            |  "subcontractorType": "soletrader",
            |  "firstName": "John",
            |  "secondName": "Q",
            |  "surname": "Smith",
            |  "country": "United Kingdom",
            |  "utr": "1234567890",
            |  "nino": "AA123456A",
            |  "tradingName": "ACME",
            |  "addressLine1": "1 Main Street",
            |  "city": "London",
            |  "county": "Greater London",
            |  "postcode": "AA1 1AA"
            |}""".stripMargin
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withRequestBody(equalToJson(payload.toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val resp = postJson(
        createAndUpdateSubcontractorUrl,
        payload,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe NO_CONTENT

      verify(
        postRequestedFor(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withRequestBody(equalToJson(payload.toString(), true, true))
      )
    }

    "bubble up error when FormP fails" in {
      AuthStub.authorisedWithCisEnrolment()

      val payload =
        Json.parse(
          """{
            |  "cisId": "1234567890",
            |  "subcontractorType": "soletrader",
            |  "firstName": "John",
            |  "secondName": "Q",
            |  "surname": "Smith",
            |  "country": "United Kingdom",
            |  "utr": "1234567890",
            |  "nino": "AA123456A",
            |  "tradingName": "ACME",
            |  "addressLine1": "1 Main Street",
            |  "city": "London",
            |  "county": "Greater London",
            |  "postcode": "AA1 1AA"
            |}""".stripMargin
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val resp = postJson(
        createAndUpdateSubcontractorUrl,
        payload,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String].toLowerCase must include("create-and-update-subcontractor-failed")
    }
  }

  "GET /cis/subcontractors/utr/:cisId" should {

    "return 200 with wrapper when authorised and formp proxy succeeds" in {
      AuthStub.authorisedWithCisEnrolment()

      stubFor(
        get(urlPathEqualTo("/formp-proxy/cis/subcontractors/123"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                """{
                  | "subcontractors": [
                  |   { "utr": "1234567890" },
                  |   { "utr": "9876543210" },
                  |   { "utr": "1122334455" }
                  | ]
                  |}""".stripMargin
              )
          )
      )

      val resp = getJson(
        getUtrUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe OK
      (resp.json \ "subcontractorUTRs").as[Seq[JsValue]].size mustBe 3

      verify(
        getRequestedFor(urlPathEqualTo("/formp-proxy/cis/subcontractors/123"))
      )
    }

    "bubble up error when FormP fails" in {
      AuthStub.authorisedWithCisEnrolment()

      stubFor(
        get(urlPathEqualTo("/formp-proxy/cis/subcontractors/123"))
          .willReturn(aResponse().withStatus(502).withBody("FormP error"))
      )

      val resp = getJson(
        getUtrUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String] must include("get-subcontractorUTRs-failed")

      verify(
        getRequestedFor(urlPathEqualTo("/formp-proxy/cis/subcontractors/123"))
      )
    }
  }
}
