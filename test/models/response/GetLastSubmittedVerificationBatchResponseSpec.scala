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
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(1234567L),
            status = Some("ACCEPTED")
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
            subcontractorName = Some("James Star"),
            subcontractorId = Some(22L)
          )
        ),
        submission = Some(
          SubmissionNewVerification(
            submissionId = 1234L,
            activeObjectId = Some(98765L),
            status = Some("ACCEPTED"),
            submissionRequestDate = Some(
              LocalDateTime.of(2026, 8, 11, 11, 50, 0)
            )
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
      (subm0 \ "activeObjectId").as[Long] mustBe 98765L
      (subm0 \ "status").as[String] mustBe "ACCEPTED"
      (subm0 \ "submissionRequestDate").as[String] mustBe "2026-08-11T11:50:00"
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
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(1234567L),
            status = Some("ACCEPTED")
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
            subcontractorName = Some("James Star"),
            subcontractorId = Some(22L)
          )
        ),
        submission = Some(
          SubmissionNewVerification(
            submissionId = 1234L,
            activeObjectId = Some(98765L),
            status = Some("ACCEPTED"),
            submissionRequestDate = Some(
              LocalDateTime.of(2026, 8, 11, 11, 50, 0)
            )
          )
        )
      )

      val json = Json.toJson(model)

      json.validate[GetLastSubmittedVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }
}
