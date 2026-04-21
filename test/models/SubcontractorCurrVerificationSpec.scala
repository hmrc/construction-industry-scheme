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


import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.SubcontractorCurrVerification

class SubcontractorCurrVerificationSpec extends SpecBase{

  "SubcontractorCurrVerification" - {
    "serialize to JSON correctly" in {
      val subcontractors = SubcontractorCurrVerification(
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
      val json = Json.toJson(subcontractors)

      (json \ "subcontractorId").as[Long] mustBe 1L
      (json \ "subbieResourceRef").as[Long] mustBe 10L
      (json \ "firstName").as[String] mustBe "John"
      (json \ "surname").as[String] mustBe "Smith"
      (json \ "secondName").toOption mustBe None
      (json \ "tradingName").as[String] mustBe "ACME"
      (json \ "utr").as[String] mustBe "1111111111"
      (json \ "nino").as[String] mustBe "AA123456A"
      (json \ "crn").as[String] mustBe "AC012345"
      (json \ "partnerUtr").as[String] mustBe "5860920998"
    }
    "deserialize from JSON correctly" in {
      val json = Json.parse(
        """
          |{
          |  "subcontractorId": 1,
          |  "subbieResourceRef": 10,
          |  "firstName" : "John",
          |  "surname" : "Smith",
          |  "secondName" : "Paul",
          |  "tradingName" : "ACME",
          |  "utr" : "1111111111",
          |  "nino" : "AA123456A",
          |  "crn" : "AC012345",
          |  "partnerUtr" : "5860920998"
          |}
          |""".stripMargin
      )
      val result = json.as[SubcontractorCurrVerification]
      result.subcontractorId mustBe 1L
      result.subbieResourceRef mustBe Some(10L)
      result.firstName mustBe Some("John")
      result.surname mustBe Some("Smith")
      result.secondName mustBe Some("Paul")
      result.tradingName mustBe Some("ACME")
      result.utr mustBe Some("1111111111")
      result.nino mustBe Some("AA123456A")
      result.crn mustBe Some("AC012345")
      result.partnerUtr mustBe Some("5860920998")
    }
    "round-trip serialize and deserialize correctly" in {
      val subcontractors = SubcontractorCurrVerification(
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
      val json = Json.toJson(subcontractors)
      val result = json.as[SubcontractorCurrVerification]
      result mustBe subcontractors
    }
  }
}
