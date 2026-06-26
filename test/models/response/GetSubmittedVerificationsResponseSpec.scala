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

package models.response

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetSubmittedVerification, GetSubmittedVerificationBatch, GetSubmittedVerificationsResponse}

import java.time.{Instant, LocalDateTime}

final class GetSubmittedVerificationsResponseSpec extends PlaySpec {

  "GetSubmittedVerificationsResponse" should {

    val model = GetSubmittedVerificationsResponse(
      scheme = Seq(
        ContractorScheme(
          schemeId = 123,
          instanceId = "abc-123",
          accountsOfficeReference = "123PA00123456",
          taxOfficeNumber = "123",
          taxOfficeReference = "AB456",
          utr = Some("1234567890"),
          name = Some("Test Contractor"),
          emailAddress = Some("test@test.com"),
          displayWelcomePage = Some("Y"),
          prePopCount = Some(1),
          prePopSuccessful = Some("Y"),
          subcontractorCounter = Some(2),
          verificationBatchCounter = Some(3),
          createDate = Some(Instant.parse("2026-06-15T10:00:00Z")),
          lastUpdate = Some(Instant.parse("2026-06-15T10:05:00Z")),
          version = Some(1)
        )
      ),
      subcontractors = Seq(
        Subcontractor(
          subcontractorId = 999L,
          utr = Some("1234567890"),
          pageVisited = Some(1),
          partnerUtr = Some("0987654321"),
          crn = Some("CRN123"),
          firstName = Some("John"),
          nino = Some("AA123456A"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          partnershipTradingName = Some("Partnership Ltd"),
          tradingName = Some("John Smith Trading"),
          subcontractorType = Some("soletrader"),
          addressLine1 = Some("1 Test Street"),
          addressLine2 = Some("Flat 2"),
          addressLine3 = Some("London"),
          addressLine4 = Some("Greater London"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("sub@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WR-123"),
          createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
          lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
          subbieResourceRef = Some(456L),
          matched = Some("Y"),
          autoVerified = Some("N"),
          verified = Some("Y"),
          verificationNumber = Some("V123456"),
          taxTreatment = Some("NET"),
          verificationDate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
          version = Some(1),
          updatedTaxTreatment = Some("NET"),
          lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-05-15T10:05:00")),
          pendingVerifications = Some(0)
        )
      ),
      verificationBatches = Seq(
        GetSubmittedVerificationBatch(
          verificationBatchId = 99L,
          schemeId = 123L,
          verificationsCounter = Some(1L),
          verifBatchResourceRef = Some(222L),
          proceedSession = Some("Y"),
          confirmArrangement = Some("Y"),
          confirmCorrect = Some("Y"),
          status = Some("SUBMITTED"),
          verificationNumber = Some("VB123"),
          createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
          lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
          version = Some(1)
        )
      ),
      verifications = Seq(
        GetSubmittedVerification(
          verificationId = 1001L,
          matched = Some("Y"),
          verificationNumber = Some("V123456"),
          taxTreatment = Some("NET"),
          actionIndicator = Some("VERIFY"),
          verificationBatchId = Some(99L),
          schemeId = Some(123L),
          subcontractorId = Some(999L),
          subcontractorName = Some("John Smith"),
          verificationResourceRef = Some(456L),
          proceed = Some("Y"),
          createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
          lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
          version = Some(1)
        )
      ),
      submissions = Seq(
        Submission(
          submissionId = 555L,
          submissionType = "VERIFICATIONS",
          activeObjectId = Some(99L),
          status = Some("SUBMITTED"),
          hmrcMarkGenerated = Some("old-irmark"),
          hmrcMarkGgis = Some("irmark"),
          emailRecipient = Some("test@test.com"),
          acceptedTime = Some("2026-06-15T10:05:00Z"),
          createDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
          lastUpdate = Some(LocalDateTime.parse("2026-06-15T10:05:00")),
          schemeId = 123L,
          agentId = Some("agent-123"),
          l_Migrated = Some(0L),
          submissionRequestDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
          govTalkErrorCode = Some("001"),
          govTalkErrorType = Some("business"),
          govTalkErrorMessage = Some("error message")
        )
      )
    )

    "serialize to JSON" in {
      Json.toJson(model) mustBe Json.parse(
        """
          |{
          |  "scheme": [
          |    {
          |      "schemeId": 123,
          |      "instanceId": "abc-123",
          |      "accountsOfficeReference": "123PA00123456",
          |      "taxOfficeNumber": "123",
          |      "taxOfficeReference": "AB456",
          |      "utr": "1234567890",
          |      "name": "Test Contractor",
          |      "emailAddress": "test@test.com",
          |      "displayWelcomePage": "Y",
          |      "prePopCount": 1,
          |      "prePopSuccessful": "Y",
          |      "subcontractorCounter": 2,
          |      "verificationBatchCounter": 3,
          |      "createDate": "2026-06-15T10:00:00Z",
          |      "lastUpdate": "2026-06-15T10:05:00Z",
          |      "version": 1
          |    }
          |  ],
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 999,
          |      "utr": "1234567890",
          |      "pageVisited": 1,
          |      "partnerUtr": "0987654321",
          |      "crn": "CRN123",
          |      "firstName": "John",
          |      "nino": "AA123456A",
          |      "secondName": "Q",
          |      "surname": "Smith",
          |      "partnershipTradingName": "Partnership Ltd",
          |      "tradingName": "John Smith Trading",
          |      "subcontractorType": "soletrader",
          |      "addressLine1": "1 Test Street",
          |      "addressLine2": "Flat 2",
          |      "addressLine3": "London",
          |      "addressLine4": "Greater London",
          |      "country": "United Kingdom",
          |      "postcode": "AA1 1AA",
          |      "emailAddress": "sub@test.com",
          |      "phoneNumber": "01234567890",
          |      "mobilePhoneNumber": "07123456789",
          |      "worksReferenceNumber": "WR-123",
          |      "createDate": "2026-06-15T10:00:00",
          |      "lastUpdate": "2026-06-15T10:05:00",
          |      "subbieResourceRef": 456,
          |      "matched": "Y",
          |      "autoVerified": "N",
          |      "verified": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verificationDate": "2026-06-15T10:05:00",
          |      "version": 1,
          |      "updatedTaxTreatment": "NET",
          |      "lastMonthlyReturnDate": "2026-05-15T10:05:00",
          |      "pendingVerifications": 0,
          |      "displayName": "John Smith"
          |    }
          |  ],
          |  "verificationBatches": [
          |    {
          |      "verificationBatchId": 99,
          |      "schemeId": 123,
          |      "verificationsCounter": 1,
          |      "verifBatchResourceRef": 222,
          |      "proceedSession": "Y",
          |      "confirmArrangement": "Y",
          |      "confirmCorrect": "Y",
          |      "status": "SUBMITTED",
          |      "verificationNumber": "VB123",
          |      "createDate": "2026-06-15T10:00:00",
          |      "lastUpdate": "2026-06-15T10:05:00",
          |      "version": 1
          |    }
          |  ],
          |  "verifications": [
          |    {
          |      "verificationId": 1001,
          |      "matched": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "actionIndicator": "VERIFY",
          |      "verificationBatchId": 99,
          |      "schemeId": 123,
          |      "subcontractorId": 999,
          |      "subcontractorName": "John Smith",
          |      "verificationResourceRef": 456,
          |      "proceed": "Y",
          |      "createDate": "2026-06-15T10:00:00",
          |      "lastUpdate": "2026-06-15T10:05:00",
          |      "version": 1
          |    }
          |  ],
          |  "submissions": [
          |    {
          |      "submissionId": 555,
          |      "submissionType": "VERIFICATIONS",
          |      "activeObjectId": 99,
          |      "status": "SUBMITTED",
          |      "hmrcMarkGenerated": "old-irmark",
          |      "hmrcMarkGgis": "irmark",
          |      "emailRecipient": "test@test.com",
          |      "acceptedTime": "2026-06-15T10:05:00Z",
          |      "createDate": "2026-06-15T10:00:00",
          |      "lastUpdate": "2026-06-15T10:05:00",
          |      "schemeId": 123,
          |      "agentId": "agent-123",
          |      "l_Migrated": 0,
          |      "submissionRequestDate": "2026-06-15T10:00:00",
          |      "govTalkErrorCode": "001",
          |      "govTalkErrorType": "business",
          |      "govTalkErrorMessage": "error message"
          |    }
          |  ]
          |}
          |""".stripMargin
      )
    }

    "deserialize from JSON" in {
      val json = Json.toJson(model)

      json.validate[GetSubmittedVerificationsResponse] mustBe JsSuccess(model)
    }

    "fail to deserialize when required fields are missing" in {
      Json
        .obj("scheme" -> Json.arr())
        .validate[GetSubmittedVerificationsResponse]
        .isError mustBe true
    }
  }
}
