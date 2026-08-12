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

package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.{JsSuccess, Json}

import uk.gov.hmrc.constructionindustryscheme.models._

final class VerificationSpec extends AnyWordSpec with Matchers {

  "Verification JSON format" should {

    "read JSON into model" in {
      val json = Json.parse(
        """
          |{
          |  "verificationId": 1001,
          |  "matched": "Y",
          |  "verificationNumber": "V0000000001",
          |  "taxTreatment": "0",
          |  "verificationBatchId": 99,
          |  "subcontractorId": 1,
          |  "actionIndicator": "VERIFY",
          |  "proceed": "Y",
          |  "verificationResourceRef": 10
          |}
          |""".stripMargin
      )

      val out = json.as[Verification]
      out.verificationId mustBe 1001L
      out.matched mustBe Some("Y")
      out.verificationNumber mustBe Some("V0000000001")
      out.taxTreatment mustBe Some("0")
      out.verificationBatchId mustBe Some(99L)
      out.subcontractorId mustBe Some(1L)
      out.actionIndicator mustBe Some("VERIFY")
      out.proceed mustBe Some("Y")
      out.verificationResourceRef mustBe Some(10L)
    }

    "read JSON when the optional F3a fields are missing" in {
      val json = Json.parse(
        """
          |{
          |  "verificationId": 1001,
          |  "matched": "Y",
          |  "verificationNumber": "V0000000001",
          |  "taxTreatment": "0",
          |  "verificationBatchId": 99,
          |  "subcontractorId": 1
          |}
          |""".stripMargin
      )

      val out = json.as[Verification]

      out.actionIndicator mustBe None
      out.proceed mustBe None
      out.verificationResourceRef mustBe None
    }

    "write model to JSON" in {
      val model = Verification(
        verificationId = 1001L,
        matched = None,
        verificationNumber = Some("V0000000001"),
        taxTreatment = None,
        verificationBatchId = Some(99L),
        subcontractorId = Some(1L),
        actionIndicator = Some("VERIFY"),
        proceed = Some("Y"),
        verificationResourceRef = Some(10L)
      )

      val json = Json.toJson(model)

      (json \ "verificationId").as[Long] mustBe 1001L
      (json \ "matched").toOption mustBe None
      (json \ "verificationNumber").as[String] mustBe "V0000000001"
      (json \ "taxTreatment").toOption mustBe None
      (json \ "verificationBatchId").as[Long] mustBe 99L
      (json \ "subcontractorId").as[Long] mustBe 1L
      (json \ "verificationResourceRef").as[Long] mustBe 10L
      (json \ "actionIndicator").as[String] mustBe "VERIFY"
      (json \ "proceed").as[String] mustBe "Y"
      (json \ "verificationResourceRef").as[Long] mustBe 10L
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = Verification(
        verificationId = 42L,
        matched = Some("N"),
        verificationNumber = None,
        taxTreatment = Some("1"),
        verificationBatchId = None,
        subcontractorId = None,
        actionIndicator = Some("MATCH"),
        proceed = Some("N"),
        verificationResourceRef = Some(20L)
      )

      val json = Json.toJson(model)
      json.validate[Verification] mustBe JsSuccess(model)
    }
  }
}
