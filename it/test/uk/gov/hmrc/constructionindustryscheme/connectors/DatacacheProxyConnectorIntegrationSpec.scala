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
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.http.HeaderCarrier

class DatacacheProxyConnectorIntegrationSpec
  extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val connector = app.injector.instanceOf[DatacacheProxyConnector]

  private val er = EmployerReference("111", "test111")
  private val erJson = Json.toJson(er)

  "DatacacheProxyConnector getInstanceId" should {

    "POST Employer Reference to /rds-datacache-proxy/cis-taxpayer/instance-id and return instanceId (200)" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody("""{"instanceId":"abc-123"}""") 
          )
      )

      val out = connector.getInstanceId(er).futureValue
      out mustBe "abc-123"
    }

    "fail the future when upstream returns a non-2xx (e.g. 500)" in {
      stubFor(
        post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer/instance-id"))
          .withRequestBody(equalToJson(erJson.toString(), true, true))
          .willReturn(aResponse().withStatus(500).withBody("rds datacache error"))
      )

      val ex = intercept[Throwable](connector.getInstanceId(er).futureValue)
      ex.getMessage must include ("500")
    }
  }
}