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
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisSubmissionRequest
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisReturnXmlBuilder

class CisReturnXmlBuilderSpec extends AnyWordSpec with Matchers {

  private def baseRequest(returnType: MonthlyReturnType, inactivity: String) =
    ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = false,
      clientTaxOfficeNumber = "",
      clientTaxOfficeRef = "",
      returnType = returnType,
      informationCorrect = "yes",
      inactivity = inactivity,
      standard = None
    )

  private def baseSub =
    ChrisStandardSubcontractor(
      subcontractorType = SoleTrader,
      name = Some(ChrisPersonName("Chris", None, "Smith")),
      tradingName = None,
      partnershipTradingName = None,
      utr = Some("1234567890"),
      crn = None,
      nino = None,
      verificationNumber = None,
      totalPayments = None,
      costOfMaterials = None,
      totalDeducted = None
    )

  "CisReturnXmlBuilder.build" should {

    "build nil return with contractor + declarations (+ inactivity when yes)" in {
      val xml = CisReturnXmlBuilder.build(baseRequest(MonthlyReturnType.Nil, inactivity = "yes"))

      xml.label mustBe "CISreturn"
      (xml \\ "Contractor" \\ "UTR").text mustBe "1234567890"
      (xml \\ "Contractor" \\ "AOref").text mustBe "123/AB456"
      (xml \\ "NilReturn").nonEmpty mustBe true
      (xml \\ "Declarations" \\ "InformationCorrect").text mustBe "yes"
      (xml \\ "Declarations" \\ "Inactivity").text mustBe "yes"
    }

    "throw if returnType=standard but standard payload missing" in {
      val ex = intercept[IllegalArgumentException] {
        CisReturnXmlBuilder.build(baseRequest(MonthlyReturnType.Standard, inactivity = "no"))
      }
      ex.getMessage must include("standard payload is required")
    }

    "throw if returnType=standard but subcontractors empty" in {
      val req = baseRequest(MonthlyReturnType.Standard, inactivity = "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq.empty,
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val ex = intercept[IllegalArgumentException] {
        CisReturnXmlBuilder.build(req)
      }
      ex.getMessage must include("At least one subcontractor")
    }

    "build standard return with at least one subcontractor + declarations" in {
      val sub = ChrisStandardSubcontractor(
        subcontractorType = SoleTrader,
        name = Some(ChrisPersonName("Chris", None, "Smith")),
        tradingName = None,
        partnershipTradingName = None,
        utr = Some("9999999999"),
        crn = None,
        nino = None,
        verificationNumber = None,
        totalPayments = None,
        costOfMaterials = None,
        totalDeducted = None
      )

      val req = baseRequest(MonthlyReturnType.Standard, inactivity = "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      xml.label mustBe "CISreturn"
      (xml \\ "Subcontractor").size mustBe 1
      (xml \\ "Declarations" \\ "EmploymentStatus").text mustBe "yes"
      (xml \\ "Declarations" \\ "Verification").text mustBe "yes"
      (xml \\ "Declarations" \\ "InformationCorrect").text mustBe "yes"
      (xml \\ "Declarations" \\ "Inactivity").isEmpty mustBe true
    }

    "include Inactivity node when inactivity is yes" in {
      val xml = CisReturnXmlBuilder.build(baseRequest(MonthlyReturnType.Nil, inactivity = "yes"))

      (xml \\ "Inactivity").text mustBe "yes"
    }

    "not include Name node when tradingName exists for sole trader" in {
      val sub = baseSub.copy(tradingName = Some("ABC Ltd"))

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "Name").isEmpty mustBe true
    }

    "include Unmatched when subcontractor UTR missing" in {
      val sub = baseSub.copy(utr = None)

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "Unmatched").text mustBe "yes"
    }

    "include CRN when subcontractorType is Partnership" in {
      val sub = baseSub.copy(
        subcontractorType = Partnership,
        crn = Some("CRN123")
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "CRN").text mustBe "CRN123"
    }

    "include NINO when subcontractorType is Partnership" in {
      val sub = baseSub.copy(
        subcontractorType = Partnership,
        nino = Some("AA123456A")
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "NINO").text mustBe "AA123456A"
    }

    "include VerificationNumber when present" in {
      val sub = baseSub.copy(
        verificationNumber = Some("V123")
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "VerificationNumber").text mustBe "V123"
    }

    "include middle name when present" in {
      val sub = baseSub.copy(
        name = Some(ChrisPersonName("Chris", Some("James"), "Smith"))
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "Name" \\ "Fore").map(_.text) must contain("James")
    }

    "include TradingName for partnership when partnershipTradingName exists" in {
      val sub = baseSub.copy(
        subcontractorType = Partnership,
        partnershipTradingName = Some("PARTNERS LTD")
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "TradingName").text mustBe "PARTNERS LTD"
    }

    "include TradingName when tradingName exists" in {
      val sub = baseSub.copy(
        tradingName = Some("ABC TRADING")
      )

      val req = baseRequest(MonthlyReturnType.Standard, "no").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(sub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "TradingName").text mustBe "ABC TRADING"
    }

    "include Inactivity node in standard return when inactivity is yes" in {
      val req = baseRequest(MonthlyReturnType.Standard, inactivity = "yes").copy(
        standard = Some(
          ChrisStandardMonthlyReturn(
            subcontractors = Seq(baseSub),
            declarations = ChrisStandardDeclarations("yes", "yes")
          )
        )
      )

      val xml = CisReturnXmlBuilder.build(req)

      (xml \\ "Declarations" \\ "Inactivity").text mustBe "yes"
    }
  }
}
