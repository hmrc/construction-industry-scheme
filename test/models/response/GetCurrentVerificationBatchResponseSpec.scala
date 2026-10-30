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
import uk.gov.hmrc.constructionindustryscheme.models.response.GetCurrentVerificationBatchResponse

import java.time.LocalDateTime

class GetCurrentVerificationBatchResponseSpec extends AnyWordSpec with Matchers {

  "GetCurrentVerificationBatchResponse Json format" should {

    "read FormP response JSON and parse all sections (including empty cursors)" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractors": [],
          |  "verificationBatch": null,
          |  "verifications": [],
          |  "submission": null
          |}
          |""".stripMargin
      )

      val result = json.validate[GetCurrentVerificationBatchResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.subcontractors mustBe empty
      out.verificationBatch mustBe None
      out.verifications mustBe empty
      out.submission mustBe None
    }

    "read FormP response JSON with an existing submission" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractors": [],
          |  "verificationBatch": {
          |    "verificationBatchId": 99,
          |    "verifBatchResourceRef": 999
          |  },
          |  "verifications": [],
          |  "submission": {
          |    "submissionId": 13602,
          |    "submissionType": "VERIFICATIONS",
          |    "activeObjectId": 99,
          |    "status": "STARTED",
          |    "hmrcMarkGenerated": null,
          |    "hmrcMarkGgis": null,
          |    "emailRecipient": null,
          |    "acceptedTime": null,
          |    "createDate": "2026-07-23T10:15:30",
          |    "lastUpdate": null,
          |    "schemeId": 123,
          |    "agentId": null,
          |    "l_Migrated": null,
          |    "submissionRequestDate": null,
          |    "govTalkErrorCode": null,
          |    "govTalkErrorType": null,
          |    "govTalkErrorMessage": null
          |  }
          |}
          |""".stripMargin
      )

      val result = json.validate[GetCurrentVerificationBatchResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.subcontractors mustBe empty
      out.verificationBatch.map(_.verificationBatchId) mustBe Some(99L)
      out.verifications mustBe empty

      val submission = out.submission.get

      submission.submissionId mustBe 13602L
      submission.submissionType mustBe "VERIFICATIONS"
      submission.activeObjectId mustBe Some(99L)
      submission.status mustBe Some("STARTED")
      submission.createDate mustBe Some(LocalDateTime.parse("2026-07-23T10:15:30"))
      submission.schemeId mustBe 123L
      submission.submissionRequestDate mustBe None
    }

    "write a response to JSON" in {
      val model = GetCurrentVerificationBatchResponse(
        subcontractors = Seq(
          SubcontractorCurrentVerification(
            subcontractorId = 1L,
            subbieResourceRef = Some(10L),
            firstName = Some("John"),
            secondName = None,
            surname = Some("Smith"),
            tradingName = Some("ACME"),
            utr = Some("1111111111"),
            nino = Some("AA123456A"),
            crn = Some("AC012345"),
            partnerUtr = Some("5860920998"),
            partnershipTradingName = Some("ACME trading"),
            subcontractorType = Some("soletrader"),
            addressLine1 = Some("Line 1"),
            addressLine2 = Some("Line 2"),
            addressLine3 = Some("Lane"),
            addressLine4 = Some("Line 4"),
            country = Some("UK"),
            postcode = Some("NE1 1AA"),
            emailAddress = Some("john.smith@test.com"),
            phoneNumber = Some("01911234567"),
            mobilePhoneNumber = Some("07123456789"),
            worksReferenceNumber = Some("WRN123"),
            matched = Some("Y"),
            autoVerified = Some("N"),
            verified = Some("Y"),
            verificationNumber = Some("V123456789"),
            taxTreatment = Some("0"),
            verificationDate = Some(LocalDateTime.parse("2026-07-23T10:15:30")),
            version = Some(1),
            updatedTaxTreatment = Some("1"),
            lastMonthlyReturnDate = Some(LocalDateTime.parse("2026-06-30T00:00:00")),
            pendingVerifications = Some(2)
          )
        ),
        verificationBatch = Some(
          VerificationBatchCurrentVerification(
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(999L)
          )
        ),
        verifications = Seq(
          VerificationCurrentVerification(
            verificationId = 1001L,
            verificationBatchId = Some(99L),
            subcontractorId = Some(1L),
            verificationResourceRef = Some(1L),
            subcontractorName = Some("John Smith"),
            verificationNumber = Some("V123456789"),
            taxTreatment = Some("0"),
            actionIndicator = Some("A"),
            proceed = Some("Y"),
            matched = Some("Y")
          )
        ),
        submission = Some(
          Submission(
            submissionId = 13602L,
            submissionType = "VERIFICATIONS",
            activeObjectId = Some(99L),
            status = Some("STARTED"),
            hmrcMarkGenerated = None,
            hmrcMarkGgis = None,
            emailRecipient = Some("john.smith@test.com"),
            acceptedTime = None,
            createDate = Some(LocalDateTime.parse("2026-07-23T10:15:30")),
            lastUpdate = None,
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)

      val sub0 = (json \ "subcontractors")(0)

      (sub0 \ "subcontractorId").as[Long] mustBe 1L
      (sub0 \ "subbieResourceRef").as[Long] mustBe 10L
      (sub0 \ "firstName").as[String] mustBe "John"
      (sub0 \ "surname").as[String] mustBe "Smith"
      (sub0 \ "secondName").toOption mustBe None
      (sub0 \ "tradingName").as[String] mustBe "ACME"
      (sub0 \ "utr").as[String] mustBe "1111111111"
      (sub0 \ "nino").as[String] mustBe "AA123456A"
      (sub0 \ "crn").as[String] mustBe "AC012345"
      (sub0 \ "partnerUtr").as[String] mustBe "5860920998"
      (sub0 \ "partnershipTradingName").as[String] mustBe "ACME trading"
      (sub0 \ "subcontractorType").as[String] mustBe "soletrader"
      (sub0 \ "addressLine1").as[String] mustBe "Line 1"
      (sub0 \ "addressLine2").as[String] mustBe "Line 2"
      (sub0 \ "addressLine3").as[String] mustBe "Lane"
      (sub0 \ "addressLine4").as[String] mustBe "Line 4"
      (sub0 \ "country").as[String] mustBe "UK"
      (sub0 \ "postcode").as[String] mustBe "NE1 1AA"
      (sub0 \ "worksReferenceNumber").as[String] mustBe "WRN123"
      (sub0 \ "emailAddress").as[String] mustBe "john.smith@test.com"
      (sub0 \ "phoneNumber").as[String] mustBe "01911234567"
      (sub0 \ "mobilePhoneNumber").as[String] mustBe "07123456789"
      (sub0 \ "matched").as[String] mustBe "Y"
      (sub0 \ "autoVerified").as[String] mustBe "N"
      (sub0 \ "verified").as[String] mustBe "Y"
      (sub0 \ "verificationNumber").as[String] mustBe "V123456789"
      (sub0 \ "taxTreatment").as[String] mustBe "0"
      (sub0 \ "verificationDate").as[String] mustBe "2026-07-23T10:15:30"
      (sub0 \ "version").as[Int] mustBe 1
      (sub0 \ "updatedTaxTreatment").as[String] mustBe "1"
      (sub0 \ "lastMonthlyReturnDate").as[String] mustBe "2026-06-30T00:00:00"
      (sub0 \ "pendingVerifications").as[Int] mustBe 2

      val vb0 = json \ "verificationBatch"

      (vb0 \ "verificationBatchId").as[Long] mustBe 99L
      (vb0 \ "verifBatchResourceRef").as[Long] mustBe 999L

      val v0 = (json \ "verifications")(0)

      (v0 \ "verificationId").as[Long] mustBe 1001L
      (v0 \ "verificationBatchId").as[Long] mustBe 99L
      (v0 \ "subcontractorId").as[Long] mustBe 1L
      (v0 \ "verificationResourceRef").as[Long] mustBe 1L
      (v0 \ "subcontractorName").as[String] mustBe "John Smith"
      (v0 \ "verificationNumber").as[String] mustBe "V123456789"
      (v0 \ "taxTreatment").as[String] mustBe "0"
      (v0 \ "actionIndicator").as[String] mustBe "A"
      (v0 \ "proceed").as[String] mustBe "Y"
      (v0 \ "matched").as[String] mustBe "Y"

      val submission = json \ "submission"

      (submission \ "submissionId").as[Long] mustBe 13602L
      (submission \ "submissionType").as[String] mustBe "VERIFICATIONS"
      (submission \ "activeObjectId").as[Long] mustBe 99L
      (submission \ "status").as[String] mustBe "STARTED"
      (submission \ "emailRecipient").as[String] mustBe "john.smith@test.com"
      (submission \ "createDate").as[String] mustBe "2026-07-23T10:15:30"
      (submission \ "schemeId").as[Long] mustBe 123L
      (submission \ "submissionRequestDate").asOpt[LocalDateTime] mustBe None
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetCurrentVerificationBatchResponse(
        subcontractors = Seq.empty,
        verificationBatch = None,
        verifications = Seq.empty,
        submission = None
      )

      val json = Json.toJson(model)
      json.validate[GetCurrentVerificationBatchResponse] mustBe JsSuccess(model)
    }

    "round-trip (model -> json -> model) with an existing submission" in {
      val model = GetCurrentVerificationBatchResponse(
        subcontractors = Seq.empty,
        verificationBatch = Some(
          VerificationBatchCurrentVerification(
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(999L)
          )
        ),
        verifications = Seq.empty,
        submission = Some(
          Submission(
            submissionId = 13602L,
            submissionType = "VERIFICATIONS",
            activeObjectId = Some(99L),
            status = Some("STARTED"),
            hmrcMarkGenerated = None,
            hmrcMarkGgis = None,
            emailRecipient = None,
            acceptedTime = None,
            createDate = Some(LocalDateTime.parse("2026-07-23T10:15:30")),
            lastUpdate = None,
            schemeId = 123L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)
      json.validate[GetCurrentVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }
}
