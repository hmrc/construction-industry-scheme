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
import uk.gov.hmrc.constructionindustryscheme.models.response.GetGovTalkStatusResponse
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime

class FormpProxyConnectorIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val connector = app.injector.instanceOf[FormpProxyConnector]

  private val instanceId      = "123"
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
        instanceId = instanceId,
        taxYear = 2024,
        taxMonth = 4,
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
           |  "verificationBatch": [
           |    {
           |      "verificationBatchId": 99
           |    }
           |  ],
           |  "verifications": [
           |    {
           |      "verificationId": 1001
           |    }
           |  ],
           |  "submission": [
           |    {
           |      "submissionId": 555
           |    }
           |  ],
           |  "monthlyReturn": [
           |    {
           |      "monthlyReturnId": 777
           |    }
           |  ]
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

      (outJson \ "verificationBatch")(0).\("verificationBatchId").as[Long] mustBe 99L
      (outJson \ "verifications")(0).\("verificationId").as[Long] mustBe 1001L

      (outJson \ "submission")(0).\("submissionId").as[Long] mustBe 555L
      (outJson \ "monthlyReturn")(0).\("monthlyReturnId").as[Long] mustBe 777L
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
}
