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

import java.time.LocalDateTime

final class SubcontractorNewVerificationSpec extends AnyWordSpec with Matchers {

  "SubcontractorNewVerification JSON format" should {

    "read JSON into model (including nulls and missing fields)" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractorId": 1,
          |  "firstName": "John",
          |  "secondName": null,
          |  "surname": "Smith",
          |  "verified": "Y",
          |  "verificationNumber": "V0000000001",
          |  "taxTreatment": "0",
          |  "verificationDate": "2026-04-01T10:00:00",
          |  "createDate": "2026-04-01T10:00:00"
          |}
          |""".stripMargin
      )

      val out = json.as[SubcontractorNewVerification]

      out.subcontractorId mustBe 1L
      out.firstName mustBe Some("John")
      out.secondName mustBe None
      out.surname mustBe Some("Smith")
      out.tradingName mustBe None
      out.partnershipTradingName mustBe None
      out.verified mustBe Some("Y")
      out.verificationNumber mustBe Some("V0000000001")
      out.taxTreatment mustBe Some("0")
      out.verificationDate mustBe Some(LocalDateTime.of(2026, 4, 1, 10, 0, 0))
      out.lastMonthlyReturnDate mustBe None
      out.createDate mustBe Some(LocalDateTime.of(2026, 4, 1, 10, 0, 0))
    }

    "write model to JSON" in {
      val model = SubcontractorNewVerification(
        subcontractorId = 1L,
        firstName = Some("John"),
        secondName = Some("Q"),
        surname = Some("Smith"),
        tradingName = Some("ACME"),
        partnershipTradingName = None,
        verified = Some("Y"),
        verificationNumber = Some("V0000000001"),
        taxTreatment = Some("0"),
        verificationDate = Some(LocalDateTime.of(2026, 4, 1, 10, 0, 0)),
        lastMonthlyReturnDate = Some(LocalDateTime.of(2026, 4, 2, 10, 0, 0)),
        createDate = Some(LocalDateTime.of(2026, 4, 2, 10, 0, 0))
      )

      val json = Json.toJson(model)

      (json \ "subcontractorId").as[Long] mustBe 1L
      (json \ "firstName").as[String] mustBe "John"
      (json \ "secondName").as[String] mustBe "Q"
      (json \ "surname").as[String] mustBe "Smith"
      (json \ "tradingName").as[String] mustBe "ACME"
      (json \ "verified").as[String] mustBe "Y"
      (json \ "verificationNumber").as[String] mustBe "V0000000001"
      (json \ "taxTreatment").as[String] mustBe "0"
      (json \ "verificationDate").as[String] mustBe "2026-04-01T10:00:00"
      (json \ "lastMonthlyReturnDate").as[String] mustBe "2026-04-02T10:00:00"
      (json \ "createDate").as[String] mustBe "2026-04-02T10:00:00"
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = SubcontractorNewVerification(
        subcontractorId = 999L,
        firstName = None,
        secondName = None,
        surname = None,
        tradingName = None,
        partnershipTradingName = Some("Partnership Name"),
        verified = None,
        verificationNumber = None,
        taxTreatment = None,
        verificationDate = None,
        lastMonthlyReturnDate = None,
        createDate = None
      )

      val json = Json.toJson(model)
      json.validate[SubcontractorNewVerification] mustBe JsSuccess(model)
    }
  }
}
