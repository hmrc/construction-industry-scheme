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
import play.api.http.Status.{BAD_GATEWAY, NO_CONTENT}
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}

class ContractorSchemeControllerIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  private val updateSchemeUrl = s"$base/scheme/update"

  "POST /cis/scheme/update" should {

    "return 204 when authorised and formp proxy succeeds" in {
      AuthStub.authorisedWithCisEnrolment()

      val payload = updateSchemePayload

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/update"))
          .withRequestBody(equalToJson(payload.toString(), true, true))
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val resp = postJson(
        updateSchemeUrl,
        payload,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe NO_CONTENT

      verify(
        postRequestedFor(urlPathEqualTo("/formp-proxy/scheme/update"))
          .withRequestBody(equalToJson(payload.toString(), true, true))
      )
    }

    "return upstream status when formp proxy fails" in {
      AuthStub.authorisedWithCisEnrolment()

      stubFor(
        post(urlPathEqualTo("/formp-proxy/scheme/update"))
          .willReturn(aResponse().withStatus(BAD_GATEWAY).withBody("FormP error"))
      )

      val resp = postJson(
        updateSchemeUrl,
        updateSchemePayload,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      resp.status mustBe BAD_GATEWAY
      (resp.json \ "message").as[String] mustBe "FormP error"
    }
  }

  private def updateSchemePayload =
    Json.obj(
      "schemeId"                -> 123,
      "instanceId"              -> "abc-123",
      "taxOfficeNumber"         -> "163",
      "taxOfficeReference"      -> "AB0063",
      "accountsOfficeReference" -> "123PA00123456",
      "prePopCount"             -> 1,
      "prePopSuccessful"        -> "Y",
      "uniqueTaxReference"      -> "1234567890",
      "name"                    -> "ABC Construction Ltd",
      "emailAddress"            -> "test@example.com",
      "version"                 -> 2
    )
}
