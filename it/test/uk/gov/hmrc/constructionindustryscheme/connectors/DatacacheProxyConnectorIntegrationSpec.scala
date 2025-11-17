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
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.{ClientListStatus, EmployerReference}
import uk.gov.hmrc.http.UpstreamErrorResponse

class DatacacheProxyConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {
  
  private val connector = app.injector.instanceOf[DatacacheProxyConnector]

  private val er = EmployerReference("111", "test111")
  private val erJson = Json.toJson(er)

  "DatacacheProxyConnector getCisTaxpayer" should {

    "POST Employer Reference to /rds-datacache-proxy/cis-taxpayer and return CisTaxpayer (200)" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(
            aResponse().withStatus(200).withBody(
              """{
                |  "uniqueId": "abc-123",
                |  "taxOfficeNumber": "111",
                |  "taxOfficeRef": "test111",
                |  "employerName1": "TEST LTD"
                |}""".stripMargin
            )
          )
      )

      val out = connector.getCisTaxpayer(er).futureValue
      out.uniqueId mustBe "abc-123"
      out.taxOfficeNumber mustBe "111"
      out.taxOfficeRef mustBe "test111"
      out.employerName1 mustBe Some("TEST LTD")
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("rds datacache error"))
      )

      val ex = intercept[Throwable](connector.getCisTaxpayer(er).futureValue)
      ex.getMessage must include ("500")
    }
    
    "propagate 404 not found when taxpayer does not exist" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(404))
      )

      val ex = connector.getCisTaxpayer(er).failed.futureValue
      ex.getMessage must include ("404")
    }
  }

  "DatacacheProxyConnector getClientListDownloadStatus" should {

    val basePath = "/rds-datacache-proxy/cis/client-list-status"
    val credId = "cred-123"
    val service = "CIS"
    val grace = 14400

    def statusUrl = urlPathEqualTo(basePath)

    "map 'InitiateDownload' status from JSON to ClientListStatus.InitiateDownload" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "status": "InitiateDownload" }""")
          )
      )

      val result = connector.getClientListDownloadStatus(credId, service, grace).futureValue
      result mustBe ClientListStatus.InitiateDownload
    }

    "map 'InProgress' status from JSON to ClientListStatus.InProgress" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "status": "InProgress" }""")
          )
      )

      val result = connector.getClientListDownloadStatus(credId, service, grace).futureValue
      result mustBe ClientListStatus.InProgress
    }

    "map 'Succeeded' status from JSON to ClientListStatus.Succeeded" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "status": "Succeeded" }""")
          )
      )

      val result = connector.getClientListDownloadStatus(credId, service, grace).futureValue
      result mustBe ClientListStatus.Succeeded
    }

    "map 'Failed' status from JSON to ClientListStatus.Failed" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "status": "Failed" }""")
          )
      )

      val result = connector.getClientListDownloadStatus(credId, service, grace).futureValue
      result mustBe ClientListStatus.Failed
    }

    "fail with 502 UpstreamErrorResponse when 'status' field is missing in JSON" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "foo": "bar" }""")
          )
      )

      val ex = connector.getClientListDownloadStatus(credId, service, grace).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      val upstream = ex.asInstanceOf[UpstreamErrorResponse]
      upstream.statusCode mustBe 502
      upstream.message must include("invalid payload")
    }

    "fail with RuntimeException when 'status' value is unknown" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{ "status": "test" }""")
          )
      )

      val ex = connector.getClientListDownloadStatus(credId, service, grace).failed.futureValue
      ex mustBe a[RuntimeException]
      ex.getMessage must include("Unknown status 'test' from rds-datacache-proxy")
    }

    "propagate non-2xx responses (e.g. 500) as failed futures" in {
      stubFor(
        get(statusUrl)
          .withQueryParam("credentialId", equalTo(credId))
          .withQueryParam("serviceName", equalTo(service))
          .withQueryParam("gracePeriod", equalTo(grace.toString))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("rds datacache error")
          )
      )

      val ex = connector.getClientListDownloadStatus(credId, service, grace).failed.futureValue
      ex mustBe a[Throwable]
      ex.getMessage must include("rds datacache error")
    }
  }
}