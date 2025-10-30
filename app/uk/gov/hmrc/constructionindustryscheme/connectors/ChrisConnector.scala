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
import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.connectors.ChrisConnector.pickUrl
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisPollRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.models.{FATAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.services.chris.{ChrisXmlPollMapper, ChrisXmlSubmissionMapper}
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
                                servicesConfig: ServicesConfig,
                                appConfig: AppConfig
                              )(implicit ec: ExecutionContext) extends Logging {

  private val chrisCisReturnUrl: String =
    servicesConfig.baseUrl("chris") + servicesConfig.getString("microservice.services.chris.submit-url")

  private def targetSubmissionUrl: String =
    pickUrl(chrisCisReturnUrl, appConfig.chrisEnableNon2xx, appConfig.chrisNon2xxOverrideUrl)

  def pollSubmission(correlationId: String, pollUrl: String)(using HeaderCarrier): Future[ChrisPollResponse] = {
    httpClient
      .post(url"$pollUrl")
      .setHeader(
        "Content-Type"  -> "application/xml",
        "Accept"        -> "application/xml",
        "CorrelationId" -> correlationId
      )
      .withBody(ChrisPollRequest(correlationId).paylaod.toString)
      .execute[HttpResponse]
      .flatMap { resp =>
        Future.fromTry(ChrisXmlPollMapper.parse(resp.body)
          .left.map(Exception(_))
          .toTry)
      }
    }

  def submitEnvelope(envelope: Elem, correlationId: String)
                    (implicit hc: HeaderCarrier): Future[SubmissionResult] =
    httpClient
      .post(url"$targetSubmissionUrl")
      .setHeader(
        "Content-Type"  -> "application/xml",
        "Accept"        -> "application/xml",
        "CorrelationId" -> correlationId
      )
      .withBody(envelope.toString)
      .execute[HttpResponse]
      .map{ resp =>
        if (is2xx(resp.status)) {
          logger.info(
            s"[ChrisConnector] corrId=$correlationId status=${resp.status} full-response-body:\n${resp.body}"
          )
        } else {
          logger.error(
            s"[ChrisConnector] NON-2xx corrId=$correlationId url=$targetSubmissionUrl status=${resp.status} response-body:\n${resp.body}"
          )
        }
        handleResponse(resp, correlationId)
      }
      .recover { case NonFatal(e) =>
        logger.error(
          s"[ChrisConnector] Transport exception calling $targetSubmissionUrl corrId=$correlationId: ${e.getClass.getSimpleName}: ${Option(e.getMessage).getOrElse("")}"
        )
        connectionError(correlationId, e)
      }


  private def handleResponse(resp: HttpResponse, correlationId: String): SubmissionResult = {
    val body = resp.body
    if (is2xx(resp.status)) {
      ChrisXmlSubmissionMapper.parse(body).fold(
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
      errorText     = truncate(rawXml)
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
        gatewayTimestamp = None,
        responseEndPoint = ResponseEndPoint("", 0),
        error            = Some(GovTalkError(errorNumber, errorType, errorText))
      )
    )

  private def truncate(s: String, maxCharacters: Int = 254): String =
    if (s.length <= maxCharacters) s else s.take(maxCharacters) + "…"
}

object ChrisConnector {
  def pickUrl(base: String, enabled: Boolean, overrideUrl: Option[String]): String =
    if (enabled) overrideUrl.filter(_.nonEmpty).getOrElse(base) else base
}
