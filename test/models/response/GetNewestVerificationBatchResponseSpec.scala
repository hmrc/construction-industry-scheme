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

import java.time.LocalDateTime

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
          |  "monthlyReturn": []
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
    }

    "write a response to JSON" in {
      val model = GetNewestVerificationBatchResponse(
        scheme = Seq(
          ContractorSchemeNewVerification(
            accountsOfficeReference = "123PA00123456",
            utr = Some("1111111111"),
            name = Some("ABC Construction Ltd"),
            emailAddress = Some("ops@example.com")
          )
        ),
        subcontractors = Seq(
          SubcontractorNewVerification(
            subcontractorId = 1L,
            firstName = Some("John"),
            secondName = None,
            surname = Some("Smith"),
            tradingName = Some("ACME"),
            partnershipTradingName = Some("ACME trading"),
            verified = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationDate = Some(LocalDateTime.of(2026, 1, 12, 11, 0, 0)),
            lastMonthlyReturnDate = None,
            createDate = Some(LocalDateTime.of(2026, 1, 4, 10, 0, 0))
          )
        ),
        verificationBatch = Seq(
          VerificationBatch(
            verificationBatchId = 99L,
            status = Some("STARTED"),
            verificationNumber = Some("V0000000001")
          )
        ),
        verifications = Seq(
          Verification(
            verificationId = 1001L,
            matched = Some("Y"),
            verificationNumber = Some("V0000000001"),
            taxTreatment = Some("0"),
            verificationBatchId = Some(99L),
            subcontractorId = Some(1L)
          )
        ),
        submission = Seq(
          SubmissionNewVerification(
            submissionId = 555L,
            activeObjectId = Some(99L),
            status = Some("ACCEPTED"),
            submissionRequestDate = Some(LocalDateTime.of(2026, 1, 12, 11, 59, 0))
          )
        ),
        monthlyReturn = Seq(
          MonthlyReturnNewVerification(
            monthlyReturnId = 777L,
            decNoMoreSubPayments = Some("N")
          )
        )
      )

      val json = Json.toJson(model)

      val scheme0 = (json \ "scheme")(0)

      (scheme0 \ "accountsOfficeReference").as[String] mustBe "123PA00123456"
      (scheme0 \ "utr").as[String] mustBe "1111111111"
      (scheme0 \ "name").as[String] mustBe "ABC Construction Ltd"
      (scheme0 \ "emailAddress").as[String] mustBe "ops@example.com"

      val sub0 = (json \ "subcontractors")(0)

      (sub0 \ "subcontractorId").as[Long] mustBe 1L
      (sub0 \ "firstName").as[String] mustBe "John"
      (sub0 \ "secondName").toOption mustBe None
      (sub0 \ "surname").as[String] mustBe "Smith"
      (sub0 \ "tradingName").as[String] mustBe "ACME"
      (sub0 \ "partnershipTradingName").as[String] mustBe "ACME trading"
      (sub0 \ "verified").as[String] mustBe "Y"
      (sub0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (sub0 \ "taxTreatment").as[String] mustBe "0"
      (sub0 \ "verificationDate").as[String] mustBe "2026-01-12T11:00:00"
      (sub0 \ "lastMonthlyReturnDate").toOption mustBe None
      (sub0 \ "createDate").as[String] mustBe "2026-01-04T10:00:00"

      val vb0 = (json \ "verificationBatch")(0)

      (vb0 \ "verificationBatchId").as[Long] mustBe 99L
      (vb0 \ "status").as[String] mustBe "STARTED"
      (vb0 \ "verificationNumber").as[String] mustBe "V0000000001"

      val v0 = (json \ "verifications")(0)

      (v0 \ "verificationId").as[Long] mustBe 1001L
      (v0 \ "matched").as[String] mustBe "Y"
      (v0 \ "verificationNumber").as[String] mustBe "V0000000001"
      (v0 \ "taxTreatment").as[String] mustBe "0"
      (v0 \ "verificationBatchId").as[Long] mustBe 99L
      (v0 \ "subcontractorId").as[Long] mustBe 1L

      val subm0 = (json \ "submission")(0)

      (subm0 \ "submissionId").as[Long] mustBe 555L
      (subm0 \ "activeObjectId").as[Long] mustBe 99L
      (subm0 \ "status").as[String] mustBe "ACCEPTED"
      (subm0 \ "submissionRequestDate").as[String] mustBe "2026-01-12T11:59:00"

      val mr0 = (json \ "monthlyReturn")(0)

      (mr0 \ "monthlyReturnId").as[Long] mustBe 777L
      (mr0 \ "decNoMoreSubPayments").as[String] mustBe "N"

    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetNewestVerificationBatchResponse(
        scheme = Seq.empty,
        subcontractors = Seq.empty,
        verificationBatch = Seq.empty,
        verifications = Seq.empty,
        submission = Seq.empty,
        monthlyReturn = Seq(
          MonthlyReturnNewVerification(
            monthlyReturnId = 777L,
            decNoMoreSubPayments = Some("N")
          )
        )
      )

      val json = Json.toJson(model)
      json.validate[GetNewestVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }
}
