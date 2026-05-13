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

package services.chris.xml

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisVerificationRequestXmlBuilder

class CisVerificationRequestXmlBuilderSpec extends AnyWordSpec with Matchers {

  private val contractorUtr   = "1234567890"
  private val contractorAoRef = "123/AB456"

  private def baseSub =
    SubcontractorCurrentVerification(
      subcontractorId = 1L,
      subbieResourceRef = Some(10L),
      firstName = Some("John"),
      secondName = Some("Q"),
      surname = Some("Smith"),
      tradingName = Some("ACME"),
      utr = Some("1111111111"),
      nino = Some("AA123456A"),
      crn = Some("CRN123"),
      partnerUtr = Some("2222222222"),
      partnershipTradingName = Some("PARTNERS LTD"),
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("Line 1"),
      addressLine2 = Some("Line 2"),
      addressLine3 = Some("Line 3"),
      addressLine4 = Some("Line 4"),
      country = Some("UK"),
      postcode = Some("NE1 1AA"),
      worksReferenceNumber = Some("WRN123")
    )

  "CisVerificationRequestXmlBuilder.build" should {

    "build CISrequest with contractor and declaration" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub),
          action = "Verify"
        )

      xml.label mustBe "CISrequest"

      (xml \\ "Contractor" \\ "UTR").text mustBe contractorUtr
      (xml \\ "Contractor" \\ "AOref").text mustBe contractorAoRef
      (xml \\ "Declaration").text mustBe "yes"
    }

    "build multiple Subcontractor nodes" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub, baseSub.copy(subcontractorId = 2L)),
          action = "Verify"
        )

      (xml \\ "Subcontractor").size mustBe 2
    }

    "build SoleTrader correctly with Name and NINO" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub.copy(subcontractorType = Some("soletrader"))),
          action = "Verify"
        )

      (xml \\ "Name").nonEmpty mustBe true
      (xml \\ "TradingName").isEmpty mustBe true
      (xml \\ "NINO").text mustBe "AA123456A"
    }

    "build Partnership correctly with TradingName, CRN, NINO and Partnership block" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub.copy(subcontractorType = Some("partnership"))),
          action = "Verify"
        )

      (xml \\ "TradingName").text mustBe "PARTNERS LTD"
      (xml \\ "CRN").text mustBe "CRN123"
      (xml \\ "NINO").text mustBe "AA123456A"
      (xml \\ "Partnership").nonEmpty mustBe true
    }

    "build Company correctly with TradingName and CRN but no NINO or Partnership" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub.copy(subcontractorType = Some("company"))),
          action = "Verify"
        )

      (xml \\ "TradingName").text mustBe "ACME"
      (xml \\ "CRN").text mustBe "CRN123"
      (xml \\ "NINO").isEmpty mustBe true
      (xml \\ "Partnership").isEmpty mustBe true
    }

    "build Trust correctly with TradingName only" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub.copy(subcontractorType = Some("trust"))),
          action = "Verify"
        )

      (xml \\ "TradingName").text mustBe "ACME"
      (xml \\ "CRN").isEmpty mustBe true
      (xml \\ "NINO").isEmpty mustBe true
      (xml \\ "Partnership").isEmpty mustBe true
    }

    "include WorksRef and UTR" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub),
          action = "Verify"
        )

      (xml \\ "WorksRef").text mustBe "WRN123"
      (xml \\ "Subcontractor" \\ "UTR").text mustBe "1111111111"
    }

    "include Address with 4 Line nodes" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(baseSub),
          action = "Verify"
        )

      val lines = xml \\ "Address" \\ "Line"
      lines.size mustBe 4

      (xml \\ "PostCode").text mustBe "NE1 1AA"
      (xml \\ "Country").text mustBe "UK"
    }

    "throw when subcontractorType is missing" in {
      val invalidSub = baseSub.copy(subcontractorType = None)

      val ex = intercept[IllegalArgumentException] {
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(invalidSub),
          action = "Verify"
        )
      }

      ex.getMessage must include("Missing subcontractorType")
    }

    "throw when subcontractorType is invalid" in {
      val invalidSub = baseSub.copy(subcontractorType = Some("invalid-type"))

      val ex = intercept[IllegalArgumentException] {
        CisVerificationRequestXmlBuilder.build(
          contractorUtr,
          contractorAoRef,
          Seq(invalidSub),
          action = "Verify"
        )
      }

      ex.getMessage must include("Invalid SubcontractorType value")
    }
  }
}
