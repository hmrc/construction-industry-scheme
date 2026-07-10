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
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime

class FormpProxyConnectorIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val connector = app.injector.instanceOf[FormpProxyConnector]

  private val instanceId      = "123"
  private val subbieResourceRef = 456L
  private val instanceReqJson = Json.obj("instanceId" -> instanceId)

  "FormpProxyConnector getMonthlyReturns" should {

    "POST instanceId to /formp-proxy/monthly-returns and return wrapper (200)" in {
      val responseJson = Json.parse("""{
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
      ex.getMessage must include("500")
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
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/nil/create"))
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
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/nil/create"))
          .withRequestBody(equalToJson(Json.toJson(req).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{ "message": "boom" }"""))
      )

      val ex = intercept[Throwable](connector.createNilMonthlyReturn(req).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "FormpProxyConnector updateMonthlyReturn" should {

    "POST request and return Unit on 2xx" in {
      val req = UpdateMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        decInformationCorrect = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        nilReturnIndicator = "Y",
        status = "STARTED",
        version = Some(1L)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateMonthlyReturn(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-2xx" in {
      val req = UpdateMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        decInformationCorrect = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        nilReturnIndicator = "Y",
        status = "STARTED",
        version = Some(1L)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return/update"))
          .withRequestBody(equalToJson(Json.toJson(req).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.updateMonthlyReturn(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
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
        amendment = "N",
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
      val req = CreateSubmissionRequest(instanceId, 2024, 4, "N")

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
        amendment = "N",
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
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 4,
        amendment = "N",
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="),
        submittableStatus = "REJECTED"
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

  "FormpProxyConnector getSubmittedMonthlyReturns" should {

    "POST instanceId to /formp-proxy/cis/retrieve-submitted-monthly-returns and return wrapper (200)" in {
      val responseJson = Json.parse(
        s"""
           |{
           |  "scheme": {
           |    "schemeId": 999,
           |    "instanceId": "$instanceId",
           |    "accountsOfficeReference": "123PA00123456",
           |    "taxOfficeNumber": "163",
           |    "taxOfficeReference": "AB0063",
           |    "name": "Scheme Name"
           |  },
           |  "monthlyReturns": [
           |    { "monthlyReturnId": 66666, "taxYear": 2025, "taxMonth": 1 },
           |    { "monthlyReturnId": 66667, "taxYear": 2025, "taxMonth": 7 }
           |  ],
           |  "submissions": [
           |    {
           |      "submissionId": 1,
           |      "submissionType": "Type",
           |      "activeObjectId": 1,
           |      "status": "Status",
           |      "hmrcMarkGenerated": "Mark",
           |      "hmrcMarkGgis": "Ggis",
           |      "emailRecipient": "Email",
           |      "acceptedTime": "2025-01-01T00:00:00Z",
           |      "schemeId": 999
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-submitted-monthly-returns"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(responseJson.toString()))
      )

      val out = connector.getSubmittedMonthlyReturns(instanceId).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-submitted-monthly-returns"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = intercept[Throwable](connector.getSubmittedMonthlyReturns(instanceId).futureValue)
      ex.getMessage must include("500")
    }
  }

  "FormpProxyConnector createAndUpdateSubcontractor" should {

    "POSTs sole trader request and returns Unit (204)" in {
      val request: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
          cisId = "10",
          utr = Some("1234567890"),
          nino = Some("AA123456A"),
          firstName = Some("John"),
          surname = Some("Smith"),
          country = Some("United Kingdom"),
          tradingName = Some("ACME")
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.createAndUpdateSubcontractor(request).futureValue mustBe ((): Unit)
    }

    "POSTs company request and returns Unit (204)" in {
      val request: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.CompanyRequest(
          cisId = "10",
          utr = Some("1234567890"),
          crn = Some("CRN123"),
          tradingName = Some("ACME LTD"),
          country = Some("United Kingdom")
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.createAndUpdateSubcontractor(request).futureValue mustBe ((): Unit)
    }

    "POSTs partnership request and returns Unit (204)" in {
      val request: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.PartnershipRequest(
          cisId = "10",
          utr = Some("1234567890"),
          partnerUtr = Some("9999999999"),
          partnershipTradingName = Some("My Partnership"),
          partnerTradingName = Some("Nominated Partner"),
          country = Some("United Kingdom")
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.createAndUpdateSubcontractor(request).futureValue mustBe ((): Unit)
    }

    "fails the future for non-204 response" in {
      val request: CreateAndUpdateSubcontractorRequest =
        CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
          cisId = "10",
          tradingName = Some("ACME")
        )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/subcontractor/create-and-update"))
          .withRequestBody(equalToJson(Json.toJson(request).as[JsObject].toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{ "message": "boom" }"""))
      )

      val ex = intercept[Throwable](connector.createAndUpdateSubcontractor(request).futureValue)
      ex.getMessage.toLowerCase must include("500")
    }
  }

  "FormpProxyConnector getMonthlyReturnForEdit" should {

    "POST request to /formp-proxy/cis/monthly-return-edit and return payload (200)" in {
      val req = GetMonthlyReturnForEditRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 7
      )

      val responseJson = Json.parse(
        s"""
           |{
           |  "scheme": [],
           |  "monthlyReturn": [],
           |  "monthlyReturnItems": [],
           |  "subcontractors": [],
           |  "submission": []
           |}
           |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-edit"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getMonthlyReturnForEdit(req).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns non-2xx (e.g. 500)" in {
      val req = GetMonthlyReturnForEditRequest(instanceId = instanceId, taxYear = 2025, taxMonth = 7)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-edit"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getMonthlyReturnForEdit(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getSubcontractorUTRs" should {

    val cisId = "cis-123"

    "GET /formp-proxy/subcontractor/:cisId and return subcontractor utr list from JSON" in {

      val responseJson = Json.parse(
        s"""
           |{
           |  "subcontractors":
           |  [
           |    {
           |      "subcontractorId": "10101",
           |      "subbieResourceRef": "1",
           |      "type": "soletrader",
           |      "utr": "1111111111",
           |      "tradingName": "AAA",
           |      "version": "1"
           |    },
           |    {
           |      "subcontractorId": "20202",
           |      "subbieResourceRef": "2",
           |      "type": "soletrader",
           |      "utr": "2222222222",
           |      "tradingName": "BBB",
           |      "version": "2"
           |    },
           |    {
           |      "subcontractorId": "30303",
           |      "subbieResourceRef": "3",
           |      "type": "soletrader",
           |      "tradingName": "CCC",
           |      "version": "3"
           |    }
           |  ]
           |}
           |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getSubcontractorUTRs(cisId).futureValue
      out mustBe Seq("1111111111", "2222222222")
    }

    "GET /formp-proxy/subcontractor/:cisId and return empty list from JSON when all subcontractors have no utr" in {

      val responseJson = Json.parse(
        s"""
           |{
           |  "subcontractors":
           |  [
           |    {
           |      "subcontractorId": "10101",
           |      "subbieResourceRef": "1",
           |      "type": "soletrader",
           |      "tradingName": "AAA",
           |      "version": "1"
           |     },
           |     {
           |      "subcontractorId": "20202",
           |      "subbieResourceRef": "2",
           |      "type": "soletrader",
           |      "tradingName": "BBB",
           |      "version": "2"
           |     }
           |  ]
           |}
           |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getSubcontractorUTRs(cisId).futureValue
      out mustBe Seq.empty
    }

    "fail the future when upstream responds with non-2xx (e.g. 502) as failed Future" in {

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(aResponse().withStatus(502).withBody("""{"message":"bad gateway"}"""))
      )

      val ex = connector.getSubcontractorUTRs(cisId).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
    }
  }

  "FormpProxyConnector syncMonthlyReturnItems" should {

    "POST /formp-proxy/cis/monthly-return-item/sync and return Unit on 204" in {
      val req = SyncMonthlyReturnItemsRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(5L, 6L),
        deleteResourceReferences = Seq(1L, 2L)
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/sync"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.syncMonthlyReturnItems(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = SyncMonthlyReturnItemsRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(5L),
        deleteResourceReferences = Seq.empty
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/sync"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.syncMonthlyReturnItems(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector deleteMonthlyReturnItem" should {

    "POST /formp-proxy/cis/monthly-return-item/delete and return Unit on 204" in {
      val req = DeleteMonthlyReturnItemProxyRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 12345L
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/delete"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.deleteMonthlyReturnItem(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = DeleteMonthlyReturnItemProxyRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 12345L
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/delete"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.deleteMonthlyReturnItem(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 400)" in {
      val req = DeleteMonthlyReturnItemProxyRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        resourceReference = 12345L
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/delete"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(400).withBody("bad request"))
      )

      val ex = connector.deleteMonthlyReturnItem(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
    }
  }

  "FormpProxyConnector updateMonthlyReturnItem" should {

    "POST /formp-proxy/cis/monthly-return-item/update and return Unit on 204" in {
      val req = UpdateMonthlyReturnItemProxyRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        itemResourceReference = 123L,
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240",
        subcontractorName = "Tyne Test Ltd",
        verificationNumber = Some("V123456")
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateMonthlyReturnItem(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = UpdateMonthlyReturnItemProxyRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        itemResourceReference = 123L,
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240",
        subcontractorName = "Tyne Test Ltd",
        verificationNumber = Some("V123456")
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-item/update"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.updateMonthlyReturnItem(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getGovTalkStatus" should {

    "POST /formp-proxy/cis/govtalkstatus/get?stage=polling and return Some(response) on 200" in {
      val req = GetGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123"
      )

      val responseJson = Json.parse(
        """
          |{
          |  "govtalk_status": [
          |    {
          |      "userIdentifier": "123",
          |      "formResultID": "sub-123",
          |      "correlationID": "ABC123",
          |      "formLock": "N",
          |      "createDate": "2025-01-01T10:00:00",
          |      "endStateDate": null,
          |      "lastMessageDate": "2025-01-01T10:00:00",
          |      "numPolls": 0,
          |      "pollInterval": 0,
          |      "protocolStatus": "initial",
          |      "gatewayURL": "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/get"))
          .withQueryParam("stage", equalTo("polling"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getGovTalkStatus(req, ChrisSubmissionPhase.Polling).futureValue
      out mustBe Some(responseJson.as[GetGovTalkStatusResponse])
    }

    "POST /formp-proxy/cis/govtalkstatus/get?stage=initial and return None on 404" in {
      val req = GetGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-404"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/get"))
          .withQueryParam("stage", equalTo("initial"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(404)
              .withBody("""{"message":"not found"}""")
          )
      )

      val out = connector.getGovTalkStatus(req, ChrisSubmissionPhase.Initial).futureValue
      out mustBe None
    }

    "fail the future when upstream responds with non-404 error (e.g. 500)" in {
      val req = GetGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-500"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/get"))
          .withQueryParam("stage", equalTo("polling"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.getGovTalkStatus(req, ChrisSubmissionPhase.Polling).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector createGovTalkStatusRecord" should {

    "POST /formp-proxy/cis/govtalkstatus/create and return Unit on 201" in {
      val req = CreateGovTalkStatusRecordRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        correlationID = "ABC123",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(201))
      )

      connector.createGovTalkStatusRecord(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-201 (e.g. 500)" in {
      val req = CreateGovTalkStatusRecordRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        correlationID = "ABC123",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.createGovTalkStatusRecord(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector updateGovTalkStatus" should {

    "POST /formp-proxy/cis/govtalkstatus/update-status and return Unit on 204" in {
      val req = UpdateGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        protocolStatus = "dataRequest"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-status"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateGovTalkStatus(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = UpdateGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        protocolStatus = "dataRequest"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-status"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.updateGovTalkStatus(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector updateGovTalkStatusCorrelationId" should {

    "POST /formp-proxy/cis/govtalkstatus/update-correlationID and return Unit on 204" in {
      val req = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        correlationID = "corr-123",
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-correlationID"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateGovTalkStatusCorrelationId(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = UpdateGovTalkStatusCorrelationIdRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        correlationID = "corr-123",
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-correlationID"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.updateGovTalkStatusCorrelationId(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector updateGovTalkStatusStatistics" should {

    "POST /formp-proxy/cis/govtalkstatus/update-statistics and return Unit on 204" in {
      val req = UpdateGovTalkStatusStatisticsRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        numPolls = 3,
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-statistics"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.updateGovTalkStatusStatistics(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = UpdateGovTalkStatusStatisticsRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        lastMessageDate = LocalDateTime.of(2025, 1, 1, 0, 0),
        numPolls = 3,
        pollInterval = 10,
        gatewayURL = "/poll/123"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-statistics"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.updateGovTalkStatusStatistics(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getNewestVerificationBatch" should {

    "GET /formp-proxy/cis/verification-batch/newest/:instanceId and return payload (200)" in {

      val responseJson = Json.parse(
        s"""
           |{
           |  "subcontractors": [
           |    {
           |      "subcontractorId": 1
           |    }
           |  ],
           |  "scheme": {
           |      "name": "david"
           |    },
           |  "verificationBatch": {
           |      "verificationBatchId": 99
           |    },
           |  "verifications": [
           |    {
           |      "verificationId": 1001
           |    }
           |  ],
           |  "submission": {
           |      "submissionId": 555
           |    },
           |  "monthlyReturn": {
           |      "monthlyReturnId": 777
           |    }
           |}
           |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/newest/$instanceId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val outJson = Json.toJson(connector.getNewestVerificationBatch(instanceId).futureValue)

      (outJson \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L
      (outJson \ "scheme").\("name").as[String] mustBe "david"

      (outJson \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
      (outJson \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      (outJson \ "submission").\("submissionId").as[Long] mustBe 555L
      (outJson \ "monthlyReturn").\("monthlyReturnId").as[Long] mustBe 777L
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/newest/$instanceId"))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getNewestVerificationBatch(instanceId).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getCurrentVerificationBatch" should {

    "GET /formp-proxy/cis/verification-batch/current/:instanceId and return payload (200)" in {

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
              .withBody(responseJson.toString())
          )
      )

      val outJson = Json.toJson(connector.getCurrentVerificationBatch(instanceId).futureValue)

      (outJson \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 1L

      (outJson \ "verificationBatch").\("verificationBatchId").as[Long] mustBe 99L
      (outJson \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/verification-batch/current/$instanceId"))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getCurrentVerificationBatch(instanceId).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector deleteUnsubmittedMonthlyReturn" should {

    "POST /formp-proxy/cis/monthly-return-item/delete and return Unit on 204" in {
      val req = DeleteUnsubmittedMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-returns/unsubmitted/delete"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.deleteUnsubmittedMonthlyReturn(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = DeleteUnsubmittedMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-returns/unsubmitted/delete"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.deleteUnsubmittedMonthlyReturn(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 400)" in {
      val req = DeleteUnsubmittedMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-returns/unsubmitted/delete"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(400).withBody("bad request"))
      )

      val ex = connector.deleteUnsubmittedMonthlyReturn(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
    }
  }

  "FormpProxyConnector getMonthlyReturnComplete" should {

    val completeReq = GetMonthlyReturnCompleteRequest(
      instanceId = instanceId,
      taxYear = 2024,
      taxMonth = 6,
      amendment = "N"
    )

    val completeResponseJson = Json.parse(
      s"""
         |{
         |  "scheme": [
         |    {
         |      "schemeId": 1,
         |      "instanceId": "$instanceId",
         |      "accountsOfficeReference": "123PA00123456",
         |      "taxOfficeNumber": "123",
         |      "taxOfficeReference": "AB456",
         |      "name": "Test Contractor"
         |    }
         |  ],
         |  "monthlyReturn": [
         |    {
         |      "monthlyReturnId": 100,
         |      "taxYear": 2024,
         |      "taxMonth": 6,
         |      "nilReturnIndicator": "N",
         |      "status": "SUBMITTED"
         |    }
         |  ],
         |  "subcontractors": [],
         |  "monthlyReturnItems": [
         |    {
         |      "monthlyReturnId": 100,
         |      "monthlyReturnItemId": 201,
         |      "totalPayments": "5000.00",
         |      "costOfMaterials": "1000.00",
         |      "totalDeducted": "800.00",
         |      "subcontractorName": "John Smith"
         |    }
         |  ],
         |  "submission": [
         |    {
         |      "submissionId": 400,
         |      "submissionType": "Original",
         |      "activeObjectId": 100,
         |      "status": "SUBMITTED",
         |      "hmrcMarkGenerated": "HMRC-123-ABC",
         |      "hmrcMarkGgis": "HMRC-123-ABC",
         |      "emailRecipient": "user@example.com",
         |      "acceptedTime": "2024-07-01T10:30:00",
         |      "schemeId": 1
         |    }
         |  ]
         |}
         |""".stripMargin
    )

    "POST to /formp-proxy/cis/monthly-return-complete and return the response (200)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-complete"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(completeReq).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(completeResponseJson.toString())
          )
      )

      val out = connector.getMonthlyReturnComplete(completeReq).futureValue

      out.scheme.head.instanceId mustBe instanceId
      out.scheme.head.name mustBe Some("Test Contractor")
      out.monthlyReturn.head.monthlyReturnId mustBe 100L
      out.monthlyReturn.head.taxYear mustBe 2024
      out.monthlyReturn.head.taxMonth mustBe 6
      out.monthlyReturnItems.head.totalPayments mustBe Some("5000.00")
      out.submission.head.submissionId mustBe 400L
      out.submission.head.status mustBe Some("SUBMITTED")
      out.submission.head.hmrcMarkGenerated mustBe Some("HMRC-123-ABC")
    }

    "return empty collections when upstream returns empty arrays (200)" in {
      val emptyResponseJson = Json.parse(
        """
          |{
          |  "scheme": [],
          |  "monthlyReturn": [],
          |  "subcontractors": [],
          |  "monthlyReturnItems": [],
          |  "submission": []
          |}
          |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-complete"))
          .withRequestBody(equalToJson(Json.toJson(completeReq).toString(), true, true))
          .willReturn(aResponse().withStatus(200).withBody(emptyResponseJson.toString()))
      )

      val out = connector.getMonthlyReturnComplete(completeReq).futureValue

      out.scheme mustBe empty
      out.monthlyReturn mustBe empty
      out.subcontractors mustBe empty
      out.monthlyReturnItems mustBe empty
      out.submission mustBe empty
    }

    "fail the future when upstream returns non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/monthly-return-complete"))
          .withRequestBody(equalToJson(Json.toJson(completeReq).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getMonthlyReturnComplete(completeReq).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector createVerificationBatchAndVerifications" should {

    "POST /formp-proxy/cis/verification-batch/create and return payload (200)" in {
      val req = CreateVerificationBatchAndVerificationsRequest(
        instanceId = instanceId,
        verificationResourceReferences = Seq(1L, 2L),
        actionIndicator = Some("A")
      )

      val responseJson = Json.parse(
        """
          |{
          |  "verificationBatchResourceReference": 10
          |}
          |""".stripMargin
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification-batch/create"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.createVerificationBatchAndVerifications(req).futureValue
      Json.toJson(out) mustBe responseJson

      (Json.toJson(out) \ "verificationBatchResourceReference").as[Long] mustBe 10L
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      val req = CreateVerificationBatchAndVerificationsRequest(
        instanceId = instanceId,
        verificationResourceReferences = Seq(1L),
        actionIndicator = Some("A")
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification-batch/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.createVerificationBatchAndVerifications(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector createAmendedMonthlyReturn" should {

    "POST /formp-proxy/cis/amend-monthly-return/create and return Unit on 201" in {
      val req = CreateAmendedMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/amend-monthly-return/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(201))
      )

      connector.createAmendedMonthlyReturn(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-201" in {
      val req = CreateAmendedMonthlyReturnRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/amend-monthly-return/create"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.createAmendedMonthlyReturn(req).failed.futureValue

      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector createSubmissionForVerification" should {

    "POST /formp-proxy/cis/verification/submission/create and return response model (201)" in {
      val req = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = instanceId,
        verificationBatchId = 99L,
        verificationBatchResourceRef = 7L,
        emailRecipient = Some("ops@example.com"),
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 111L,
            proceedVerification = "Y"
          ),
          VerificationToUpdate(
            subcontractorName = "BOB BUILDER",
            verificationResourceRef = 222L,
            proceedVerification = "N"
          )
        ),
        agentId = None
      )

      val responseJson = Json.obj("submissionId" -> 555L)

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/submission/create"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.createSubmissionForVerification(req).futureValue
      Json.toJson(out) mustBe responseJson
      out.submissionId mustBe 555L
    }

    "fail with UpstreamErrorResponse when upstream returns non-2xx (e.g. 500)" in {
      val req = CreateSubmissionAndUpdateVerificationsRequest(
        instanceId = instanceId,
        verificationBatchId = 99L,
        verificationBatchResourceRef = 7L,
        emailRecipient = Some("ops@example.com"),
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 111L,
            proceedVerification = "Y"
          )
        ),
        agentId = None
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/submission/create"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.createSubmissionForVerification(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getSubmittedMonthlyReturnsData" should {

    "POST request to /formp-proxy/cis/monthly-return-edit and return payload (200)" in {
      val req = GetSubmittedMonthlyReturnsDataRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      val responseJson = Json.toJson(
        GetSubmittedMonthlyReturnsDataProxyResponse(
          scheme = ContractorScheme(
            schemeId = 100,
            instanceId = "abc-123",
            accountsOfficeReference = "accountsOfficeReference",
            taxOfficeNumber = "taxOfficeNumber",
            taxOfficeReference = "taxOfficeReference"
          ),
          monthlyReturn = Seq.empty,
          monthlyReturnItems = Seq.empty,
          submission = Seq.empty
        )
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-submitted-monthly-returns-data"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getSubmittedMonthlyReturnsData(req).futureValue
      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns non-2xx (e.g. 500)" in {
      val req = GetSubmittedMonthlyReturnsDataRequest(
        instanceId = instanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/retrieve-submitted-monthly-returns-data"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getSubmittedMonthlyReturnsData(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector modifyVerifications" should {

    "POST /formp-proxy/cis/verification-batch/modify and return payload (204)" in {
      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification-batch/modify"))
          .withHeader("Content-Type", containing("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.modifyVerifications(req).futureValue mustBe ((): Unit)
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      val req = ModifyVerificationsRequest(
        instanceId = "abc-123",
        deleteVerifications = Some(
          DeleteVerifications(
            verificationResourceReferences = Seq(111L, 222L)
          )
        ),
        createVerifications = Some(
          CreateVerifications(
            verificationBatchResourceRef = 10L,
            verificationResourceReferences = Seq(333L, 444L)
          )
        )
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification-batch/modify"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.modifyVerifications(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector updateVerificationSubmission" should {

    "POST request to /formp-proxy/cis/verification/submission/update and return Unit when upstream returns 204" in {
      val request = UpdateVerificationSubmissionRequest(
        instanceId = "1",
        verificationBatchResourceRef = 2001L,
        submittableStatus = "SUBMITTED",
        submissionRequestDate = Some(LocalDateTime.parse("2026-06-15T03:30:52")),
        hmrcMarkGenerated = Some("hmrc-mark")
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/submission/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(request).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(204)
          )
      )

      connector.updateVerificationSubmission(request).futureValue mustBe()
    }

    "fail the future when upstream returns non-204" in {
      val request = UpdateVerificationSubmissionRequest(
        instanceId = "1",
        verificationBatchResourceRef = 2001L,
        submittableStatus = "SUBMITTED",
        submissionRequestDate = None,
        hmrcMarkGenerated = None
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/submission/update"))
          .withRequestBody(equalToJson(Json.toJson(request).toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("formp error")
          )
      )

      val ex = connector.updateVerificationSubmission(request).failed.futureValue

      ex mustBe a[UpstreamErrorResponse]

      val upstream = ex.asInstanceOf[UpstreamErrorResponse]
      upstream.statusCode mustBe 500
      upstream.message mustBe "formp error"
    }
  }

  "FormpProxyConnector processVerificationResponseFromChris" should {

    "POST request to /formp-proxy/cis/verification/response/process and return Unit when upstream returns 204" in {
      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "1",
        verificationBatchResourceRef = 5L,
        submissionStatus = "SUBMITTED",
        acceptedTime = "2017-04-06T08:46:08.081",
        irMarkReceived = Some("hmrc-mark"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 13L,
            matched = Some("Y"),
            verified = Some("N"),
            verificationNumber = Some("V1000000007"),
            taxTreatment = "net",
            verifiedDate = Some(LocalDateTime.parse("2017-04-06T08:46:08.081"))
          )
        )
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/response/process"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(
            equalToJson(Json.toJson[ProcessVerificationResponseFromChrisRequest](request).toString(), true, true)
          )
          .willReturn(
            aResponse()
              .withStatus(204)
          )
      )

      connector.processVerificationResponseFromChris(request).futureValue mustBe()
    }

    "fail the future when upstream returns non-204" in {
      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "1",
        verificationBatchResourceRef = 5L,
        submissionStatus = "SUBMITTED",
        acceptedTime = "2017-04-06T08:46:08.081",
        irMarkReceived = Some("hmrc-mark"),
        verificationResults = Seq.empty
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/response/process"))
          .withRequestBody(
            equalToJson(Json.toJson[ProcessVerificationResponseFromChrisRequest](request).toString(), true, true)
          )
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("formp error")
          )
      )

      val ex = connector.processVerificationResponseFromChris(request).failed.futureValue

      ex mustBe a[UpstreamErrorResponse]

      val upstream = ex.asInstanceOf[UpstreamErrorResponse]
      upstream.statusCode mustBe 500
      upstream.message mustBe "formp error"
    }
  }

  "FormpProxyConnector getSchemeEmail" should {

    "POST to /formp-proxy/scheme/email and return Some(email) when upstream returns 200 with email JSON" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/email"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "email": "contractor@example.com" }""")
          )
      )

      val result = connector.getSchemeEmail(instanceId).futureValue
      result mustBe Some("contractor@example.com")
    }

    "return None when the email field is absent from the 200 response body" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/email"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "someOtherField": "value" }""")
          )
      )

      val result = connector.getSchemeEmail(instanceId).futureValue
      result mustBe None
    }

    "return None when upstream responds with 404" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/email"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(404).withBody("""{"message":"not found"}"""))
      )

      val result = connector.getSchemeEmail(instanceId).futureValue
      result mustBe None
    }

    "fail the future with UpstreamErrorResponse when upstream returns 500" in {
      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/email"))
          .withRequestBody(equalToJson(instanceReqJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
      )

      val ex = connector.getSchemeEmail(instanceId).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }
  "FormpProxyConnector resetGovTalkStatus" should {

    "POST /formp-proxy/cis/govtalkstatus/reset and return Unit on 204" in {
      val req = ResetGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/reset"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(204))
      )

      connector.resetGovTalkStatus(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204 (e.g. 500)" in {
      val req = ResetGovTalkStatusRequest(
        userIdentifier = instanceId,
        formResultID = "sub-123",
        oldProtocolStatus = "dataRequest",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/reset"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("formp error"))
      )

      val ex = connector.resetGovTalkStatus(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getBatchPollSubmissions" should {

    "GET /formp-proxy/cis/batchpoll-submissions and return wrapper (200)" in {
      val responseJson = Json.parse(
        """
        {
          "verificationSubmissions": [
            {
              "submissionId": 90001,
              "submissionType": "CISVERIFY",
              "agentId": "A123456",
              "taxOfficeNumber": "123",
              "taxOfficeReference": "ABC123",
              "instanceId": "instance-verification-001",
              "status": "SUBMITTED",
              "verificationBatchResourceRef": 70001
            }
          ],
          "monthlyReturnSubmissions": [
            {
              "submissionId": 90002,
              "submissionType": "CIS300MR",
              "status": "SUBMITTED",
              "taxOfficeNumber": "123",
              "taxOfficeReference": "456789",
              "taxYear": 2025,
              "taxMonth": 6,
              "instanceId": "instance-monthly-return-001",
              "agentId": "A123456"
            }
          ]
        }
      """
      )

      stubFor(
        get(urlPathEqualTo("/formp-proxy/cis/batchpoll-submissions"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val out = connector.getBatchPollSubmissions().futureValue

      Json.toJson(out) mustBe responseJson
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        get(urlPathEqualTo("/formp-proxy/cis/batchpoll-submissions"))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"message":"boom"}""")
          )
      )

      val ex = connector.getBatchPollSubmissions().failed.futureValue

      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector processVerificationResponseFromChris" should {

    "POST /formp-proxy/cis/verification/response/process and return Unit on 204" in {
      val req = ProcessVerificationResponseFromChrisRequest(
        instanceId = instanceId,
        verificationBatchResourceRef = 77L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "ACCEPTED",
        irMarkReceived = Some("IR_MARK_RECEIVED"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 111L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = Some("V123456"),
            taxTreatment = "NET",
            verifiedDate = Some(LocalDateTime.of(2026, 6, 15, 10, 5, 0))
          )
        )
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/cis/verification/response/process"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      connector.processVerificationResponseFromChris(req).futureValue mustBe ((): Unit)
    }

    "fail with UpstreamErrorResponse when upstream returns non-204" in {
      val req = ProcessVerificationResponseFromChrisRequest(
        instanceId = instanceId,
        verificationBatchResourceRef = 77L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "FAILED",
        irMarkReceived = Some("IR_MARK_RECEIVED"),
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 111L,
            matched = Some("N"),
            verified = Some("N"),
            verificationNumber = Some("V123456"),
            taxTreatment = "GROSS",
            verifiedDate = Some(LocalDateTime.of(2026, 6, 15, 10, 5, 0))
          )
        )
      )

        stubFor(
          post(urlPathEqualTo("/formp-proxy/cis/verification/response/process"))
            .withRequestBody(equalToJson(Json.toJson(req).toString(), true, true))
            .willReturn(aResponse().withStatus(500).withBody("""{"message":"boom"}"""))
        )

        val ex = connector.processVerificationResponseFromChris(req).failed.futureValue
        ex mustBe a[UpstreamErrorResponse]
        ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }
  }

  "FormpProxyConnector getSubcontractorDeleteStatus" should {

    "GET /formp-proxy/cis/subcontractor/:cisId/:subbieResourceRef/delete-status and return response model (200)" in {

      val responseJson = Json.parse(
        """{
          |  "subcontractorName": "Gamma Builders",
          |  "subcontractorCanBeDeleted": true
          |}""".stripMargin
      )

      stubFor(
        get(
          urlPathEqualTo(
            s"/formp-proxy/cis/subcontractor/$instanceId/$subbieResourceRef/delete-status"
          )
        ).willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseJson.toString())
        )
      )

      val out =
        connector
          .getSubcontractorDeleteStatus(instanceId, subbieResourceRef)
          .futureValue

      out mustBe GetSubcontractorForDeleteResponse(
        subcontractorName = "Gamma Builders",
        subcontractorCanBeDeleted = true
      )
    }

    "return response model when subcontractor cannot be deleted" in {

      val responseJson = Json.parse(
        """{
          |  "subcontractorName": "Gamma Builders",
          |  "subcontractorCanBeDeleted": false
          |}""".stripMargin
      )

      stubFor(
        get(
          urlPathEqualTo(
            s"/formp-proxy/cis/subcontractor/$instanceId/$subbieResourceRef/delete-status"
          )
        ).willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(responseJson.toString())
        )
      )

      val out =
        connector
          .getSubcontractorDeleteStatus(instanceId, subbieResourceRef)
          .futureValue

      out mustBe GetSubcontractorForDeleteResponse(
        subcontractorName = "Gamma Builders",
        subcontractorCanBeDeleted = false
      )
    }

    "fail with UpstreamErrorResponse when upstream returns non-2xx" in {

      stubFor(
        get(
          urlPathEqualTo(
            s"/formp-proxy/cis/subcontractor/$instanceId/$subbieResourceRef/delete-status"
          )
        ).willReturn(
          aResponse()
            .withStatus(500)
            .withBody("""{"message":"boom"}""")
        )
      )

      val ex =
        connector
          .getSubcontractorDeleteStatus(instanceId, subbieResourceRef)
          .failed
          .futureValue

      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 500
    }

    "fail with UpstreamErrorResponse when upstream returns 400" in {

      stubFor(
        get(
          urlPathEqualTo(
            s"/formp-proxy/cis/subcontractor/$instanceId/$subbieResourceRef/delete-status"
          )
        ).willReturn(
          aResponse()
            .withStatus(400)
            .withBody("""{"message":"bad request"}""")
        )
      )

      val ex =
        connector
          .getSubcontractorDeleteStatus(instanceId, subbieResourceRef)
          .failed
          .futureValue

      ex mustBe a[UpstreamErrorResponse]

      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
    }
  }

  "FormpProxyConnector getSubcontractorList" should {

    val cisId = "cis-123"

    "GET /formp-proxy/cis/subcontractors/:cisId and return the full subcontractor list" in {
      val responseJson = Json.parse(
        """
          |{
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 999,
          |      "subbieResourceRef": 456,
          |      "utr": "1234567890",
          |      "pageVisited": 1,
          |      "firstName": "John",
          |      "secondName": "Q",
          |      "surname": "Smith",
          |      "tradingName": "John Smith Trading",
          |      "subcontractorType": "soletrader",
          |      "addressLine1": "1 Test Street",
          |      "addressLine2": "Flat 2",
          |      "addressLine3": "London",
          |      "country": "United Kingdom",
          |      "postcode": "AA1 1AA",
          |      "emailAddress": "subcontractor@example.com",
          |      "phoneNumber": "01234567890",
          |      "mobilePhoneNumber": "07123456789",
          |      "worksReferenceNumber": "WR-123",
          |      "matched": "Y",
          |      "autoVerified": "N",
          |      "verified": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verificationDate": "2026-06-15T10:05:00",
          |      "version": 1,
          |      "updatedTaxTreatment": "NET",
          |      "lastMonthlyReturnDate": "2026-05-15T10:05:00",
          |      "pendingVerifications": 0
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val response = connector.getSubcontractorList(cisId).futureValue

      response.subcontractors must have size 1

      val subcontractor = response.subcontractors.head
      subcontractor.subcontractorId mustBe 999L
      subcontractor.subbieResourceRef mustBe Some(456L)
      subcontractor.utr mustBe Some("1234567890")
      subcontractor.firstName mustBe Some("John")
      subcontractor.surname mustBe Some("Smith")
      subcontractor.subcontractorType mustBe Some("soletrader")
      subcontractor.taxTreatment mustBe Some("NET")
      subcontractor.verificationNumber mustBe Some("V123456")
      subcontractor.displayName mustBe "John Smith"

      verify(
        getRequestedFor(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
      )
    }

    "returns an empty subcontractor list when FormP returns no subcontractors" in {
      val responseJson = Json.obj(
        "subcontractors" -> Json.arr()
      )

      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(responseJson.toString())
          )
      )

      val response = connector.getSubcontractorList(cisId).futureValue

      response.subcontractors mustBe empty
    }

    "fails when FormP returns a non-2xx response" in {
      stubFor(
        get(urlPathEqualTo(s"/formp-proxy/cis/subcontractors/$cisId"))
          .willReturn(
            aResponse()
              .withStatus(502)
              .withBody("""{"message":"bad gateway"}""")
          )
      )

      val ex = connector.getSubcontractorList(cisId).failed.futureValue

      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 502
    }
  }
}
