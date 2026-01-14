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
import org.scalatest.OptionValues
import org.scalatest.EitherValues.*
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class SubmissionControllerIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val chrisPath = "/submission/ChRIS/CISR/Filing/sync/CIS300MR"
  private val submissionId = "sub-123"
  private val validRequestJson: JsValue = Json.obj(
    "utr" -> "1234567890",
    "aoReference" -> "754PT00002240",
    "informationCorrect" -> "yes",
    "inactivity" -> "yes",
    "monthYear" -> "2025-09",
    "email" -> "test@test.com"
  )

  private val createUrl = s"$base/submissions/create"
  private def submitToChrisUrl(id: String) = s"$base/submissions/$id/submit-to-chris"
  private def updateUrl(id: String) = s"$base/submissions/$id/update"


  "POST /cis/submissions/create" should {

    "returns 201 with submissionId when authorised and JSON is valid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val body = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "hmrcMarkGenerated" -> "Y"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(matchingJsonPath("$.instanceId"))
          .withRequestBody(matchingJsonPath("$.taxYear"))
          .withRequestBody(matchingJsonPath("$.taxMonth"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "submissionId": "sub-123" }""")
          )
      )

      val res = postJson(createUrl, body,
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      res.status mustBe CREATED
      (res.json \ "submissionId").asOpt[String].value.length must be > 0
    }

    "returns 400 when JSON is invalid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val response = postJsonEither(
        createUrl,
        Json.obj("taxYear" -> 2024),
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      val err = response.left.value
      err.statusCode mustBe BAD_REQUEST
    }

    "returns 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        createUrl,
        Json.obj("instanceId" -> "123", "taxYear" -> 2024, "taxMonth" -> 4)
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }
  }
  
  "POST /cis/chris (submitNilMonthlyReturn)" should {

    "return 200 with success json when authorised and ChRIS returns 200" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      stubFor(
        post(urlPathEqualTo(chrisPath))
          .withHeader("Content-Type", equalTo("application/xml"))
          .withHeader("Accept",       equalTo("application/xml"))
          .withHeader("CorrelationId", matching("[A-F0-9]{32}"))
          .withRequestBody(matchingXPath("/*[local-name()='GovTalkMessage']"))
          .withRequestBody(matchingXPath("//*[local-name()='Contractor']/*[local-name()='UTR' and text()='1234567890']"))
          .willReturn(aResponse().withStatus(500).withBody("boom from chris"))
      )

      val resp = postJson(submitToChrisUrl(submissionId), validRequestJson,
        "X-Session-Id" -> "Session-123", "Authorization" -> "Bearer it-token")

      resp.status mustBe OK
      (resp.json \ "submissionId").as[String] mustBe submissionId
      (resp.json \ "status").as[String] mustBe "FATAL_ERROR"
      (resp.json \ "hmrcMarkGenerated").as[String].nonEmpty mustBe true
      val err = (resp.json \ "error").as[JsObject]
      (err \ "type").as[String].toLowerCase must include ("fatal")
    }

    "return 400 when request JSON is invalid" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      val invalidJson = Json.obj(
        "utr" -> 123,
        "monthYear" -> "2025-09"
      )

      val response = postJsonEither(
        submitToChrisUrl(submissionId),
        invalidJson,
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        submitToChrisUrl(submissionId),
        validRequestJson
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }
  }


  "POST /cis/submissions/:id/update" should {

    "return 204 when authorised and JSON is valid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(matchingJsonPath("$.instanceId"))
          .withRequestBody(matchingJsonPath("$.taxYear"))
          .withRequestBody(matchingJsonPath("$.taxMonth"))
          .withRequestBody(matchingJsonPath("$.submittableStatus"))
          .willReturn(aResponse().withStatus(204))
      )

      val json = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "submittableStatus" -> "ACCEPTED",
      )

      val res = postJson(
        updateUrl(submissionId),
        json,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      res.status mustBe NO_CONTENT
    }

    "return 400 when JSON is invalid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val response = postJsonEither(
        updateUrl(submissionId),
        Json.obj("taxYear" -> 2024),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        updateUrl(submissionId),
        Json.obj(
          "instanceId"        -> "123",
          "taxYear"           -> 2024,
          "taxMonth"          -> 4,
          "submittableStatus" -> "ACCEPTED"
        )
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }

    "return 502 when downstream formp-proxy is non-2xx (e.g. 502)" in {
      AuthStub.authorisedWithoutCisEnrolment()

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val json = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "submittableStatus" -> "REJECTED"
      )

      val response = postJsonEither(
        updateUrl(submissionId),
        json,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_GATEWAY
    }
  }
}