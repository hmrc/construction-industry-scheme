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

class GetCurrentVerificationBatchResponseSpec extends AnyWordSpec with Matchers {

  "GetCurrentVerificationBatchResponse Json format" should {

    "read FormP response JSON and parse all sections (including empty cursors)" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractors": [],
          |  "verificationBatch": [],
          |  "verifications": []
          |}
          |""".stripMargin
      )

      val result = json.validate[GetCurrentVerificationBatchResponse]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.subcontractors mustBe empty
      out.verificationBatch mustBe empty
      out.verifications mustBe empty
    }

    "write a response to JSON" in {
      val model = GetCurrentVerificationBatchResponse(
        subcontractors = Seq(
          SubcontractorCurrVerification(
            subcontractorId = 1L,
            subbieResourceRef = Some(10L),
            firstName = Some("John"),
            surname = Some("Smith"),
            secondName = None,
            tradingName = Some("ACME"),
            utr = Some("1111111111"),
            nino = Some("AA123456A"),
            crn = Some("AC012345"),
            partnerUtr = Some("5860920998")
          )
        ),
        verificationBatch = Seq(
          VerificationBatchCurrVerification(
            verificationBatchId = 99L,
            verifBatchResourceRef = Some(999L)
          )
        ),
        verifications = Seq(
          VerificationCurrVerification(
            verificationId = 1001L,
            verificationBatchId = Some(99L),
            subcontractorId = Some(1L),
            verificationResourceRef = Some(1L)
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


      val vb0 = (json \ "verificationBatch")(0)

      (vb0 \ "verificationBatchId").as[Long] mustBe 99L
      (vb0 \ "verifBatchResourceRef").as[Long] mustBe 999L

      val v0 = (json \ "verifications")(0)

      (v0 \ "verificationId").as[Long] mustBe 1001L
      (v0 \ "verificationBatchId").as[Long] mustBe 99L
      (v0 \ "subcontractorId").as[Long] mustBe 1L
      (v0 \ "verificationResourceRef").as[Long] mustBe 1L
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = GetCurrentVerificationBatchResponse(
        subcontractors = Seq.empty,
        verificationBatch = Seq.empty,
        verifications = Seq.empty
      )

      val json = Json.toJson(model)
      json.validate[GetCurrentVerificationBatchResponse] mustBe JsSuccess(model)
    }
  }

}
