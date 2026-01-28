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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, equalTo, equalToJson, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.http.Fault
import org.apache.pekko.Done
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.itutil.ApplicationWithWiremock
import uk.gov.hmrc.constructionindustryscheme.models.requests.NilMonthlyReturnOrgSuccessEmail

class EmailConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val connector = app.injector.instanceOf[EmailConnector]
  private val emailPath = "/hmrc/email"

  private def mkRequest(email: String = "test@test.com"): NilMonthlyReturnOrgSuccessEmail =
    NilMonthlyReturnOrgSuccessEmail(
      to = List(email),
      templateId = "cis_nil_monthly_return_org_success",
      parameters = Map("month" -> "September", "year" -> "2025")
    )

  "EmailConnector.sendSuccessfulEmail" should {

    "POST JSON to /hmrc/email and return Done on 202 ACCEPTED" in {
      val req      = mkRequest()
      val bodyJson = Json.toJson(req).toString()

      stubFor(
        post(urlPathEqualTo(emailPath))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(bodyJson, true, true))
          .willReturn(
            aResponse()
              .withStatus(202)
              .withBody("""{"message":"accepted"}""")
          )
      )

      connector.sendSuccessfulEmail(req).futureValue mustBe Done
    }

    "fail the Future when upstream returns non-202 (e.g. 500)" in {
      val req      = mkRequest()
      val bodyJson = Json.toJson(req).toString()

      stubFor(
        post(urlPathEqualTo(emailPath))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(bodyJson, true, true))
          .willReturn(
            aResponse()
              .withStatus(500)
              .withBody("""{"error":"boom"}""")
          )
      )

      val ex = connector.sendSuccessfulEmail(req).failed.futureValue
      ex.getMessage must include("Send email failed: status: 500")
      ex.getMessage must include("""{"error":"boom"}""")
    }

    "fail the Future when the HTTP call fails (e.g. connection reset)" in {
      val req      = mkRequest()
      val bodyJson = Json.toJson(req).toString()

      stubFor(
        post(urlPathEqualTo(emailPath))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(bodyJson, true, true))
          .willReturn(
            aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)
          )
      )

      connector.sendSuccessfulEmail(req).failed.futureValue
    }
  }
}
