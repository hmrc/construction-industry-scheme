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
import uk.gov.hmrc.constructionindustryscheme.models.response.GetLastSubmittedVerificationBatchResponse

import java.time.LocalDateTime

final class GetLastSubmittedVerificationBatchResponseSpec extends AnyWordSpec with Matchers {

  "GetLastSubmittedVerificationBatchResponse Json format" should {

    "read FormP response JSON and parse all sections (including empty cursors)" in {
      val json = Json.parse(
        """
          |{
          |  "scheme": null,
          |  "subcontractors": [],
          |  "verificationBatch": null,
          |  "verifications": [],
          |  "submission": null
          |}
          |""".stripMargin
      )

      val result = json.validate[GetLastSubmittedVerificationBatchResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get

      out.scheme mustBe None
      out.subcontractors mustBe empty
      out.verificationBatch mustBe None
      out.verifications mustBe empty
      out.submission mustBe None
    }

    "write a response to JSON" in {
      val model = GetLastSubmittedVerificationBatchResponse(
        scheme = Some(
          ContractorSchemeLastVerification(
            accountsOfficeReference = Some("123PA00123456"),
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com")
          )
        ),
        subcontractors = Seq(
          SubcontractorLastVerification(
            subcontractorId = 1L,
            subcontractorType = Some("soletrader"),
            subbieResourceRef = Some(10L),
            utr = Some("1111111111")
          )
        ),
        verificationBatch = Some(
          VerificationBatchLastVerification(
            verificationBatchId = Some(99L),
            verifBatchResourceRef = Some(1234567L),
            verificationBatchStatus = Some("ACCEPTED")
          )
        ),
        verifications = Seq(
          VerificationLastVerification(
            verificationId = 1001L,
            verificationBatchId = Some(99L),
            verificationResourceRef = Some(12345),
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            subcontractorName = Some("James Star")
          )
        ),
        submission = Some(
          Submission(
            submissionId = 1234L,
            submissionType = "CT600",
            activeObjectId = Some(98765L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("20260811115300"),
            hmrcMarkGgis = Some("ABC123XYZ456"),
            emailRecipient = Some("test@example.com"),
            acceptedTime = Some("2026-08-11T11:53:00"),
            createDate = Some(LocalDateTime.of(2026, 8, 11, 11, 45, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 8, 11, 11, 53, 0)),
            schemeId = 456789L,
            agentId = Some("AGENT123"),
            l_Migrated = Some(0L),
            submissionRequestDate = Some(
              LocalDateTime.of(2026, 8, 11, 11, 50, 0)
            ),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)

      val scheme0 = json \ "scheme"

      (scheme0 \ "accountsOfficeReference").as[String] mustBe "123PA00123456"
      (scheme0 \ "utr").as[String] mustBe "1111111111"
      (scheme0 \ "name").as[String] mustBe "ABC Construction Ltd"
      (scheme0 \ "emailAddress").as[String] mustBe "ops@example.com"

      val sub0 = (json \ "subcontractors")(0)

      (sub0 \ "subcontractorId").as[Long] mustBe 1L
      (sub0 \ "subcontractorType").as[String] mustBe "soletrader"
      (sub0 \ "subbieResourceRef").as[Long] mustBe 10L
      (sub0 \ "utr").as[String] mustBe "1111111111"

      val vb0 = json \ "verificationBatch"

      (vb0 \ "verificationBatchId").as[Long] mustBe 99L
      (vb0 \ "verifBatchResourceRef").as[Long] mustBe 1234567L

      val v0 = (json \ "verifications")(0)

      (v0 \ "verificationId").as[Long] mustBe 1001L
      (v0 \ "verificationBatchId").as[Long] mustBe 99L
      (v0 \ "verificationResourceRef").as[Long] mustBe 12345L
      (v0 \ "matched").as[String] mustBe "Y"
      (v0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (v0 \ "taxTreatment").as[String] mustBe "0"
      (v0 \ "subcontractorName").as[String] mustBe "James Star"

      val subm0 = json \ "submission"

      (subm0 \ "submissionId").as[Long] mustBe 1234L
      (subm0 \ "submissionType").as[String] mustBe "CT600"
      (subm0 \ "activeObjectId").as[Long] mustBe 98765L
      (subm0 \ "status").as[String] mustBe "ACCEPTED"
      (subm0 \ "hmrcMarkGenerated").as[String] mustBe "20260811115300"
      (subm0 \ "hmrcMarkGgis").as[String] mustBe "ABC123XYZ456"
      (subm0 \ "emailRecipient").as[String] mustBe "test@example.com"
      (subm0 \ "acceptedTime").as[String] mustBe "2026-08-11T11:53:00"
      (subm0 \ "createDate").as[String] mustBe "2026-08-11T11:45:00"
      (subm0 \ "lastUpdate").as[String] mustBe "2026-08-11T11:53:00"
      (subm0 \ "schemeId").as[Long] mustBe 456789L
      (subm0 \ "agentId").as[String] mustBe "AGENT123"
      (subm0 \ "l_Migrated").as[Long] mustBe 0L
      (subm0 \ "submissionRequestDate").as[String] mustBe "2026-08-11T11:50:00"
      (subm0 \ "govTalkErrorCode").toOption mustBe None
      (subm0 \ "govTalkErrorType").toOption mustBe None
      (subm0 \ "govTalkErrorMessage").toOption mustBe None
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetLastSubmittedVerificationBatchResponse(
        scheme = Some(
          ContractorSchemeLastVerification(
            accountsOfficeReference = Some("123PA00123456"),
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com")
          )
        ),
        subcontractors = Seq(
          SubcontractorLastVerification(
            subcontractorId = 1L,
            subcontractorType = Some("soletrader"),
            subbieResourceRef = Some(10L),
            utr = Some("1111111111")
          )
        ),
        verificationBatch = Some(
          VerificationBatchLastVerification(
            verificationBatchId = Some(99L),
            verifBatchResourceRef = Some(1234567L),
            verificationBatchStatus = Some("ACCEPTED")
          )
        ),
        verifications = Seq(
          VerificationLastVerification(
            verificationId = 1001L,
            verificationBatchId = Some(99L),
            verificationResourceRef = Some(12345),
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            subcontractorName = Some("James Star")
          )
        ),
        submission = Some(
          Submission(
            submissionId = 1234L,
            submissionType = "CT600",
            activeObjectId = Some(98765L),
            status = Some("ACCEPTED"),
            hmrcMarkGenerated = Some("20260811115300"),
            hmrcMarkGgis = Some("ABC123XYZ456"),
            emailRecipient = Some("test@example.com"),
            acceptedTime = Some("2026-08-11T11:53:00"),
            createDate = Some(LocalDateTime.of(2026, 8, 11, 11, 45, 0)),
            lastUpdate = Some(LocalDateTime.of(2026, 8, 11, 11, 53, 0)),
            schemeId = 456789L,
            agentId = Some("AGENT123"),
            l_Migrated = Some(0L),
            submissionRequestDate = Some(
              LocalDateTime.of(2026, 8, 11, 11, 50, 0)
            ),
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)

      json.validate[GetLastSubmittedVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }
}
