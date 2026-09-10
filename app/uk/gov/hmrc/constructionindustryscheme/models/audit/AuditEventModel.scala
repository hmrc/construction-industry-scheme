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

package uk.gov.hmrc.constructionindustryscheme.models.audit

import play.api.libs.json.{Format, JsArray, JsObject, JsValue, Json}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

trait AuditEventModel {
  private val auditSource: String = "construction-industry-scheme"
  val auditType: String
  val detailJson: JsValue

  def extendedDataEvent: ExtendedDataEvent =
    ExtendedDataEvent(auditSource = auditSource, auditType = auditType, detail = detailJson)
}

final case class MonthlyReturnSubcontractorAuditDetail(
  subcontractorType: String,
  firstName: Option[String],
  middleName: Option[String],
  lastName: Option[String],
  tradingName: Option[String],
  partnershipTradingName: Option[String],
  utr: Option[String],
  companyRegistrationNumber: Option[String],
  nino: Option[String],
  verificationNumber: Option[String],
  totalPaymentsMade: Option[BigDecimal],
  costOfMaterials: Option[BigDecimal],
  totalAmountDeducted: Option[BigDecimal]
) {
  def toJson: JsObject =
    Json.obj("subcontractorType" -> subcontractorType) ++
      firstName.fold(Json.obj())(v => Json.obj("firstName" -> v)) ++
      middleName.fold(Json.obj())(v => Json.obj("middleName" -> v)) ++
      lastName.fold(Json.obj())(v => Json.obj("lastName" -> v)) ++
      tradingName.fold(Json.obj())(v => Json.obj("tradingName" -> v)) ++
      partnershipTradingName.fold(Json.obj())(v => Json.obj("partnershipTradingName" -> v)) ++
      utr.fold(Json.obj())(v => Json.obj("utr" -> v)) ++
      companyRegistrationNumber.fold(Json.obj())(v => Json.obj("companyRegistrationNumber" -> v)) ++
      nino.fold(Json.obj())(v => Json.obj("nino" -> v)) ++
      verificationNumber.fold(Json.obj())(v => Json.obj("verificationNumber" -> v)) ++
      totalPaymentsMade.fold(Json.obj())(v => Json.obj("totalPaymentsMade" -> v)) ++
      costOfMaterials.fold(Json.obj())(v => Json.obj("costOfMaterials" -> v)) ++
      totalAmountDeducted.fold(Json.obj())(v => Json.obj("totalAmountDeducted" -> v))
}

final case class MonthlyReturnRequestEvent(
  correlationId: String,
  submissionDateTime: String,
  contractorUtr: String,
  accountsOfficeReference: String,
  periodEndDate: String,
  isAgent: Boolean,
  isResubmission: Boolean,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  returnType: String,
  isInformationCorrect: Boolean,
  isInactive: Boolean,
  confirmationEmail: Option[String],
  subcontractors: Seq[MonthlyReturnSubcontractorAuditDetail]
) extends AuditEventModel {
  override val auditType: String   = "MonthlyReturnRequest"
  override val detailJson: JsValue = {
    val base = Json.obj(
      "correlationId"           -> correlationId,
      "submissionDateTime"      -> submissionDateTime,
      "contractorUtr"           -> contractorUtr,
      "accountsOfficeReference" -> accountsOfficeReference,
      "periodEndDate"           -> periodEndDate,
      "isAgent"                 -> isAgent,
      "isResubmission"          -> isResubmission,
      "taxOfficeNumber"         -> taxOfficeNumber,
      "taxOfficeReference"      -> taxOfficeReference,
      "returnType"              -> returnType,
      "isInformationCorrect"    -> isInformationCorrect,
      "isInactive"              -> isInactive
    ) ++ confirmationEmail.fold(Json.obj())(e => Json.obj("confirmationEmail" -> e))
    if (subcontractors.isEmpty) base
    else base ++ Json.obj("subcontractors" -> JsArray(subcontractors.map(_.toJson)))
  }
}

final case class MonthlyReturnResponseEvent(
  status: String,
  correlationId: String,
  returnType: String,
  gatewayTimestamp: Option[String],
  acceptedTime: Option[String],
  errorNumber: Option[String],
  errorType: Option[String],
  errorText: Option[String]
) extends AuditEventModel {
  override val auditType: String   = "MonthlyReturnResponse"
  override val detailJson: JsValue =
    Json.obj(
      "status"        -> status,
      "correlationId" -> correlationId,
      "returnType"    -> returnType
    ) ++
      gatewayTimestamp.fold(Json.obj())(v => Json.obj("gatewayTimestamp" -> v)) ++
      acceptedTime.fold(Json.obj())(v => Json.obj("acceptedTime" -> v)) ++
      errorNumber.fold(Json.obj())(v => Json.obj("errorNumber" -> v)) ++
      errorType.fold(Json.obj())(v => Json.obj("errorType" -> v)) ++
      errorText.fold(Json.obj())(v => Json.obj("errorText" -> v))
}

final case class VerificationSubcontractorAuditDetail(
  subcontractorType: Option[String],
  firstName: Option[String],
  middleName: Option[String],
  lastName: Option[String],
  tradingName: Option[String],
  partnershipTradingName: Option[String],
  utr: Option[String],
  companyRegistrationNumber: Option[String],
  nino: Option[String]
) {
  def toJson: JsObject =
    subcontractorType.fold(Json.obj())(v => Json.obj("subcontractorType" -> v)) ++
      firstName.fold(Json.obj())(v => Json.obj("firstName" -> v)) ++
      middleName.fold(Json.obj())(v => Json.obj("middleName" -> v)) ++
      lastName.fold(Json.obj())(v => Json.obj("lastName" -> v)) ++
      tradingName.fold(Json.obj())(v => Json.obj("tradingName" -> v)) ++
      partnershipTradingName.fold(Json.obj())(v => Json.obj("partnershipTradingName" -> v)) ++
      utr.fold(Json.obj())(v => Json.obj("utr" -> v)) ++
      companyRegistrationNumber.fold(Json.obj())(v => Json.obj("companyRegistrationNumber" -> v)) ++
      nino.fold(Json.obj())(v => Json.obj("nino" -> v))
}

final case class VerificationRequestEvent(
  correlationId: String,
  isAgent: Boolean,
  taxOfficeNumber: String,
  taxOfficeReference: String,
  contractorUtr: String,
  accountsOfficeReference: String,
  verificationBatchId: String,
  emailRecipient: Option[String],
  subcontractors: Seq[VerificationSubcontractorAuditDetail]
) extends AuditEventModel {
  override val auditType: String   = "VerificationRequest"
  override val detailJson: JsValue = {
    val base = Json.obj(
      "correlationId"           -> correlationId,
      "isAgent"                 -> isAgent,
      "taxOfficeNumber"         -> taxOfficeNumber,
      "taxOfficeReference"      -> taxOfficeReference,
      "contractorUtr"           -> contractorUtr,
      "accountsOfficeReference" -> accountsOfficeReference,
      "verificationBatchId"     -> verificationBatchId
    ) ++ emailRecipient.fold(Json.obj())(v => Json.obj("emailRecipient" -> v))
    if (subcontractors.isEmpty) base
    else base ++ Json.obj("subcontractors" -> JsArray(subcontractors.map(_.toJson)))
  }
}

final case class VerificationResponseEvent(
  status: String,
  correlationId: String,
  gatewayTimestamp: Option[String],
  acceptedTime: Option[String],
  errorNumber: Option[String],
  errorType: Option[String],
  errorText: Option[String]
) extends AuditEventModel {
  override val auditType: String   = "VerificationResponse"
  override val detailJson: JsValue =
    Json.obj(
      "status"        -> status,
      "correlationId" -> correlationId
    ) ++
      gatewayTimestamp.fold(Json.obj())(v => Json.obj("gatewayTimestamp" -> v)) ++
      acceptedTime.fold(Json.obj())(v => Json.obj("acceptedTime" -> v)) ++
      errorNumber.fold(Json.obj())(v => Json.obj("errorNumber" -> v)) ++
      errorType.fold(Json.obj())(v => Json.obj("errorType" -> v)) ++
      errorText.fold(Json.obj())(v => Json.obj("errorText" -> v))
}

final case class MonthlyReturnPollResponseEvent(
  status: String,
  correlationId: String,
  pollUrl: Option[String],
  pollIntervalSeconds: Option[Int],
  acceptedTime: Option[String],
  irMarkReceived: Option[String],
  lastMessageDate: Option[String],
  error: Option[JsValue],
  govTalkErrorStatus: Option[JsValue]
) extends AuditEventModel {
  override val auditType: String   = "MonthlyReturnPollResponse"
  override val detailJson: JsValue =
    Json.obj(
      "status"        -> status,
      "correlationId" -> correlationId
    ) ++
      pollUrl.fold(Json.obj())(v => Json.obj("pollUrl" -> v)) ++
      pollIntervalSeconds.fold(Json.obj())(v => Json.obj("pollIntervalSeconds" -> v)) ++
      acceptedTime.fold(Json.obj())(v => Json.obj("acceptedTime" -> v)) ++
      irMarkReceived.fold(Json.obj())(v => Json.obj("irMarkReceived" -> v)) ++
      lastMessageDate.fold(Json.obj())(v => Json.obj("lastMessageDate" -> v)) ++
      error.fold(Json.obj())(v => Json.obj("error" -> v)) ++
      govTalkErrorStatus.fold(Json.obj())(v => Json.obj("govTalkErrorStatus" -> v))
}

final case class VerificationPollResponseEvent(
  status: String,
  correlationId: String,
  pollUrl: Option[String],
  pollIntervalSeconds: Option[Int],
  acceptedTime: Option[String],
  irMarkReceived: Option[String],
  lastMessageDate: Option[String],
  error: Option[JsValue],
  govTalkErrorStatus: Option[JsValue]
) extends AuditEventModel {
  override val auditType: String   = "VerificationPollResponse"
  override val detailJson: JsValue =
    Json.obj(
      "status"        -> status,
      "correlationId" -> correlationId
    ) ++
      pollUrl.fold(Json.obj())(v => Json.obj("pollUrl" -> v)) ++
      pollIntervalSeconds.fold(Json.obj())(v => Json.obj("pollIntervalSeconds" -> v)) ++
      acceptedTime.fold(Json.obj())(v => Json.obj("acceptedTime" -> v)) ++
      irMarkReceived.fold(Json.obj())(v => Json.obj("irMarkReceived" -> v)) ++
      lastMessageDate.fold(Json.obj())(v => Json.obj("lastMessageDate" -> v)) ++
      error.fold(Json.obj())(v => Json.obj("error" -> v)) ++
      govTalkErrorStatus.fold(Json.obj())(v => Json.obj("govTalkErrorStatus" -> v))
}

final case class ClientListRetrievalFailedEvent(
  credentialId: String,
  phase: String,
  reason: Option[String] = None,
  code: String = "3046"
) extends AuditEventModel:
  override val auditType: String   = "ClientListRetrievalFailure"
  override val detailJson: JsValue =
    Json.obj(
      "credentialId" -> credentialId,
      "phase"        -> phase,
      "outcome"      -> "failed",
      "code"         -> code
    ) ++ reason.fold(Json.obj())(r => Json.obj("reason" -> r))

final case class ClientListRetrievalInProgressEvent(
  credentialId: String,
  phase: String,
  code: String = "3008"
) extends AuditEventModel:
  override val auditType: String   = "ClientListRetrievalInProgress"
  override val detailJson: JsValue =
    Json.obj(
      "credentialId" -> credentialId,
      "phase"        -> phase,
      "outcome"      -> "in-progress",
      "code"         -> code
    )

object ClientListRetrievalFailedEvent:
  given Format[ClientListRetrievalFailedEvent] = Json.format[ClientListRetrievalFailedEvent]

object ClientListRetrievalInProgressEvent:
  given Format[ClientListRetrievalInProgressEvent] = Json.format[ClientListRetrievalInProgressEvent]
