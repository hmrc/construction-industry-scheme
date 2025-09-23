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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.JsValue
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class MonthlyReturnsControllerSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val url = s"http://localhost:$port/cis/monthly-returns"

  "GET /cis/monthly-returns" should {

    "return 200 with wrapper when authorised and proxies succeed (happy path)" in {
      AuthStub.authorisedWithCisEnrolment(ton = "111", tor = "test111")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withRequestBody(equalToJson("""{"taxOfficeNumber":"111","taxOfficeReference":"test111"}""", true, true))
          .willReturn(aResponse().withStatus(200).withBody("""{"instanceId":"abc-123"}"""))
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson("""{"instanceId":"abc-123"}""", true, true))
          .willReturn(aResponse().withStatus(200).withBody(
            """{
              | "monthlyReturnList": [
              |   { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
              |   { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
              | ]
              |}""".stripMargin))
      )

      val resp = wsClient.url(url)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token"
        )
        .get()
        .futureValue

      resp.status mustBe OK
      (resp.json \ "monthlyReturnList").as[Seq[JsValue]].size mustBe 2

      verify(postRequestedFor(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id")))
      verify(postRequestedFor(urlPathEqualTo("/formp-proxy/monthly-returns")))
    }

    "return 400 when CIS enrolment identifiers are missing" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val resp = wsClient.url(url)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token"
        )
        .get()
        .futureValue

      resp.status mustBe BAD_REQUEST
      (resp.json \ "message").as[String].toLowerCase must include ("missing cis enrolment")
      verify(0, postRequestedFor(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id")))
      verify(0, postRequestedFor(urlPathEqualTo("/formp-proxy/monthly-returns")))
    }

    "return 401 when there is no active session" in {
      AuthStub.noActiveSession()

      val resp = wsClient.url(url)
        .get()
        .futureValue

      resp.status mustBe UNAUTHORIZED
    }

    "bubble up 500 if SP2 fails" in {
      AuthStub.authorisedWithCisEnrolment(ton = "111", tor = "test111")

      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .willReturn(aResponse().withStatus(200).withBody("""{"instanceId":"abc-123"}"""))
      )
      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .willReturn(aResponse().withStatus(500).withBody("SP2 error"))
      )

      val resp = wsClient.url(url)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token"
        )
        .get()
        .futureValue

      resp.status mustBe INTERNAL_SERVER_ERROR
      (resp.json \ "message").as[String].toLowerCase must include ("returned 500")
    }
  }
}
