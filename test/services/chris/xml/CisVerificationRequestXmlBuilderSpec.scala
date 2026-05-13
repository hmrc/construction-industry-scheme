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
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisVerificationRequestXmlBuilder

class CisVerificationRequestXmlBuilderSpec extends AnyWordSpec with Matchers {

  private val request = ChrisVerificationRequest(
    instanceId = "id-1",
    isAgent = false,
    clientTaxOfficeNumber = "",
    clientTaxOfficeRef = "",
    contractorUTR = "1234567890",
    contractorAORef = "123/AB456",
    verificationBatchId = "batch-1",
    verificationBatchResourceRef = "batch-ref",
    emailRecipient = None,
    verifications = Seq(
      VerificationDetails("John Smith", "10", proceedVerification = true),
      VerificationDetails("Jane Doe", "20", proceedVerification = false)
    )
  )

  private def baseSub(ref: Long) =
    SubcontractorCurrentVerification(
      subcontractorId = ref,
      subbieResourceRef = Some(ref),
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
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      xml.label mustBe "CISrequest"

      (xml \\ "Contractor" \\ "UTR").text.trim mustBe "1234567890"
      (xml \\ "Contractor" \\ "AOref").text.trim mustBe "123/AB456"
      (xml \\ "Declaration").text.trim mustBe "yes"
    }

    "build multiple subcontractors" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10), baseSub(20)))

      (xml \\ "Subcontractor").size mustBe 2
    }

    "set action to verify when proceedVerification is true" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      (xml \\ "Subcontractor" \\ "Action").text.trim mustBe "verify"
    }

    "set action to match when proceedVerification is false" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(20)))

      (xml \\ "Subcontractor" \\ "Action").text.trim mustBe "match"
    }

    "default action to match when no verification found" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(999)))

      (xml \\ "Subcontractor" \\ "Action").text.trim mustBe "match"
    }

    "build SoleTrader correctly with Name and no TradingName" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10).copy(subcontractorType = Some("soletrader"))))

      (xml \\ "Name").nonEmpty mustBe true
      (xml \\ "TradingName").isEmpty mustBe true
    }

    "build Partnership correctly" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(baseSub(10).copy(subcontractorType = Some("partnership")))
        )

      (xml \\ "TradingName").text.trim mustBe "PARTNERS LTD"
      (xml \\ "CRN").text.trim mustBe "CRN123"
      (xml \\ "NINO").text.trim mustBe "AA123456A"
      (xml \\ "Partnership").nonEmpty mustBe true
    }

    "build Company without NINO or Partnership" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(baseSub(10).copy(subcontractorType = Some("company")))
        )

      (xml \\ "NINO").isEmpty mustBe true
      (xml \\ "Partnership").isEmpty mustBe true
    }

    "include address lines and fields" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      val lines = xml \\ "Address" \\ "Line"
      lines.size mustBe 4

      (xml \\ "PostCode").text.trim mustBe "NE1 1AA"
      (xml \\ "Country").text.trim mustBe "UK"
    }

    "throw when subcontractorType is missing" in {
      val invalid = baseSub(10).copy(subcontractorType = None)

      val ex = intercept[IllegalArgumentException] {
        CisVerificationRequestXmlBuilder.build(request, Seq(invalid))
      }

      ex.getMessage must include("Missing subcontractorType")
    }

    "throw when subcontractorType is invalid" in {
      val invalid = baseSub(10).copy(subcontractorType = Some("invalid"))

      val ex = intercept[IllegalArgumentException] {
        CisVerificationRequestXmlBuilder.build(request, Seq(invalid))
      }

      ex.getMessage must include("Invalid SubcontractorType value")
    }
  }
}
