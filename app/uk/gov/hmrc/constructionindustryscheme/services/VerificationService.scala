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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class VerificationService @Inject() (formpProxyConnector: FormpProxyConnector) {

  def getNewestVerificationBatch(instanceId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetNewestVerificationBatchResponse] =
    formpProxyConnector.getNewestVerificationBatch(instanceId)

  def getCurrentVerificationBatch(instanceId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetCurrentVerificationBatchResponse] =
    formpProxyConnector.getCurrentVerificationBatch(instanceId)

  def createVerificationBatchAndVerifications(
    request: CreateVerificationBatchAndVerificationsRequest
  )(implicit hc: HeaderCarrier): Future[CreateVerificationBatchAndVerificationsResponse] =
    formpProxyConnector.createVerificationBatchAndVerifications(request)

  def modifyVerifications(
    request: ModifyVerificationsRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.modifyVerifications(request)

  def createSubmissionAndUpdateVerifications(
    request: CreateSubmissionAndUpdateVerificationsRequest
  )(implicit hc: HeaderCarrier): Future[CreateSubmissionAndUpdateVerificationsResponse] =
    formpProxyConnector.createSubmissionForVerification(request)

  def updateVerificationSubmission(
    request: UpdateVerificationSubmissionRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateVerificationSubmission(request)

  def processVerificationResponseFromChris(
    request: ProcessVerificationResponseFromChrisRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.processVerificationResponseFromChris(request)

  def getSubmittedVerifications(
    request: GetSubmittedVerificationsRequest
  )(implicit hc: HeaderCarrier): Future[GetSubmittedVerificationsResponse] =
    formpProxyConnector.getSubmittedVerifications(request)
}
