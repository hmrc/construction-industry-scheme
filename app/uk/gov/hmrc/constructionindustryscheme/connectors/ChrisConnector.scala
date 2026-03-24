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
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisPollRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.{ChrisPollXmlMapper, ChrisSubmissionXmlMapper}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.xml.Elem

@Singleton
class ChrisConnector @Inject() (
  httpClient: HttpClientV2,
  servicesConfig: ServicesConfig
)(implicit ec: ExecutionContext)
    extends Logging {

  private val chrisCisReturnUrl: String =
    servicesConfig.baseUrl("chris") + servicesConfig.getString("microservice.services.chris.submit-url")

  def pollSubmission(correlationId: String, pollUrl: String)(using HeaderCarrier): Future[ChrisPollResponse] =
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
        if (is2xx(resp.status)) {
          ChrisPollXmlMapper.parse(resp.body) match {
            case Left(err)     =>
              logger.error(
                s"[ChrisConnector] Failed to parse 2xx polling response corrId=$correlationId url=$pollUrl status=${resp.status} body:\n${resp.body}"
              )
              Future.successful(ChrisPollResponse(FATAL_ERROR, None, None))
            case Right(parsed) =>
              Future.successful(parsed)
          }
        } else if (resp.status >= 500) {
          logger.error(
            s"[ChrisConnector] 5xx polling corrId=$correlationId url=$pollUrl status=${resp.status} body:\n${resp.body}"
          )
          Future.successful(ChrisPollResponse(ACCEPTED, correlationId, None, None, None))
        } else {
          logger.error(
            s"[ChrisConnector] Non-2xx/Non-5xx polling corrId=$correlationId url=$pollUrl status=${resp.status} body:\n${resp.body}"
          )
          Future.successful(ChrisPollResponse(FATAL_ERROR, None, None))
        }
      }
      .recover { case NonFatal(e) =>
        logger.error(
          s"[ChrisConnector] Transport exception calling $pollUrl corrId=$correlationId: ${e.getClass.getSimpleName}: ${e.getMessage}"
        )
        ChrisPollResponse(ACCEPTED, correlationId, None, None, None)
      }

  def submitEnvelope(envelope: Elem, correlationId: String)(implicit hc: HeaderCarrier): Future[SubmissionResult] =
    httpClient
      .post(url"$chrisCisReturnUrl")
      .setHeader(
        "Content-Type"  -> "application/xml",
        "Accept"        -> "application/xml",
        "CorrelationId" -> correlationId
      )
      .withBody(envelope.toString)
      .execute[HttpResponse]
      .flatMap { resp =>
        if (is2xx(resp.status)) {
          logger.info(s"[ChrisConnector] corrId=$correlationId status=${resp.status} full-response-body:\n${resp.body}")
          Future.successful(handle2xxResponse(resp, correlationId))
        } else if (resp.status >= 500) {
          Future.failed(UpstreamErrorResponse(resp.body, resp.status, resp.status))
        } else {
          Future.successful(httpError(correlationId, resp.body, resp.status))
        }
      }

  private def handle2xxResponse(resp: HttpResponse, correlationId: String): SubmissionResult = {
    val body = resp.body
    ChrisSubmissionXmlMapper
      .parse(body)
      .fold(
        err => parseError(correlationId, body, err),
        ok => ok
      )
  }

  private def is2xx(status: Int): Boolean =
    status >= 200 && status < 300

  private def truncate(s: String, maxCharacters: Int = 254): String =
    if (s.length <= maxCharacters) s else s.take(maxCharacters) + "…"

  private def parseError(correlationId: String, rawXml: String, err: String): SubmissionResult =
    errorResult(correlationId = correlationId, rawXml = rawXml, errorNumber = "parse", errorText = err)

  private def httpError(correlationId: String, rawXml: String, status: Int): SubmissionResult =
    errorResult(
      correlationId = correlationId,
      rawXml = rawXml,
      errorNumber = s"http$status",
      errorText = truncate(rawXml)
    )

  private def errorResult(
    correlationId: String,
    rawXml: String,
    errorNumber: String,
    errorText: String,
    errorType: String = "fatal"
  ): SubmissionResult =
    SubmissionResult(
      status = FATAL_ERROR,
      rawXml = rawXml,
      meta = GovTalkMeta(
        qualifier = "error",
        function = "submit",
        className = "",
        correlationId = correlationId,
        gatewayTimestamp = None,
        responseEndPoint = ResponseEndPoint("", 0),
        error = Some(GovTalkError(errorNumber, errorType, errorText))
      )
    )
}
