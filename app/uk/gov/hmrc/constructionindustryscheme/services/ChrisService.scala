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

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.utils.XmlToJsonConvertor.convertXmlToJson
import uk.gov.hmrc.constructionindustryscheme.connectors.ChrisConnector
import uk.gov.hmrc.constructionindustryscheme.models.audit.AuditResponseReceivedModel
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeBuilder

class ChrisService @Inject()(
                              connector: ChrisConnector,
                              auditService: AuditService
                            )(implicit ec: ExecutionContext) extends Logging {

  def submitNilMonthlyReturn(
                              chrisRequest: ChrisSubmissionRequest,
                              authRequest: AuthenticatedRequest[_]
                            )(implicit hc: HeaderCarrier): Future[HttpResponse] = {

    val envelopeXml = ChrisEnvelopeBuilder.build(chrisRequest, authRequest)

    val submissionData = convertXmlToJson(envelopeXml.toString)

    connector.submitEnvelope(envelopeXml)
      .map { response =>
        val responseData = convertXmlToJson(response.body)
        val resReceived = AuditResponseReceivedModel(response.status, responseData)
        
        auditService.nilReturnSubmissionAudit(submissionData, resReceived)
        if (response.status >= 200 && response.status < 300) {
          logger.info(s"ChRIS submission accepted: status=${response.status}")
          response
        } else {
          val msg = s"ChRIS submission failed status=${response.status} body=${response.body}"
          logger.warn(msg)
          throw new RuntimeException(msg)
        }
      }
  }
}
