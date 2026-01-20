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

import org.apache.pekko.Done
import play.api.Logging
import play.api.http.Status.ACCEPTED
import play.api.libs.json.Json
import play.api.libs.ws.writeableOf_JsValue
import uk.gov.hmrc.constructionindustryscheme.models.requests.SendEmailRequest
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EmailConnector @Inject() (servicesConfig: ServicesConfig, httpClient: HttpClientV2)(implicit ec: ExecutionContext)
    extends Logging {

  private val emailBaseUrl = servicesConfig.baseUrl("email")

  def send(sendEmailRequest: SendEmailRequest)(implicit hc: HeaderCarrier): Future[Done] =
    httpClient
      .post(url"$emailBaseUrl/hmrc/email")
      .withBody(Json.toJson(sendEmailRequest))
      .execute[HttpResponse]
      .flatMap { response =>
        logger.warn(s"Email POST returned ${response.status}: ${response.body}")
        response.status match {
          case ACCEPTED => Future.successful(Done)
          case status   =>
            logger.warn(s"Send email failed with status: $status")
            Future.successful(Done)
        }
      }
      .recoverWith { case NonFatal(e) =>
        logger.warn("Error sending email", e)
        Future.successful(Done)
      }
}
