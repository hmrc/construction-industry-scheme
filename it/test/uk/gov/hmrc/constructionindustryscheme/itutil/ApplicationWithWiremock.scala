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

package uk.gov.hmrc.constructionindustryscheme.itutil

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*

import scala.concurrent.ExecutionContext

trait ApplicationWithWiremock
  extends AnyWordSpec
    with GuiceOneServerPerSuite
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  lazy val wireMock = new WireMock

  val extraConfig: Map[String, Any] = {
    Map[String, Any](
      "microservice.services.auth.host" -> WireMockConstants.stubHost,
      "microservice.services.auth.port" -> WireMockConstants.stubPort,
      "microservice.services.chris.host" -> WireMockConstants.stubHost,
      "microservice.services.chris.port" -> WireMockConstants.stubPort,
      "microservice.services.chris.affix-url" -> "/submission/ChRIS/CISR/Filing/sync/CIS300MR",
      "microservice.services.rds-datacache-proxy.host" -> WireMockConstants.stubHost,
      "microservice.services.rds-datacache-proxy.port" -> WireMockConstants.stubPort,
      "microservice.services.formp-proxy.host"      -> WireMockConstants.stubHost,
      "microservice.services.formp-proxy.port"      -> WireMockConstants.stubPort,
      "microservice.services.email.host" -> WireMockConstants.stubHost,
      "microservice.services.email.port" -> WireMockConstants.stubPort,
      "microservice.services.client-exchange-proxy.host" -> WireMockConstants.stubHost,
      "microservice.services.client-exchange-proxy.port" -> WireMockConstants.stubPort
    )
  }

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(extraConfig)
    .build()

  protected lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]
  implicit protected val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit protected val hc: HeaderCarrier = HeaderCarrier()

  protected val base: String = s"http://localhost:$port/cis"

  def getJson(url: String, headers: (String, String)*)(implicit hc: HeaderCarrier, ec: ExecutionContext): HttpResponse =
    httpClient
      .get(url"$url")
      .setHeader(("Accept" -> "application/json") +: headers.toList: _*)
      .execute[HttpResponse]
      .futureValue

  def getJsonWithQuery(url: String, qsKey: String, qsValue: String, headers: (String, String)*): HttpResponse =
    httpClient
      .get(url"$url?$qsKey=$qsValue")
      .setHeader(headers *)
      .execute[HttpResponse]
      .futureValue

  protected def postJson(url: String, body: JsValue, headers: (String, String)*): HttpResponse =
    httpClient.post(url"$url")
      .setHeader(("Content-Type" -> "application/json") +: headers: _*)
      .withBody(body)
      .execute[HttpResponse]
      .futureValue

  protected def postJsonEither(
    url: String,
    body: JsValue,
    headers: (String, String)*
  )(implicit hc: HeaderCarrier): Either[UpstreamErrorResponse, HttpResponse] =
    httpClient
      .post(url"$url")
      .setHeader(("Content-Type" -> "application/json") +: headers: _*)
      .withBody(body)
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .futureValue

  override protected def beforeAll(): Unit =
    wireMock.start()
    super.beforeAll()

  override def beforeEach(): Unit =
    wireMock.resetAll()
    super.beforeEach()

  override def afterAll(): Unit =
    wireMock.stop()
    super.afterAll()
}