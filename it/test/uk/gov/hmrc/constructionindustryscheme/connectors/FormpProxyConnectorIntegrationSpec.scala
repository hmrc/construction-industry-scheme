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

package uk.gov.hmrc.constructionindustryscheme.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.{NilMonthlyReturnRequest, UserMonthlyReturns}
import uk.gov.hmrc.http.UpstreamErrorResponse

class FormpProxyConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {
  
  private val connector = app.injector.instanceOf[FormpProxyConnector]

  private val instanceId = "123"
  private val instanceReqJson = Json.obj("instanceId" -> instanceId)

  "FormpProxyConnector getMonthlyReturns" should {

    "POST instanceId to /formp-proxy/monthly-returns and return wrapper (200)" in {
      val responseJson = Json.parse(
        """{
          |  "monthlyReturnList": [
          |    { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
          |    { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
          |  ]
          |}""".stripMargin) 

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "return empty wrapper when upstream returns an empty list" in {
      val responseJson = Json.parse("""{ "monthlyReturnList": [] }""")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(responseJson.toString()))
      )

      val out = connector.getMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = intercept[Throwable](connector.getMonthlyReturns(instanceId).futureValue)
      ex.getMessage must include ("500")
    }
  }

  "FormpProxyConnector createNilMonthlyReturn" should {

    "POSTs request and returns response model (200)" in {
      val req = NilMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 2,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      val respJson = Json.obj("status" -> "STARTED")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-return/nil/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(respJson.toString()))
      )

      val out = connector.createNilMonthlyReturn(req).futureValue
      Json.toJson(out) mustBe respJson
    }

    "propagates upstream error for non-2xx" in {
      val req = NilMonthlyReturnRequest(instanceId, 2025, 2, "Y", "Y")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/monthly-return/nil/create"))
          .withRequestBody(equalToJson(Json.toJson(req).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{ "message": "oops" }"""))
      )

      val ex = intercept[Throwable](connector.createNilMonthlyReturn(req).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "FormpProxyConnector createAndTrackSubmission" should {

    "POSTs request and maps JSON to submissionId" in {
      val req = CreateSubmissionRequest(
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 4,
        emailRecipient = Some("ops@example.com")
      )

      val responseJson = Json.obj("submissionId" -> "sub-123")

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(201).withBody(responseJson.toString()))
      )

      val id = connector.createSubmission(req).futureValue
      id mustBe "sub-123"
    }

    "propagates upstream error (e.g. 500) as failed Future" in {
      val req = CreateSubmissionRequest(instanceId, 2024, 4)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = intercept[Throwable](connector.createSubmission(req).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "FormpProxyConnector updateSubmission" should {

    "returns Unit when upstream responds 204/200" in {
      val req = UpdateSubmissionRequest(
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        submittableStatus = "ACCEPTED"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateSubmission(req).futureValue mustBe ((): Unit)
    }

    "fails with UpstreamErrorResponse when non-2xx" in {
      val req = UpdateSubmissionRequest(
        instanceId = instanceId, taxYear = 2024, taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="), submittableStatus = "REJECTED"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val ex = connector.updateSubmission(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
    }
  }

}