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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.Subcontractor

class SubcontractorSpec extends AnyFreeSpec with Matchers {

  def subcontractor(
    subcontractorType: Option[String] = None,
    firstName: Option[String] = None,
    surname: Option[String] = None,
    tradingName: Option[String] = None,
    partnershipTradingName: Option[String] = None
  ): Subcontractor = Subcontractor(
    subcontractorId = 1L,
    utr = None,
    pageVisited = None,
    partnerUtr = None,
    crn = None,
    firstName = firstName,
    nino = None,
    secondName = None,
    surname = surname,
    partnershipTradingName = partnershipTradingName,
    tradingName = tradingName,
    subcontractorType = subcontractorType,
    addressLine1 = None,
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    country = None,
    postCode = None,
    emailAddress = None,
    phoneNumber = None,
    mobilePhoneNumber = None,
    worksReferenceNumber = None,
    createDate = None,
    lastUpdate = None,
    subbieResourceRef = None,
    matched = None,
    autoVerified = None,
    verified = None,
    verificationNumber = None,
    taxTreatment = None,
    verificationDate = None,
    version = None,
    updatedTaxTreatment = None,
    lastMonthlyReturnDate = None,
    pendingVerifications = None
  )

  "displayName" - {

    "for a sole trader" - {

      "should return firstName and surname when both are present" in {
        subcontractor(
          subcontractorType = Some("soletrader"),
          firstName = Some("John"),
          surname = Some("Smith")
        ).displayName shouldBe "John Smith"
      }

      "should return surname when firstName is not present" in {
        subcontractor(
          subcontractorType = Some("soletrader"),
          surname = Some("Smith")
        ).displayName shouldBe "Smith"
      }

      "should return tradingName when firstName and surname are not present" in {
        subcontractor(
          subcontractorType = Some("soletrader"),
          tradingName = Some("Smith Trading")
        ).displayName shouldBe "Smith Trading"
      }

      "should be case-insensitive for subcontractorType" in {
        subcontractor(
          subcontractorType = Some("SOLETRADER"),
          firstName = Some("John"),
          surname = Some("Smith")
        ).displayName shouldBe "John Smith"
      }
    }

    "for a company" - {

      "should return tradingName" in {
        subcontractor(
          subcontractorType = Some("company"),
          tradingName = Some("Acme Ltd")
        ).displayName shouldBe "Acme Ltd"
      }
    }

    "for a trust" - {

      "should return tradingName" in {
        subcontractor(
          subcontractorType = Some("trust"),
          tradingName = Some("Family Trust")
        ).displayName shouldBe "Family Trust"
      }
    }

    "for a partnership" - {

      "should return partnershipTradingName when present" in {
        subcontractor(
          subcontractorType = Some("partnership"),
          tradingName = Some("Trading Name"),
          partnershipTradingName = Some("Partnership Name")
        ).displayName shouldBe "Partnership Name"
      }

      "should return tradingName when partnershipTradingName is not present" in {
        subcontractor(
          subcontractorType = Some("partnership"),
          tradingName = Some("Trading Name")
        ).displayName shouldBe "Trading Name"
      }
    }

    "should return 'No name provided' when no matching case" in {
      subcontractor().displayName shouldBe "No name provided"
    }

    "should return 'No name provided' for unknown subcontractorType" in {
      subcontractor(
        subcontractorType = Some("unknown"),
        tradingName = Some("Some Name")
      ).displayName shouldBe "No name provided"
    }
  }
}
