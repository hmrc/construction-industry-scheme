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

import com.google.inject.{Inject, Singleton}
import play.api.libs.json.JsValue
import uk.gov.hmrc.constructionindustryscheme.models.audit.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.*

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  def monthlyNilReturnRequestEvent(jsonData: JsValue)(implicit hc: HeaderCarrier): Future[AuditResult] =
    auditConnector.sendExtendedEvent(MonthlyNilReturnRequestEvent(jsonData).extendedDataEvent)

  def monthlyNilReturnResponseEvent(response: AuditResponseReceivedModel)(implicit
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(MonthlyNilReturnResponseEvent(response).extendedDataEvent)

  def clientListRetrievalFailed(credentialId: String, phase: String, reason: Option[String] = None)(implicit
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ClientListRetrievalFailedEvent(credentialId, phase, reason).extendedDataEvent
    )

  def clientListRetrievalInProgress(credentialId: String, phase: String)(implicit
    hc: HeaderCarrier
  ): Future[AuditResult] =
    auditConnector.sendExtendedEvent(
      ClientListRetrievalInProgressEvent(credentialId, phase).extendedDataEvent
    )

}
