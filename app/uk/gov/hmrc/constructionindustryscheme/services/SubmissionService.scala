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

package uk.gov.hmrc.constructionindustryscheme.services

import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{BuiltSubmissionPayload, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class SubmissionService @Inject()(
  chrisConnector: ChrisConnector,
  formpProxyConnector: FormpProxyConnector
) extends Logging {
  def createSubmission(request: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[String] =
    formpProxyConnector.createSubmission(request)

  def updateSubmission(req: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateSubmission(req)

  def submitToChris(payload: BuiltSubmissionPayload)(implicit hc: HeaderCarrier): Future[SubmissionResult] = {
    chrisConnector.submitEnvelope(payload.envelope, payload.correlationId)
  }
}
