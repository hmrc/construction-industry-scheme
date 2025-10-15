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
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class SubmissionControllerIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val submitUrl = s"http://localhost:$port/cis/chris"
  private val chrisPath = "/submission/ChRIS/CISR/Filing/sync/CIS300MR"

  private val validRequestJson: JsValue = Json.obj(
    "utr"                -> "1234567890",
    "aoReference"        -> "123/AB456",
    "informationCorrect" -> "yes",
    "inactivity"         -> "yes",
    "monthYear"          -> "2025-09"
  )

  "POST /cis/chris (submitNilMonthlyReturn)" should {

    "return 200 with success json when authorised and ChRIS returns 200" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      stubFor(
        post(urlPathEqualTo(chrisPath))
          .withHeader("Content-Type", equalTo("application/xml"))
          .withRequestBody(matchingXPath("/*[local-name()='GovTalkMessage']"))
          .withRequestBody(matchingXPath("//*[local-name()='Contractor']/*[local-name()='UTR' and text()='1234567890']"))
          .withRequestBody(matchingXPath("//*[local-name()='Contractor']/*[local-name()='AOref' and text()='123/AB456']"))
          .withRequestBody(matchingXPath("//*[local-name()='NilReturn' and text()='yes']"))
          .withRequestBody(matchingXPath("//*[local-name()='InformationCorrect' and text()='yes']"))
          .withRequestBody(matchingXPath("//*[local-name()='Inactivity' and text()='yes']"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/xml")
              .withBody("<Ack/>")
          )
      )

      val resp = wsClient.url(submitUrl)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token",
          "Content-Type"  -> "application/json"
        )
        .post(validRequestJson)
        .futureValue

      resp.status mustBe OK
      (resp.json \ "success").as[Boolean] mustBe true
      (resp.json \ "status").as[Int] mustBe 200
      (resp.json \ "body").as[String]  mustBe "<Ack/>"

      verify(
        postRequestedFor(urlPathEqualTo(chrisPath))
          .withHeader("Content-Type", equalTo("application/xml"))
      )
    }

    "return 400 when request JSON is invalid" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      val invalidJson = Json.obj(
        "utr" -> 123,
        "monthYear" -> "2025-09"
      )

      val resp = wsClient.url(submitUrl)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token",
          "Content-Type"  -> "application/json"
        )
        .post(invalidJson)
        .futureValue

      resp.status mustBe BAD_REQUEST
      (resp.json \ "message").isDefined mustBe true

      verify(0, postRequestedFor(urlPathEqualTo(chrisPath)))
    }

    "return 500 when ChRIS responds 500" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      stubFor(
        post(urlPathEqualTo(chrisPath))
          .withRequestBody(matchingXPath("/*[local-name()='GovTalkMessage']"))
          .willReturn(aResponse().withStatus(500).withBody("boom from chris"))
      )

      val resp = wsClient.url(submitUrl)
        .addHttpHeaders(
          "X-Session-Id" -> "it-session-123",
          "Authorization" -> "Bearer it-token",
          "Content-Type"  -> "application/json"
        )
        .post(validRequestJson)
        .futureValue

      resp.status mustBe INTERNAL_SERVER_ERROR
      (resp.json \ "success").as[Boolean] mustBe false
      (resp.json \ "message").as[String].toLowerCase must include ("boom")
    }

    "return 401 when there is no active session" in {
      AuthStub.noActiveSession()

      val resp = wsClient.url(submitUrl)
        .addHttpHeaders("Content-Type" -> "application/json")
        .post(validRequestJson)
        .futureValue

      resp.status mustBe UNAUTHORIZED
      verify(0, postRequestedFor(urlPathEqualTo(chrisPath)))
    }
  }
}