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
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.audit.*
import uk.gov.hmrc.constructionindustryscheme.models.{ChRISSubmission, MonthlyReturnType, SubmissionResult}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisSubmissionRequest, ChrisVerificationRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.utils.Normalise
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.*

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AuditService @Inject (
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext) {

  def monthlyReturnRequestEvent(
    request: ChrisSubmissionRequest,
    correlationId: String,
    submissionDateTime: String
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = MonthlyReturnRequestEvent(
      correlationId = correlationId,
      submissionDateTime = submissionDateTime,
      contractorUtr = request.utr,
      accountsOfficeReference = request.aoReference,
      periodEndDate = ChRISSubmission.parsePeriodEnd(request.monthYear),
      isAgent = request.isAgent,
      isResubmission = request.isResubmission,
      taxOfficeNumber = request.clientTaxOfficeNumber,
      taxOfficeReference = request.clientTaxOfficeRef,
      returnType = request.returnType match {
        case MonthlyReturnType.Standard => "Standard"
        case MonthlyReturnType.Nil      => "Nil"
      },
      isInformationCorrect = Normalise.isYes(request.informationCorrect),
      isInactive = Normalise.isYes(request.inactivity),
      confirmationEmail = request.email,
      subcontractors = request.standard.fold(Seq.empty[MonthlyReturnSubcontractorAuditDetail])(
        _.subcontractors.map { s =>
          MonthlyReturnSubcontractorAuditDetail(
            subcontractorType = s.subcontractorType.toString,
            firstName = s.name.map(_.first),
            middleName = s.name.flatMap(_.middle),
            lastName = s.name.map(_.last),
            tradingName = s.tradingName,
            partnershipTradingName = s.partnershipTradingName,
            utr = s.utr,
            companyRegistrationNumber = s.crn,
            nino = s.nino,
            verificationNumber = s.verificationNumber,
            totalPaymentsMade = s.totalPayments,
            costOfMaterials = s.costOfMaterials,
            totalAmountDeducted = s.totalDeducted
          )
        }
      )
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

  def monthlyReturnResponseEvent(result: SubmissionResult, returnType: String)(implicit
    hc: HeaderCarrier
  ): Future[AuditResult] = {
    val event = MonthlyReturnResponseEvent(
      status = result.status.toString,
      correlationId = result.meta.correlationId,
      returnType = returnType,
      gatewayTimestamp = result.meta.gatewayTimestamp,
      acceptedTime = result.meta.acceptedTime,
      errorNumber = result.meta.error.map(_.errorNumber),
      errorType = result.meta.error.map(_.errorType),
      errorText = result.meta.error.map(_.errorText)
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

  def verificationRequestEvent(
    request: ChrisVerificationRequest,
    correlationId: String
  )(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = VerificationRequestEvent(
      correlationId = correlationId,
      isAgent = request.isAgent,
      taxOfficeNumber = request.clientTaxOfficeNumber,
      taxOfficeReference = request.clientTaxOfficeRef,
      contractorUtr = request.contractorUTR,
      accountsOfficeReference = request.contractorAORef,
      verificationBatchId = request.verificationBatchId,
      emailRecipient = request.emailRecipient,
      subcontractors = request.subcontractors.map { s =>
        VerificationSubcontractorAuditDetail(
          subcontractorType = s.subcontractorType,
          firstName = s.firstName,
          middleName = s.secondName,
          lastName = s.surname,
          tradingName = s.tradingName,
          partnershipTradingName = s.partnershipTradingName,
          utr = s.utr,
          companyRegistrationNumber = s.crn,
          nino = s.nino
        )
      }
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

  def verificationResponseEvent(result: SubmissionResult)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = VerificationResponseEvent(
      status = result.status.toString,
      correlationId = result.meta.correlationId,
      gatewayTimestamp = result.meta.gatewayTimestamp,
      acceptedTime = result.meta.acceptedTime,
      errorNumber = result.meta.error.map(_.errorNumber),
      errorType = result.meta.error.map(_.errorType),
      errorText = result.meta.error.map(_.errorText)
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

  def monthlyReturnPollResponseEvent(response: ChrisPollResponse)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = MonthlyReturnPollResponseEvent(
      status = response.status.toString,
      correlationId = response.correlationId,
      pollUrl = response.pollUrl,
      pollIntervalSeconds = response.pollInterval,
      acceptedTime = response.acceptedTime,
      irMarkReceived = response.irMarkReceived,
      lastMessageDate = response.lastMessageDate,
      error = response.error,
      govTalkErrorStatus = response.govTalkErrorStatus.map(Json.toJson(_))
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

  def verificationPollResponseEvent(response: ChrisPollResponse)(implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = VerificationPollResponseEvent(
      status = response.status.toString,
      correlationId = response.correlationId,
      pollUrl = response.pollUrl,
      pollIntervalSeconds = response.pollInterval,
      acceptedTime = response.acceptedTime,
      irMarkReceived = response.irMarkReceived,
      lastMessageDate = response.lastMessageDate,
      error = response.error,
      govTalkErrorStatus = response.govTalkErrorStatus.map(Json.toJson(_))
    )
    auditConnector.sendExtendedEvent(event.extendedDataEvent)
  }

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
