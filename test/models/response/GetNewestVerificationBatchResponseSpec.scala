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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.GetNewestVerificationBatchResponse

import java.time.{Instant, LocalDateTime}

final class GetNewestVerificationBatchResponseSpec extends AnyWordSpec with Matchers {

  "GetNewestVerificationBatchResponse Json format" should {

    "read FormP response JSON and parse all sections (including empty cursors)" in {
      val json = Json.parse(
        """
          |{
          |  "scheme": [],
          |  "subcontractors": [],
          |  "verificationBatch": [],
          |  "verifications": [],
          |  "submission": [],
          |  "monthlyReturn": [],
          |  "monthlyReturnSubmission": []
          |}
          |""".stripMargin
      )

      val result = json.validate[GetNewestVerificationBatchResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.scheme mustBe empty
      out.subcontractors mustBe empty
      out.verificationBatch mustBe empty
      out.verifications mustBe empty
      out.submission mustBe empty
      out.monthlyReturn mustBe empty
      out.monthlyReturnSubmission mustBe empty
    }

    "write a response to JSON" in {
      val model = GetNewestVerificationBatchResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 123,
            instanceId = "abc-123",
            accountsOfficeReference = "123PA00123456",
            taxOfficeNumber = "163",
            taxOfficeReference = "AB0063",
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com"),
            displayWelcomePage = Some("Y"),
            prePopCount = Some(5),
            prePopSuccessful = Some("Y"),
            subcontractorCounter = Some(10),
            verificationBatchCounter = Some(2),
            createDate = Some(Instant.parse("2026-01-01T10:00:00Z")),
            lastUpdate = Some(Instant.parse("2026-01-01T10:00:00Z")),
            version = Some(1)
          )
        ),
        subcontractors = Seq(
          Subcontractor(
            subcontractorId = 1L,
            utr = Some("1111111111"),
            pageVisited = Some(2),
            partnerUtr = None,
            crn = None,
            firstName = Some("John"),
            nino = Some("AA123456A"),
            secondName = None,
            surname = Some("Smith"),
            partnershipTradingName = None,
            tradingName = Some("ACME"),
            subcontractorType = Some("soletrader"),
            addressLine1 = Some("1 Main Street"),
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = Some("United Kingdom"),
            postcode = Some("AA1 1AA"),
            emailAddress = Some("test@test.com"),
            phoneNumber = Some("01234567890"),
            mobilePhoneNumber = Some("07123456789"),
            worksReferenceNumber = Some("WRN-123"),
            createDate = Some(LocalDateTime.of(2026, 1, 10, 9, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 11, 10, 0, 0)),
            subbieResourceRef = Some(10L),
            matched = Some("Y"),
            autoVerified = Some("N"),
            verified = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationDate = Some(LocalDateTime.of(2026, 1, 12, 11, 0, 0)),
            version = Some(1),
            updatedTaxTreatment = None,
            lastMonthlyReturnDate = None,
            pendingVerifications = Some(0)
          )
        ),
        verificationBatch = Seq(
          VerificationBatch(
            verificationBatchId = 99L,
            schemeId = 123L,
            verificationsCounter = Some(1),
            verifBatchResourceRef = Some(7L),
            proceedSession = Some("Y"),
            confirmArrangement = Some("Y"),
            confirmCorrect = Some("Y"),
            status = Some("STARTED"),
            verificationNumber = Some("V0000000001"),
            createDate = Some(LocalDateTime.of(2026, 1, 12, 11, 30, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 12, 12, 0, 0)),
            version = Some(1)
          )
        ),
        verifications = Seq(
          Verification(
            verificationId = 1001L,
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            actionIndicator = Some("A"),
            verificationBatchId = Some(99L),
            schemeId = Some(123L),
            subcontractorId = Some(1L),
            subcontractorName = Some("ACME"),
            verificationResourceRef = Some(1L),
            proceed = Some("Y"),
            createDate = Some(LocalDateTime.of(2026, 1, 12, 11, 35, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 12, 11, 45, 0)),
            version = Some(1)
          )
        ),
        submission = Seq(
          Submission(
            submissionId = 555L,
            submissionType = "VERIFICATIONS",
            activeObjectId = Some(99L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("mark-gen"),
            hmrcMarkGgis = None,
            emailRecipient = Some("ops@example.com"),
            acceptedTime = Some("12:00:00"),
            createDate = Some(LocalDateTime.of(2026, 1, 12, 12, 1, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 12, 12, 2, 0)),
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = Some(LocalDateTime.of(2026, 1, 12, 11, 59, 0)),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        ),
        monthlyReturn = Seq(
          MonthlyReturn(
            monthlyReturnId = 777L,
            taxYear = 2025,
            taxMonth = 1,
            nilReturnIndicator = Some("N"),
            decEmpStatusConsidered = Some("Y"),
            decAllSubsVerified = Some("Y"),
            decInformationCorrect = Some("Y"),
            decNoMoreSubPayments = Some("N"),
            decNilReturnNoPayments = Some("N"),
            status = Some("SUBMITTED"),
            lastUpdate = Some(LocalDateTime.of(2026, 1, 31, 12, 34, 56)),
            amendment = Some("N"),
            supersededBy = None
          )
        ),
        monthlyReturnSubmission = Seq(
          Submission(
            submissionId = 556L,
            submissionType = "MONTHLY_RETURN",
            activeObjectId = Some(777L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("mark-gen-mr"),
            hmrcMarkGgis = None,
            emailRecipient = Some("ops@example.com"),
            acceptedTime = Some("12:01:00"),
            createDate = Some(LocalDateTime.of(2026, 2, 1, 9, 0, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 2, 1, 9, 1, 0)),
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = Some(LocalDateTime.of(2026, 2, 1, 8, 59, 0)),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)

      val scheme0 = (json \ "scheme")(0)

      (scheme0 \ "schemeId").as[Int] mustBe 123
      (scheme0 \ "instanceId").as[String] mustBe "abc-123"
      (scheme0 \ "accountsOfficeReference").as[String] mustBe "123PA00123456"
      (scheme0 \ "taxOfficeNumber").as[String] mustBe "163"
      (scheme0 \ "taxOfficeReference").as[String] mustBe "AB0063"
      (scheme0 \ "utr").as[String] mustBe "1111111111"
      (scheme0 \ "name").as[String] mustBe "ABC Construction Ltd"
      (scheme0 \ "emailAddress").as[String] mustBe "ops@example.com"
      (scheme0 \ "displayWelcomePage").as[String] mustBe "Y"
      (scheme0 \ "prePopCount").as[Int] mustBe 5
      (scheme0 \ "prePopSuccessful").as[String] mustBe "Y"
      (scheme0 \ "subcontractorCounter").as[Int] mustBe 10
      (scheme0 \ "verificationBatchCounter").as[Int] mustBe 2
      (scheme0 \ "createDate").as[String] mustBe "2026-01-01T10:00:00Z"
      (scheme0 \ "lastUpdate").as[String] mustBe "2026-01-01T10:00:00Z"
      (scheme0 \ "version").as[Int] mustBe 1

      val sub0 = (json \ "subcontractors")(0)

      (sub0 \ "subcontractorId").as[Long] mustBe 1L
      (sub0 \ "utr").as[String] mustBe "1111111111"
      (sub0 \ "pageVisited").as[Int] mustBe 2
      (sub0 \ "partnerUtr").toOption mustBe None
      (sub0 \ "crn").toOption mustBe None
      (sub0 \ "firstName").as[String] mustBe "John"
      (sub0 \ "nino").as[String] mustBe "AA123456A"
      (sub0 \ "secondName").toOption mustBe None
      (sub0 \ "surname").as[String] mustBe "Smith"
      (sub0 \ "partnershipTradingName").toOption mustBe None
      (sub0 \ "tradingName").as[String] mustBe "ACME"
      (sub0 \ "subcontractorType").as[String] mustBe "soletrader"
      (sub0 \ "addressLine1").as[String] mustBe "1 Main Street"
      (sub0 \ "addressLine2").toOption mustBe None
      (sub0 \ "addressLine3").toOption mustBe None
      (sub0 \ "addressLine4").toOption mustBe None
      (sub0 \ "country").as[String] mustBe "United Kingdom"
      (sub0 \ "postcode").as[String] mustBe "AA1 1AA"
      (sub0 \ "emailAddress").as[String] mustBe "test@test.com"
      (sub0 \ "phoneNumber").as[String] mustBe "01234567890"
      (sub0 \ "mobilePhoneNumber").as[String] mustBe "07123456789"
      (sub0 \ "worksReferenceNumber").as[String] mustBe "WRN-123"
      (sub0 \ "createDate").as[String] mustBe "2026-01-10T09:00:00"
      (sub0 \ "lastUpdate").as[String] mustBe "2026-01-11T10:00:00"
      (sub0 \ "subbieResourceRef").as[Long] mustBe 10L
      (sub0 \ "matched").as[String] mustBe "Y"
      (sub0 \ "autoVerified").as[String] mustBe "N"
      (sub0 \ "verified").as[String] mustBe "Y"
      (sub0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (sub0 \ "taxTreatment").as[String] mustBe "0"
      (sub0 \ "verificationDate").as[String] mustBe "2026-01-12T11:00:00"
      (sub0 \ "version").as[Int] mustBe 1
      (sub0 \ "updatedTaxTreatment").toOption mustBe None
      (sub0 \ "lastMonthlyReturnDate").toOption mustBe None
      (sub0 \ "pendingVerifications").as[Int] mustBe 0

      val vb0 = (json \ "verificationBatch")(0)

      (vb0 \ "verificationBatchId").as[Long] mustBe 99L
      (vb0 \ "schemeId").as[Long] mustBe 123L
      (vb0 \ "verificationsCounter").as[Int] mustBe 1
      (vb0 \ "verifBatchResourceRef").as[Long] mustBe 7L
      (vb0 \ "proceedSession").as[String] mustBe "Y"
      (vb0 \ "confirmArrangement").as[String] mustBe "Y"
      (vb0 \ "confirmCorrect").as[String] mustBe "Y"
      (vb0 \ "status").as[String] mustBe "STARTED"
      (vb0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (vb0 \ "createDate").as[String] mustBe "2026-01-12T11:30:00"
      (vb0 \ "lastUpdate").as[String] mustBe "2026-01-12T12:00:00"
      (vb0 \ "version").as[Int] mustBe 1

      val v0 = (json \ "verifications")(0)

      (v0 \ "verificationId").as[Long] mustBe 1001L
      (v0 \ "matched").as[String] mustBe "Y"
      (v0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (v0 \ "taxTreatment").as[String] mustBe "0"
      (v0 \ "actionIndicator").as[String] mustBe "A"
      (v0 \ "verificationBatchId").as[Long] mustBe 99L
      (v0 \ "schemeId").as[Long] mustBe 123L
      (v0 \ "subcontractorId").as[Long] mustBe 1L
      (v0 \ "subcontractorName").as[String] mustBe "ACME"
      (v0 \ "verificationResourceRef").as[Long] mustBe 1L
      (v0 \ "proceed").as[String] mustBe "Y"
      (v0 \ "createDate").as[String] mustBe "2026-01-12T11:35:00"
      (v0 \ "lastUpdate").as[String] mustBe "2026-01-12T11:45:00"
      (v0 \ "version").as[Int] mustBe 1

      val subm0 = (json \ "submission")(0)

      (subm0 \ "submissionId").as[Long] mustBe 555L
      (subm0 \ "submissionType").as[String] mustBe "VERIFICATIONS"
      (subm0 \ "activeObjectId").as[Long] mustBe 99L
      (subm0 \ "status").as[String] mustBe "ACCEPTED"
      (subm0 \ "hmrcMarkGenerated").as[String] mustBe "mark-gen"
      (subm0 \ "hmrcMarkGgis").toOption mustBe None
      (subm0 \ "emailRecipient").as[String] mustBe "ops@example.com"
      (subm0 \ "acceptedTime").as[String] mustBe "12:00:00"
      (subm0 \ "createDate").as[String] mustBe "2026-01-12T12:01:00"
      (subm0 \ "lastUpdate").as[String] mustBe "2026-01-12T12:02:00"
      (subm0 \ "schemeId").as[Long] mustBe 123L
      (subm0 \ "agentId").toOption mustBe None
      (subm0 \ "l_Migrated").toOption mustBe None
      (subm0 \ "submissionRequestDate").as[String] mustBe "2026-01-12T11:59:00"
      (subm0 \ "govTalkErrorCode").toOption mustBe None
      (subm0 \ "govTalkErrorType").toOption mustBe None
      (subm0 \ "govTalkErrorMessage").toOption mustBe None

      val mr0 = (json \ "monthlyReturn")(0)

      (mr0 \ "monthlyReturnId").as[Long] mustBe 777L
      (mr0 \ "taxYear").as[Int] mustBe 2025
      (mr0 \ "taxMonth").as[Int] mustBe 1
      (mr0 \ "nilReturnIndicator").as[String] mustBe "N"
      (mr0 \ "decEmpStatusConsidered").as[String] mustBe "Y"
      (mr0 \ "decAllSubsVerified").as[String] mustBe "Y"
      (mr0 \ "decInformationCorrect").as[String] mustBe "Y"
      (mr0 \ "decNoMoreSubPayments").as[String] mustBe "N"
      (mr0 \ "decNilReturnNoPayments").as[String] mustBe "N"
      (mr0 \ "status").as[String] mustBe "SUBMITTED"
      (mr0 \ "lastUpdate").as[String] mustBe "2026-01-31T12:34:56"
      (mr0 \ "amendment").as[String] mustBe "N"
      (mr0 \ "supersededBy").toOption mustBe None

      val mrs0 = (json \ "monthlyReturnSubmission")(0)

      (mrs0 \ "submissionId").as[Long] mustBe 556L
      (mrs0 \ "submissionType").as[String] mustBe "MONTHLY_RETURN"
      (mrs0 \ "activeObjectId").as[Long] mustBe 777L
      (mrs0 \ "status").as[String] mustBe "ACCEPTED"
      (mrs0 \ "hmrcMarkGenerated").as[String] mustBe "mark-gen-mr"
      (mrs0 \ "hmrcMarkGgis").toOption mustBe None
      (mrs0 \ "emailRecipient").as[String] mustBe "ops@example.com"
      (mrs0 \ "acceptedTime").as[String] mustBe "12:01:00"
      (mrs0 \ "createDate").as[String] mustBe "2026-02-01T09:00:00"
      (mrs0 \ "lastUpdate").as[String] mustBe "2026-02-01T09:01:00"
      (mrs0 \ "schemeId").as[Long] mustBe 123L
      (mrs0 \ "agentId").toOption mustBe None
      (mrs0 \ "l_Migrated").toOption mustBe None
      (mrs0 \ "submissionRequestDate").as[String] mustBe "2026-02-01T08:59:00"
      (mrs0 \ "govTalkErrorCode").toOption mustBe None
      (mrs0 \ "govTalkErrorType").toOption mustBe None
      (mrs0 \ "govTalkErrorMessage").toOption mustBe None
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetNewestVerificationBatchResponse(
        scheme = Seq(
          ContractorScheme(
            schemeId = 999,
            instanceId = "roundtrip-1",
            accountsOfficeReference = "123PA00123456",
            taxOfficeNumber = "163",
            taxOfficeReference = "AB0063",
            version = Some(1)
          )
        ),
        subcontractors = Seq.empty,
        verificationBatch = Seq.empty,
        verifications = Seq.empty,
        submission = Seq.empty,
        monthlyReturn = Seq.empty,
        monthlyReturnSubmission = Seq.empty
      )

      val json = Json.toJson(model)
      json.validate[GetNewestVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }
}
