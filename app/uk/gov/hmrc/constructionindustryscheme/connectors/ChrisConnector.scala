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

import javax.inject.{Inject, Singleton}
import play.api.libs.ws.DefaultBodyWritables.writeableOf_String
import uk.gov.hmrc.constructionindustryscheme.models.{FATAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisXmlMapper
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.Elem

@Singleton
class ChrisConnector @Inject()(
                                httpClient: HttpClientV2,
                                servicesConfig: ServicesConfig
                              )(implicit ec: ExecutionContext) {

  private val chrisCisReturnUrl: String =
    servicesConfig.baseUrl("chris") + servicesConfig.getString("microservice.services.chris.affix-url")

  def submitEnvelope(envelope: Elem, correlationId: String)
                    (implicit hc: HeaderCarrier): Future[SubmissionResult] =
    httpClient
      .post(url"$chrisCisReturnUrl")
      .setHeader(
        "Content-Type"  -> "application/xml",
        "Accept"        -> "application/xml",
        "CorrelationId" -> correlationId
      )
      .withBody(envelope.toString)
      .execute[HttpResponse]
      .map(handleResponse(_, correlationId))
      .recover { case NonFatal(e) => connectionError(correlationId, e) }


  private def handleResponse(resp: HttpResponse, correlationId: String): SubmissionResult = {
    val body = resp.body
    if (is2xx(resp.status)) {
      ChrisXmlMapper.parse(body).fold(
        err => parseError(correlationId, body, err),
        ok  => ok
      )
    } else {
      httpError(correlationId, body, resp.status)
    }
  }

  private def is2xx(status: Int): Boolean =
    status >= 200 && status < 300

  private def parseError(correlationId: String, rawXml: String, err: String): SubmissionResult =
    errorResult(
      correlationId = correlationId,
      rawXml        = rawXml,
      errorNumber   = "parse",
      errorType     = "fatal",
      errorText     = err
    )

  private def httpError(correlationId: String, rawXml: String, status: Int): SubmissionResult =
    errorResult(
      correlationId = correlationId,
      rawXml        = rawXml,
      errorNumber   = s"http-$status",
      errorType     = "fatal",
      errorText     = truncate(rawXml, 255)
    )

  private def connectionError(correlationId: String, e: Throwable): SubmissionResult =
    errorResult(
      correlationId = correlationId,
      rawXml        = "<connection-error/>",
      errorNumber   = "conn",
      errorType     = "fatal",
      errorText     = s"Connection error: ${e.getClass.getSimpleName}"
    )

  private def errorResult(
                           correlationId: String,
                           rawXml: String,
                           errorNumber: String,
                           errorType: String,
                           errorText: String
                         ): SubmissionResult =
    SubmissionResult(
      status = FATAL_ERROR,
      rawXml = rawXml,
      meta   = GovTalkMeta(
        qualifier        = "error",
        function         = "submit",
        className        = "",
        correlationId    = correlationId,
        gatewayTimestamp = "",
        responseEndPoint = ResponseEndPoint("", 0),
        error            = Some(GovTalkError(errorNumber, errorType, errorText))
      )
    )

  private def truncate(s: String, n: Int): String =
    if (s.length <= n) s else s.take(n) + "…"
}
