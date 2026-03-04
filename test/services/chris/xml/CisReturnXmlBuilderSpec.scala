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
  }
}
