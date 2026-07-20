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

package uk.gov.hmrc.constructionindustryscheme.e2e.support

import play.api.libs.json.{JsValue, Json}

import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.net.http.{HttpClient, HttpHeaders, HttpRequest}
import java.net.{URI, URLEncoder}
import java.nio.charset.StandardCharsets
import java.time.Duration

final case class SimpleResponse(status: Int, body: String, headers: HttpHeaders) {
  def json: JsValue = Json.parse(body)
}

/** Blocking HTTP helpers over the JDK client - no curl/jq, so identical behaviour on macOS, Linux and WSL/Windows.
  */
object HttpSupport {

  private val client: HttpClient =
    HttpClient
      .newBuilder()
      .connectTimeout(Duration.ofSeconds(5))
      .build()

  def get(
    url: String,
    headers: Map[String, String] = Map.empty,
    timeout: Duration = Duration.ofSeconds(60)
  ): SimpleResponse =
    send(builder(url, headers, timeout).GET().build())

  def post(
    url: String,
    jsonBody: String,
    headers: Map[String, String] = Map.empty,
    timeout: Duration = Duration.ofSeconds(60)
  ): SimpleResponse =
    send(
      builder(url, headers + ("Content-Type" -> "application/json"), timeout)
        .POST(BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
        .build()
    )

  def encode(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8)

  private def builder(url: String, headers: Map[String, String], timeout: Duration): HttpRequest.Builder =
    headers.foldLeft(HttpRequest.newBuilder(URI.create(url)).timeout(timeout)) { case (b, (k, v)) =>
      b.header(k, v)
    }

  private def send(request: HttpRequest): SimpleResponse = {
    val response = client.send(request, BodyHandlers.ofString())
    SimpleResponse(response.statusCode(), response.body(), response.headers())
  }
}
