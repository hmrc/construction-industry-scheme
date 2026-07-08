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

package services

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.models.{CisResponseSubcontractor, VerificationResult}
import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredRequestedVerification, StoredVerificationContext}
import uk.gov.hmrc.constructionindustryscheme.services.VerificationResultMapper

import java.time.LocalDateTime

class VerificationResultMapperSpec extends SpecBase {

  "VerificationResultMapper" - {

    "map ChRIS result to verification result for sole trader when verify action has verification number" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("unmatched"),
        taxTreatment = Some("net"),
        verificationNumber = Some("V1000000007")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            subbieResourceRef = Some(13L),
            actionIndicator = "verify",
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue mustBe Seq(
        VerificationResult(
          resourceRef = 13L,
          matched = None,
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = "net",
          verifiedDate = None
        )
      )
    }

    "map ChRIS result with missing verification number and leave verified empty" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("unmatched"),
        taxTreatment = Some("net"),
        verificationNumber = None
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            subbieResourceRef = Some(13L),
            actionIndicator = "verify",
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue mustBe Seq(
        VerificationResult(
          resourceRef = 13L,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = "net",
          verifiedDate = None
        )
      )
    }

    "trim verification number before mapping and set verified date when matched" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("matched"),
        taxTreatment = Some("net"),
        verificationNumber = Some("  V1000000007  ")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            subbieResourceRef = Some(13L),
            actionIndicator = "verify",
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue mustBe Seq(
        VerificationResult(
          resourceRef = 13L,
          matched = Some("Y"),
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = "net",
          verifiedDate = Some(verifiedDate)
        )
      )
    }

    "map blank verification number to None and leave verified empty" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("unmatched"),
        taxTreatment = Some("net"),
        verificationNumber = Some("   ")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            subbieResourceRef = Some(13L),
            actionIndicator = "verify",
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue mustBe Seq(
        VerificationResult(
          resourceRef = 13L,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = "net",
          verifiedDate = None
        )
      )
    }

    "fail when no requested verification matches" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("9999999999"),
        foreName = Some("Jane"),
        surname = Some("Bloggs"),
        matched = Some("matched"),
        taxTreatment = None,
        verificationNumber = None
      )

      val context = storedVerificationContext(requestedVerifications = Seq.empty)

      val ex = mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .failed
        .futureValue

      ex.getMessage must include("No matching requested verification found")
    }
  }

  private def cisResponseSubcontractor(
    utr: Option[String] = Some("1234567890"),
    partnershipUtr: Option[String] = None,
    tradingName: Option[String] = None,
    foreName: Option[String] = Some("John"),
    middleName: Option[String] = Some("A"),
    surname: Option[String] = Some("Smith"),
    nino: Option[String] = Some("AB123456C"),
    matched: Option[String] = Some("unmatched"),
    taxTreatment: Option[String] = Some("net"),
    verificationNumber: Option[String] = Some("V1000000007")
  ): CisResponseSubcontractor =
    CisResponseSubcontractor(
      utr = utr,
      partnershipUtr = partnershipUtr,
      tradingName = tradingName,
      foreName = foreName,
      middleName = middleName,
      surname = surname,
      nino = nino,
      matched = matched,
      taxTreatment = taxTreatment,
      verificationNumber = verificationNumber
    )

  private def storedVerificationContext(
    requestedVerifications: Seq[StoredRequestedVerification]
  ): StoredVerificationContext =
    StoredVerificationContext(
      verificationBatchResourceRef = 5L,
      hmrcMarkGenerated = "hmrc-mark",
      submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00"),
      actionIndicators = Seq.empty,
      requestedVerifications = requestedVerifications
    )

  private def storedRequestedVerification(
    verificationResourceRef: Long = 13L,
    subcontractorId: Long = 1L,
    subbieResourceRef: Option[Long] = Some(13L),
    subcontractorName: String = "John Smith",
    actionIndicator: String = "verify",
    proceedVerification: Boolean = true,
    foreName: Option[String] = Some("John"),
    middleName: Option[String] = Some("A"),
    surname: Option[String] = Some("Smith"),
    tradingName: Option[String] = None,
    utr: Option[String] = Some("1234567890"),
    nino: Option[String] = Some("AB123456C"),
    crn: Option[String] = None,
    subcontractorType: Option[String] = Some("soletrader"),
    partnershipUtr: Option[String] = None
  ): StoredRequestedVerification =
    StoredRequestedVerification(
      verificationResourceRef = verificationResourceRef,
      subcontractorId = subcontractorId,
      subbieResourceRef = subbieResourceRef,
      subcontractorName = subcontractorName,
      actionIndicator = actionIndicator,
      proceedVerification = proceedVerification,
      foreName = foreName,
      middleName = middleName,
      surname = surname,
      tradingName = tradingName,
      utr = utr,
      nino = nino,
      crn = crn,
      subcontractorType = subcontractorType,
      partnershipUtr = partnershipUtr
    )
}
