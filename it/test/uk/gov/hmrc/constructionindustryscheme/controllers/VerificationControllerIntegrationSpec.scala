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

package uk.gov.hmrc.constructionindustryscheme.controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import play.api.http.Status.{BAD_GATEWAY, OK}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class VerificationControllerIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val instanceId                     = "123"
  private val getCurrentVerificationBatchUrl = s"$base/verification-batch/current/$instanceId"

  "GET /cis/taxpayer" should {

    "return 200 with CurrentVerificationBatch when authorised and formp proxy succeeds" in {
      AuthStub.authorisedWithCisEnrolment()

      val responseJson = Json.parse(
        s"""
           |{
           |  "subcontractors": [
           |    {
           |      "subcontractorId": 1
           |    }
           |  ],
           |  "verificationBatch": {
           |      "verificationBatchId": 99
           |    },
           |  "verifications": [
           |    {
           |      "verificationId": 1001
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/current/$instanceId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(
                responseJson.toString()
              )
          )
      )

      val resp = getJson(
        getCurrentVerificationBatchUrl,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe OK
      (resp.json \ "subcontractors" \ 0 \ "subcontractorId").as[Long] mustBe 1L
      (resp.json \ "verificationBatch"  \ "verificationBatchId").as[Long] mustBe 99L
      (resp.json \ "verifications" \ 0 \ "verificationId").as[Long] mustBe 1001L

      verify(
        getRequestedFor(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/current/$instanceId"))
      )
    }

    "bubble up error when FormP fails" in {
      AuthStub.authorisedWithCisEnrolment()

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/current/$instanceId"))
          .willReturn(aResponse().withStatus(502).withBody("FormP error"))
      )

      val resp = getJson(
        getCurrentVerificationBatchUrl,
        "X-Session-Id" -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String] must include("get-current-verification-batch-failed")

      verify(
        getRequestedFor(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/current/$instanceId"))
      )
    }
  }
}
