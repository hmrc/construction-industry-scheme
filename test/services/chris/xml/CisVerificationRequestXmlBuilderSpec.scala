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
import scala.xml.{Node, Utility}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisVerificationRequestXmlBuilder

class CisVerificationRequestXmlBuilderSpec extends AnyWordSpec with Matchers {

  private def normalizeXml(n: Node): Node =
    Utility.trim(n)

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

  private def subcontractorNode(xml: scala.xml.Elem) =
    (xml \\ "Subcontractor").head

  "CisVerificationRequestXmlBuilder.build" should {

    "build CISrequest with contractor and default declaration" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      xml.label mustBe "CISrequest"
      (xml \\ "Contractor" \\ "UTR").text.trim mustBe "1234567890"
      (xml \\ "Contractor" \\ "AOref").text.trim mustBe "123/AB456"
      (xml \\ "Declaration").text.trim mustBe "yes"
    }

    "build CISrequest with explicit declaration when provided" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)), declaration = "no")

      (xml \\ "Declaration").text.trim mustBe "no"
    }

    "build multiple subcontractors" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10), baseSub(20)))

      (xml \\ "Subcontractor").size mustBe 2
    }

    "set action to verify when proceedVerification is true" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      (subcontractorNode(xml) \\ "Action").text.trim mustBe "verify"
    }

    "set action to match when proceedVerification is false" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(20)))

      (subcontractorNode(xml) \\ "Action").text.trim mustBe "match"
    }

    "default action to match when no verification is found" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(999)))

      (subcontractorNode(xml) \\ "Action").text.trim mustBe "match"
    }

    "for SoleTrader with both Name and TradingName present, map only TradingName" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(
            baseSub(10).copy(
              subcontractorType = Some("soletrader"),
              firstName = Some("John"),
              surname = Some("Smith"),
              tradingName = Some("ACME")
            )
          )
        )

      val sub = subcontractorNode(xml)

      (sub \\ "TradingName").text.trim mustBe "ACME"
      (sub \\ "Name").isEmpty mustBe true
    }

    "for SoleTrader with no TradingName, build Name using FIRSTNAME only for Fore and ignore second name" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(
            baseSub(10).copy(
              subcontractorType = Some("soletrader"),
              firstName = Some("John"),
              secondName = Some("Q"),
              surname = Some("Smith"),
              tradingName = None
            )
          )
        )

      val sub = subcontractorNode(xml)

      (sub \\ "Name").nonEmpty mustBe true
      (sub \\ "Name" \\ "Fore").text.trim mustBe "John"
      (sub \\ "Name" \\ "Sur").text.trim mustBe "Smith"
      (sub \\ "TradingName").isEmpty mustBe true
    }

    "for Partnership, build TradingName, CRN, NINO, Partnership block and use partnerUtr as top-level UTR" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(
            baseSub(10).copy(
              subcontractorType = Some("partnership"),
              tradingName = Some("ACME"),
              partnershipTradingName = Some("PARTNERS LTD"),
              utr = Some("1111111111"),
              partnerUtr = Some("2222222222")
            )
          )
        )

      val sub = subcontractorNode(xml)

      (sub \\ "TradingName").text.trim mustBe "ACME"
      (sub \\ "CRN").text.trim mustBe "CRN123"
      (sub \\ "NINO").text.trim mustBe "AA123456A"
      (sub \\ "Subcontractor" \\ "UTR").head.text.trim mustBe "2222222222"
      (sub \\ "Partnership").nonEmpty mustBe true
      (sub \\ "Partnership" \\ "Name").text.trim mustBe "PARTNERS LTD"
      (sub \\ "Partnership" \\ "UTR").text.trim mustBe "1111111111"
    }

    "for Company, build TradingName and CRN, omit NINO and Partnership, and use subcontractor UTR" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(baseSub(10).copy(subcontractorType = Some("company")))
        )

      val sub = subcontractorNode(xml)

      (sub \\ "TradingName").text.trim mustBe "ACME"
      (sub \\ "CRN").text.trim mustBe "CRN123"
      (sub \\ "UTR").text.trim mustBe "1111111111"
      (sub \\ "NINO").isEmpty mustBe true
      (sub \\ "Partnership").isEmpty mustBe true
      (sub \\ "Name").isEmpty mustBe true
    }

    "for Trust, build TradingName only and use subcontractor UTR" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(baseSub(10).copy(subcontractorType = Some("trust")))
        )

      val sub = subcontractorNode(xml)

      (sub \\ "TradingName").text.trim mustBe "ACME"
      (sub \\ "UTR").text.trim mustBe "1111111111"
      (sub \\ "CRN").isEmpty mustBe true
      (sub \\ "NINO").isEmpty mustBe true
      (sub \\ "Partnership").isEmpty mustBe true
      (sub \\ "Name").isEmpty mustBe true
    }

    "include WorksRef when present" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      (subcontractorNode(xml) \\ "WorksRef").text.trim mustBe "WRN123"
    }

    "omit Address when no address fields are present" in {
      val xml =
        CisVerificationRequestXmlBuilder.build(
          request,
          Seq(
            baseSub(10).copy(
              addressLine1 = None,
              addressLine2 = None,
              addressLine3 = None,
              addressLine4 = None,
              postcode = None,
              country = None
            )
          )
        )

      (subcontractorNode(xml) \\ "Address").isEmpty mustBe true
    }

    "include Address with 4 Line nodes when address exists" in {
      val xml = CisVerificationRequestXmlBuilder.build(request, Seq(baseSub(10)))

      val sub   = subcontractorNode(xml)
      val lines = sub \\ "Address" \\ "Line"

      lines.size mustBe 4
      lines.map(_.text.trim) mustBe Seq("Line 1", "Line 2", "Line 3", "Line 4")
      (sub \\ "PostCode").text.trim mustBe "NE1 1AA"
      (sub \\ "Country").text.trim mustBe "UK"
    }

    "throw when address fields are present but ADDRESS_LINE_1 is missing" in {
      val invalid =
        baseSub(10).copy(
          addressLine1 = None,
          addressLine2 = Some("Line 2"),
          postcode = Some("NE1 1AA"),
          country = Some("UK")
        )

      val ex = intercept[IllegalArgumentException] {
        CisVerificationRequestXmlBuilder.build(request, Seq(invalid))
      }

      ex.getMessage must include("ADDRESS_LINE_1 is mandatory")
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

    "produce the expected CISrequest structure for Partnership" in {

      val partnershipSub =
        baseSub(10).copy(
          subcontractorType = Some("partnership"),
          tradingName = Some("ACME"),
          partnershipTradingName = Some("PARTNERS LTD"),
          utr = Some("1111111111"),
          partnerUtr = Some("2222222222")
        )

      val actual =
        CisVerificationRequestXmlBuilder.build(request, Seq(partnershipSub))

      val expected =
        <CISrequest>
          <Contractor>
            <UTR>1234567890</UTR>
            <AOref>123/AB456</AOref>
          </Contractor>

          <Subcontractor>
            <Action>verify</Action>
            <Type>partnership</Type>
            <TradingName>ACME</TradingName>
            <WorksRef>WRN123</WorksRef>
            <UTR>2222222222</UTR>
            <CRN>CRN123</CRN>
            <NINO>AA123456A</NINO>

            <Partnership>
              <Name>PARTNERS LTD</Name>
              <UTR>1111111111</UTR>
            </Partnership>

            <Address>
              <Line>Line 1</Line>
              <Line>Line 2</Line>
              <Line>Line 3</Line>
              <Line>Line 4</Line>
              <PostCode>NE1 1AA</PostCode>
              <Country>UK</Country>
            </Address>
          </Subcontractor>

          <Declaration>yes</Declaration>
        </CISrequest>

      normalizeXml(actual) mustBe normalizeXml(expected)
    }
  }
}
