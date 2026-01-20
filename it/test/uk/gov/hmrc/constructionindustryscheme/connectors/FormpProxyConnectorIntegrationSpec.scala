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
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.*
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
          .willReturn(aResponse().withStatus(500).withBody("""{ "message": "boom" }"""))
      )

      val ex = intercept[Throwable](connector.createNilMonthlyReturn(req).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "FormpProxyConnector createMonthlyReturn" should {

    "POST /formp-proxy/cis/monthly-return/standard/create and return Unit on 2xx" in {
      val req = MonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/standard/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(201))
      )

      connector.createMonthlyReturn(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-2xx (e.g. 500)" in {
      val req = MonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/standard/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.createMonthlyReturn(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
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

  "FormpProxyConnector getContractorScheme" should {

    "GET /formp-proxy/scheme/:instanceId and return Some(ContractorScheme) on 200" in {
      val responseJson = Json.parse(
        s"""
           |{
           |  "schemeId": 999,
           |  "instanceId": "$instanceId",
           |  "accountsOfficeReference": "123PA00123456",
           |  "taxOfficeNumber": "163",
           |  "taxOfficeReference": "AB0063",
           |  "utr": "1234567890",
           |  "name": "ABC Construction Ltd"
           |}
           |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/scheme/$instanceId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val outOpt = connector.getContractorScheme(instanceId).futureValue
      outOpt.isDefined mustBe true

      val out = outOpt.value
      out.schemeId mustBe 999
      out.instanceId mustBe instanceId
      out.accountsOfficeReference mustBe "123PA00123456"
      out.taxOfficeNumber mustBe "163"
      out.taxOfficeReference mustBe "AB0063"
      out.utr mustBe Some("1234567890")
      out.name mustBe Some("ABC Construction Ltd")
    }

    "return None when upstream responds with 404" in {
      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/scheme/$instanceId"))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withBody("""{"message":"not found"}""")
          )
      )

      val outOpt = connector.getContractorScheme(instanceId).futureValue
      outOpt mustBe None
    }

    "fail the future when upstream responds with a non-404 error (e.g. 500)" in {
      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/scheme/$instanceId"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.getContractorScheme(instanceId).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector createContractorScheme" should {

    "POST /formp-proxy/scheme and return schemeId on success" in {
      val req = CreateContractorSchemeParams(
        instanceId = instanceId,
        accountsOfficeReference = "123PA00123456",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd")
      )

      val responseJson = Json.obj("schemeId" -> 9876)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody(responseJson.toString())
          )
      )

      val id = connector.createContractorScheme(req).futureValue
      id mustBe 9876
    }

    "fail the future when upstream responds with non-2xx (e.g. 500)" in {
      val req = CreateContractorSchemeParams(
        instanceId = instanceId,
        accountsOfficeReference = "123PA00123456",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd")
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.createContractorScheme(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector updateContractorScheme" should {

    "return Unit when upstream responds with 2xx" in {
      val req = UpdateContractorSchemeParams(
        schemeId = 999,
        instanceId = instanceId,
        accountsOfficeReference = "123PA00123456",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd"),
        emailAddress = Some("test@example.com"),
        prePopCount = Some(1),
        prePopSuccessful = Some("Y"),
        version = Some(3)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(200))
      )

      connector.updateContractorScheme(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream responds with non-2xx" in {
      val req = UpdateContractorSchemeParams(
        schemeId = 999,
        instanceId = instanceId,
        accountsOfficeReference = "123PA00123456",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd"),
        emailAddress = Some("test@example.com"),
        prePopCount = Some(1),
        prePopSuccessful = Some("Y"),
        version = Some(3)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/update"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(502)
              .withBody("bad gateway")
          )
      )

      val ex = connector.updateContractorScheme(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
    }
  }

  "FormpProxyConnector updateSchemeVersion" should {

    "POST /formp-proxy/scheme/version-update and return version from JSON" in {
      val req = UpdateSchemeVersionRequest(
        instanceId = instanceId,
        version = 1
      )

      val responseJson = Json.obj("version" -> 2)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/version-update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.updateSchemeVersion(req).futureValue
      out mustBe 2
    }

    "fail the future when upstream responds with non-2xx (e.g. 500)" in {
      val req = UpdateSchemeVersionRequest(instanceId = instanceId, version = 1)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/version-update"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.updateSchemeVersion(req).failed.futureValue
      ex mustBe a[play.api.libs.json.JsResultException]
    }
  }

  "FormpProxyConnector createSubcontractor" should {

    "POST /formp-proxy/subcontractor/create and return subbieResourceRef from JSON" in {
      val req = CreateSubcontractorRequest(
        schemeId = 999,
        subcontractorType = SoleTrader,
        version = 1
      )

      val responseJson = Json.obj("subbieResourceRef" -> 1234)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.createSubcontractor(req).futureValue
      out mustBe 1234
    }

    "fail the future when upstream responds with non-2xx (e.g. 502) as failed Future" in {
      val req = CreateSubcontractorRequest(schemeId = 999, subcontractorType = SoleTrader, version = 1)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(502).withBody("""{"message":"bad gateway"}"""))
      )

      val ex = connector.createSubcontractor(req).failed.futureValue
      ex mustBe a[play.api.libs.json.JsResultException]
    }
  }

  "FormpProxyConnector applyPrepopulation" should {

    "POST /formp-proxy/scheme/prepopulate and return version from JSON" in {
      val req = ApplyPrepopulationRequest(
        schemeId = 789,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        utr = Some("9876543210"),
        name = "Test Contractor",
        emailAddress = Some("test@test.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = 5,
        prePopSuccessful = "Y",
        version = 1,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      val responseJson = Json.obj("version" -> 2)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/prepopulate"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.applyPrepopulation(req).futureValue
      out mustBe 2
    }

    "fail the future when upstream responds with non-2xx (e.g. 500)" in {
      val req = ApplyPrepopulationRequest(
        schemeId = 789,
        instanceId = "abc-123",
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        utr = Some("9876543210"),
        name = "Test Contractor",
        emailAddress = Some("test@test.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = 5,
        prePopSuccessful = "Y",
        version = 1,
        subcontractorTypes = Seq(SoleTrader, Company)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/prepopulate"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.applyPrepopulation(req).failed.futureValue
      ex mustBe a[play.api.libs.json.JsResultException]
    }
  }

  "FormpProxyConnector getUnsubmittedMonthlyReturns" should {

    "POST instanceId to /formp-proxy/cis/retrieve-unsubmitted-monthly-returns and return wrapper (200)" in {
      val responseJson = Json.parse(
        s"""
           |{
           |  "scheme": {
           |    "schemeId": 999,
           |    "instanceId": "$instanceId",
           |    "accountsOfficeReference": "123PA00123456",
           |    "taxOfficeNumber": "163",
           |    "taxOfficeReference": "AB0063"
           |  },
           |  "monthlyReturn": [
           |    { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
           |    { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
           |  ]
           |}
           |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-unsubmitted-monthly-returns"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(responseJson.toString()))
      )

      val out = connector.getUnsubmittedMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-unsubmitted-monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = intercept[Throwable](connector.getUnsubmittedMonthlyReturns(instanceId).futureValue)
      ex.getMessage must include("500")
    }
  }

  "FormpProxyConnector updateSubcontractor" should {

    "POSTs request and returns response model (201)" in {
      val request = UpdateSubcontractorRequest(schemeId = 10, subbieResourceRef = 10, tradingName = Some("trading Name"))

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result: Unit = connector.updateSubcontractor(request).futureValue
      result mustBe ()
    }

    "propagates upstream error for non-2xx" in {
      val request = UpdateSubcontractorRequest(schemeId = 10, subbieResourceRef = 10, tradingName = Some("trading Name"))

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/update"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{ "message": "boom" }"""))
      )

      val ex = intercept[Throwable](connector.updateSubcontractor(request).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }
}
