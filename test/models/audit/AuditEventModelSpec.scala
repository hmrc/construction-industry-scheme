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

package models.audit

import base.SpecBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.{JsObject, JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.models.audit.{ClientListRetrievalFailedEvent, ClientListRetrievalInProgressEvent, MonthlyNilReturnRequestEvent, MonthlyNilReturnResponseEvent, MonthlyReturnPollResponseEvent, MonthlyReturnRequestEvent, MonthlyReturnResponseEvent, MonthlyReturnSubcontractorAuditDetail, VerificationPollResponseEvent, VerificationRequestEvent, VerificationResponseEvent, VerificationSubcontractorAuditDetail}

class AuditEventModelSpec extends SpecBase {

  "MonthlyNilReturnRequestEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = MonthlyNilReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = false,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "MonthlyNilReturnRequest"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting confirmationEmail when absent" in {
      val event = MonthlyNilReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = false,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        isInformationCorrect = true,
        isInactive = true,
        confirmationEmail = None
      )

      val expected = Json.obj(
        "correlationId"           -> "CORR123",
        "submissionDateTime"      -> "2025-05-09T10:30:00Z",
        "contractorUtr"           -> "1234567890",
        "accountsOfficeReference" -> "123/AB456",
        "periodEndDate"           -> "2025-05-05",
        "isAgent"                 -> false,
        "isResubmission"          -> false,
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "AB456",
        "returnType"              -> "Nil",
        "isInformationCorrect"    -> true,
        "isInactive"              -> true
      )

      event.detailJson mustBe expected
    }

    "include confirmationEmail in detail JSON when present" in {
      val event = MonthlyNilReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = false,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = Some("contractor@example.com")
      )

      (event.detailJson \ "confirmationEmail").as[String] mustBe "contractor@example.com"
    }

  }

  "MonthlyNilReturnResponseEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = MonthlyNilReturnResponseEvent(
        status = "ACCEPTED",
        correlationId = "CORR-789",
        gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
        acceptedTime = Some("2025-05-09T10:30:01Z"),
        errorNumber = None,
        errorType = None,
        errorText = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "MonthlyNilReturnResponse"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting optional fields when absent" in {
      val event = MonthlyNilReturnResponseEvent(
        status = "ACCEPTED",
        correlationId = "CORR-789",
        gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
        acceptedTime = Some("2025-05-09T10:30:01Z"),
        errorNumber = None,
        errorType = None,
        errorText = None
      )

      val expected = Json.obj(
        "status"           -> "ACCEPTED",
        "correlationId"    -> "CORR-789",
        "gatewayTimestamp" -> "2025-05-09T10:30:00Z",
        "acceptedTime"     -> "2025-05-09T10:30:01Z"
      )

      event.detailJson mustBe expected
    }

    "include error fields in detail JSON when present" in {
      val event = MonthlyNilReturnResponseEvent(
        status = "FATAL_ERROR",
        correlationId = "CORR-789",
        gatewayTimestamp = None,
        acceptedTime = None,
        errorNumber = Some("1046"),
        errorType = Some("fatal"),
        errorText = Some("submission rejected")
      )

      (event.detailJson \ "status").as[String] mustBe "FATAL_ERROR"
      (event.detailJson \ "errorNumber").as[String] mustBe "1046"
      (event.detailJson \ "errorType").as[String] mustBe "fatal"
      (event.detailJson \ "errorText").as[String] mustBe "submission rejected"
      (event.detailJson \ "gatewayTimestamp").toOption mustBe None
    }
  }

  "MonthlyReturnRequestEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = MonthlyReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = true,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC456",
        returnType = "Nil",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = None,
        subcontractors = Seq.empty
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "MonthlyReturnRequest"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting confirmationEmail when absent" in {
      val event = MonthlyReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = true,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC456",
        returnType = "Nil",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = None,
        subcontractors = Seq.empty
      )

      val expected = Json.obj(
        "correlationId"           -> "CORR123",
        "submissionDateTime"      -> "2025-05-09T10:30:00Z",
        "contractorUtr"           -> "1234567890",
        "accountsOfficeReference" -> "123/AB456",
        "periodEndDate"           -> "2025-05-05",
        "isAgent"                 -> true,
        "isResubmission"          -> false,
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "ABC456",
        "returnType"              -> "Nil",
        "isInformationCorrect"    -> true,
        "isInactive"              -> false
      )

      event.detailJson mustBe expected
    }

    "include confirmationEmail in detail JSON when present" in {
      val event = MonthlyReturnRequestEvent(
        correlationId = "CORR123",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = true,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC456",
        returnType = "Standard",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = Some("contractor@example.com"),
        subcontractors = Seq.empty
      )

      (event.detailJson \ "confirmationEmail").as[String] mustBe "contractor@example.com"
    }

    "produce a flat detail JSON with subcontractors for a standard return" in {
      val subcontractor = MonthlyReturnSubcontractorAuditDetail(
        subcontractorType = "company",
        firstName = None,
        middleName = None,
        lastName = None,
        tradingName = Some("Test Co Ltd"),
        partnershipTradingName = None,
        utr = Some("9876543210"),
        companyRegistrationNumber = None,
        nino = None,
        verificationNumber = None,
        totalPaymentsMade = Some(BigDecimal("5000.00")),
        costOfMaterials = Some(BigDecimal("1000.00")),
        totalAmountDeducted = Some(BigDecimal("800.00"))
      )

      val event = MonthlyReturnRequestEvent(
        correlationId = "CORR456",
        submissionDateTime = "2025-05-09T10:30:00Z",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        periodEndDate = "2025-05-05",
        isAgent = false,
        isResubmission = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC456",
        returnType = "Standard",
        isInformationCorrect = true,
        isInactive = false,
        confirmationEmail = None,
        subcontractors = Seq(subcontractor)
      )

      val detail = event.detailJson

      (detail \ "correlationId").as[String] mustBe "CORR456"
      (detail \ "submissionDateTime").as[String] mustBe "2025-05-09T10:30:00Z"
      (detail \ "contractorUtr").as[String] mustBe "1234567890"
      (detail \ "returnType").as[String] mustBe "Standard"
      (detail \ "isAgent").as[Boolean] mustBe false
      (detail \ "subcontractors").isDefined mustBe true
      val subs = (detail \ "subcontractors").as[Seq[JsObject]]
      subs must have size 1
      (subs.head \ "subcontractorType").as[String] mustBe "company"
      (subs.head \ "tradingName").as[String] mustBe "Test Co Ltd"
      (subs.head \ "utr").as[String] mustBe "9876543210"
      (subs.head \ "totalPaymentsMade").as[BigDecimal] mustBe BigDecimal("5000.00")
      (subs.head \ "firstName").toOption mustBe None
    }
  }

  "MonthlyReturnResponseEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = MonthlyReturnResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-456",
        gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
        acceptedTime = None,
        errorNumber = None,
        errorType = None,
        errorText = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "MonthlyReturnResponse"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting optional fields when absent" in {
      val event = MonthlyReturnResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-456",
        gatewayTimestamp = Some("2025-05-09T10:30:00Z"),
        acceptedTime = None,
        errorNumber = None,
        errorType = None,
        errorText = None
      )

      val expected = Json.obj(
        "status"           -> "SUBMITTED",
        "correlationId"    -> "CORR-456",
        "gatewayTimestamp" -> "2025-05-09T10:30:00Z"
      )

      event.detailJson mustBe expected
    }
  }

  "VerificationRequestEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = VerificationRequestEvent(
        correlationId = "CORR-VER-123",
        isAgent = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        verificationBatchId = "BATCH-001",
        emailRecipient = None,
        subcontractors = Seq.empty
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "VerificationRequest"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting emailRecipient when absent" in {
      val event = VerificationRequestEvent(
        correlationId = "CORR-VER-123",
        isAgent = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        verificationBatchId = "BATCH-001",
        emailRecipient = None,
        subcontractors = Seq.empty
      )

      val expected = Json.obj(
        "correlationId"           -> "CORR-VER-123",
        "isAgent"                 -> false,
        "taxOfficeNumber"         -> "123",
        "taxOfficeReference"      -> "AB456",
        "contractorUtr"           -> "1234567890",
        "accountsOfficeReference" -> "123/AB456",
        "verificationBatchId"     -> "BATCH-001"
      )

      event.detailJson mustBe expected
    }

    "include emailRecipient in detail JSON when present" in {
      val event = VerificationRequestEvent(
        correlationId = "CORR-VER-123",
        isAgent = true,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        verificationBatchId = "BATCH-001",
        emailRecipient = Some("agent@example.com"),
        subcontractors = Seq.empty
      )

      (event.detailJson \ "emailRecipient").as[String] mustBe "agent@example.com"
    }

    "include subcontractors in detail JSON when present" in {
      val sub = VerificationSubcontractorAuditDetail(
        subcontractorType = Some("individual"),
        firstName = Some("Jane"),
        middleName = None,
        lastName = Some("Smith"),
        tradingName = None,
        partnershipTradingName = None,
        utr = Some("9876543210"),
        companyRegistrationNumber = None,
        nino = Some("AB123456C")
      )

      val event = VerificationRequestEvent(
        correlationId = "CORR-VER-456",
        isAgent = false,
        taxOfficeNumber = "123",
        taxOfficeReference = "AB456",
        contractorUtr = "1234567890",
        accountsOfficeReference = "123/AB456",
        verificationBatchId = "BATCH-002",
        emailRecipient = None,
        subcontractors = Seq(sub)
      )

      val detail = event.detailJson
      (detail \ "subcontractors").isDefined mustBe true
      val subs   = (detail \ "subcontractors").as[Seq[JsObject]]
      subs must have size 1
      (subs.head \ "subcontractorType").as[String] mustBe "individual"
      (subs.head \ "firstName").as[String] mustBe "Jane"
      (subs.head \ "nino").as[String] mustBe "AB123456C"
      (subs.head \ "middleName").toOption mustBe None
    }
  }

  "VerificationResponseEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = VerificationResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-VER-789",
        gatewayTimestamp = Some("2025-10-16T13:25:28.720"),
        acceptedTime = None,
        errorNumber = None,
        errorType = None,
        errorText = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "VerificationResponse"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting optional fields when absent" in {
      val event = VerificationResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-VER-789",
        gatewayTimestamp = Some("2025-10-16T13:25:28.720"),
        acceptedTime = None,
        errorNumber = None,
        errorType = None,
        errorText = None
      )

      val expected = Json.obj(
        "status"           -> "SUBMITTED",
        "correlationId"    -> "CORR-VER-789",
        "gatewayTimestamp" -> "2025-10-16T13:25:28.720"
      )

      event.detailJson mustBe expected
    }

    "include error fields in detail JSON when present" in {
      val event = VerificationResponseEvent(
        status = "FATAL_ERROR",
        correlationId = "CORR-VER-789",
        gatewayTimestamp = None,
        acceptedTime = None,
        errorNumber = Some("1046"),
        errorType = Some("fatal"),
        errorText = Some("verification rejected")
      )

      (event.detailJson \ "status").as[String] mustBe "FATAL_ERROR"
      (event.detailJson \ "errorNumber").as[String] mustBe "1046"
      (event.detailJson \ "errorType").as[String] mustBe "fatal"
      (event.detailJson \ "errorText").as[String] mustBe "verification rejected"
    }
  }

  "MonthlyReturnPollResponseEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = MonthlyReturnPollResponseEvent(
        status = "ACCEPTED",
        correlationId = "CORR-POLL-123",
        pollUrl = Some("http://poll.example/123"),
        pollIntervalSeconds = Some(10),
        acceptedTime = Some("2025-05-09T10:30:01Z"),
        irMarkReceived = None,
        lastMessageDate = None,
        error = None,
        govTalkErrorStatus = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "MonthlyReturnPollResponse"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting optional fields when absent" in {
      val event = MonthlyReturnPollResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-POLL-456",
        pollUrl = None,
        pollIntervalSeconds = None,
        acceptedTime = None,
        irMarkReceived = None,
        lastMessageDate = None,
        error = None,
        govTalkErrorStatus = None
      )

      val expected = Json.obj(
        "status"        -> "SUBMITTED",
        "correlationId" -> "CORR-POLL-456"
      )

      event.detailJson mustBe expected
    }

    "include all optional fields in detail JSON when present" in {
      val errorJson              = Json.obj("text" -> "recoverable error")
      val govTalkErrorStatusJson =
        Json.obj("kind" -> "RecoverableError", "errorCode" -> "1001", "errorText" -> "recoverable error")

      val event = MonthlyReturnPollResponseEvent(
        status = "STARTED",
        correlationId = "CORR-POLL-789",
        pollUrl = Some("http://poll.example/789"),
        pollIntervalSeconds = Some(5),
        acceptedTime = Some("2025-05-09T10:30:01Z"),
        irMarkReceived = Some("ABCDEFGHIJ"),
        lastMessageDate = Some("2025-05-09T10:29:00Z"),
        error = Some(errorJson),
        govTalkErrorStatus = Some(govTalkErrorStatusJson)
      )

      (event.detailJson \ "pollUrl").as[String] mustBe "http://poll.example/789"
      (event.detailJson \ "pollIntervalSeconds").as[Int] mustBe 5
      (event.detailJson \ "acceptedTime").as[String] mustBe "2025-05-09T10:30:01Z"
      (event.detailJson \ "irMarkReceived").as[String] mustBe "ABCDEFGHIJ"
      (event.detailJson \ "lastMessageDate").as[String] mustBe "2025-05-09T10:29:00Z"
      (event.detailJson \ "error").as[JsValue] mustBe errorJson
      (event.detailJson \ "govTalkErrorStatus").as[JsValue] mustBe govTalkErrorStatusJson
    }
  }

  "VerificationPollResponseEvent" - {

    "have the correct auditType and auditSource" in {
      val event    = VerificationPollResponseEvent(
        status = "ACCEPTED",
        correlationId = "CORR-VPOLL-123",
        pollUrl = Some("http://poll.example/ver/123"),
        pollIntervalSeconds = Some(10),
        acceptedTime = Some("2025-05-09T10:30:01Z"),
        irMarkReceived = None,
        lastMessageDate = None,
        error = None,
        govTalkErrorStatus = None
      )
      val extended = event.extendedDataEvent
      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "VerificationPollResponse"
      extended.detail      shouldBe event.detailJson
    }

    "produce a flat detail JSON omitting optional fields when absent" in {
      val event = VerificationPollResponseEvent(
        status = "SUBMITTED",
        correlationId = "CORR-VPOLL-456",
        pollUrl = None,
        pollIntervalSeconds = None,
        acceptedTime = None,
        irMarkReceived = None,
        lastMessageDate = None,
        error = None,
        govTalkErrorStatus = None
      )

      val expected = Json.obj(
        "status"        -> "SUBMITTED",
        "correlationId" -> "CORR-VPOLL-456"
      )

      event.detailJson mustBe expected
    }
  }

  "ClientListRetrievalFailedEvent" - {

    "have the correct auditType, auditSource and detail with reason" in {
      val event = ClientListRetrievalFailedEvent(
        credentialId = "cred-123",
        phase = "business#1",
        reason = Some("no-business-intervals")
      )

      val extended = event.extendedDataEvent

      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "ClientListRetrievalFailure"

      val expectedDetail = Json.obj(
        "credentialId" -> "cred-123",
        "phase"        -> "business#1",
        "outcome"      -> "failed",
        "code"         -> "3046",
        "reason"       -> "no-business-intervals"
      )

      extended.detail shouldBe expectedDetail
      extended.detail shouldBe event.detailJson
    }

    "have the correct detail JSON when no reason is provided" in {
      val event = ClientListRetrievalFailedEvent(
        credentialId = "cred-999",
        phase = "browser",
        reason = None
      )

      val expectedDetail = Json.obj(
        "credentialId" -> "cred-999",
        "phase"        -> "browser",
        "outcome"      -> "failed",
        "code"         -> "3046"
      )

      event.detailJson shouldBe expectedDetail
    }

    "serialize and deserialize correctly to/from JSON" in {
      val event = ClientListRetrievalFailedEvent(
        credentialId = "cred-123",
        phase = "business#1",
        reason = Some("initiate-on-final-business-interval")
      )

      val json   = Json.toJson(event)
      val parsed = json.as[ClientListRetrievalFailedEvent]

      parsed shouldBe event
    }
  }

  "ClientListRetrievalInProgressEvent" - {

    "have the correct auditType, auditSource and detail" in {
      val event = ClientListRetrievalInProgressEvent(
        credentialId = "cred-456",
        phase = "browser"
      )

      val extended = event.extendedDataEvent

      extended.auditSource shouldBe "construction-industry-scheme"
      extended.auditType   shouldBe "ClientListRetrievalInProgress"

      val expectedDetail = Json.obj(
        "credentialId" -> "cred-456",
        "phase"        -> "browser",
        "outcome"      -> "in-progress",
        "code"         -> "3008"
      )

      extended.detail shouldBe expectedDetail
      extended.detail shouldBe event.detailJson
    }

    "serialize and deserialize correctly to/from JSON" in {
      val event = ClientListRetrievalInProgressEvent(
        credentialId = "cred-456",
        phase = "business"
      )

      val json   = Json.toJson(event)
      val parsed = json.as[ClientListRetrievalInProgressEvent]

      parsed shouldBe event
    }
  }
}
