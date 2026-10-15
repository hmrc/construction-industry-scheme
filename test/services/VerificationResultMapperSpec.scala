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

    "No requested verification is available for an unmatched response subcontractor" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("9999999999"),
        tradingName = Some("Different Trading Name"),
        foreName = Some("Different"),
        middleName = None,
        surname = Some("Person")
      )

      val result = mapper.mapAll(
        chrisResults = Seq(chrisResult),
        context = storedVerificationContext(requestedVerifications = Seq.empty),
        verifiedDate = verifiedDate
      )

      result.failed.futureValue.getMessage mustBe
        s"Errors occurred during mapping: No requested verification available for subcontractor: $chrisResult"
    }

    "fail with all mapping errors when no requested verifications are available" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val first = cisResponseSubcontractor(
        utr = Some("1111111111"),
        tradingName = Some("First Trading Name"),
        foreName = Some("First"),
        middleName = None,
        surname = Some("Person")
      )

      val second = cisResponseSubcontractor(
        utr = Some("2222222222"),
        tradingName = Some("Second Trading Name"),
        foreName = Some("Second"),
        middleName = None,
        surname = Some("Person")
      )

      val result = mapper.mapAll(
        chrisResults = Seq(first, second),
        context = storedVerificationContext(requestedVerifications = Seq.empty),
        verifiedDate = verifiedDate
      )

      result.failed.futureValue.getMessage mustBe
        s"Errors occurred during mapping: No requested verification available for subcontractor: $first; " +
        s"No requested verification available for subcontractor: $second"
    }

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
            verificationResourceRef = 14L,
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = None,
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = Some("net"),
          verifiedDate = None
        )
      )
    }

    "map ChRIS result to verification result for partnership when verify action has verification number" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        partnershipUtr = Some("1234567890"),
        tradingName = Some("John A"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("matched"),
        taxTreatment = None,
        verificationNumber = Some("V1000000007")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
            tradingName = Some("John A"),
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("partnership")
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = Some("Y"),
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = None,
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
            verificationResourceRef = 14L,
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = Some("net"),
          verifiedDate = None
        )
      )
    }

    "map ChRIS result with missing verification number and match is matched" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("matched"),
        taxTreatment = Some("net"),
        verificationNumber = None
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = Some("net"),
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
            verificationResourceRef = 14L,
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = Some("Y"),
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = Some("net"),
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
            verificationResourceRef = 14L,
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = Some("net"),
          verifiedDate = None
        )
      )
    }

    "map ChRIS result to verification result for partnership when verify action has verification number but no subcontractors" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        partnershipUtr = Some("12345678909"),
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
            verificationResourceRef = 14L,
            foreName = Some("John"),
            tradingName = Some("Not matching"),
            surname = Some("Smith"),
            utr = Some("1234567890"),
            subcontractorType = Some("partnership")
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
          resourceRef = 14L,
          matchedToRequest = false,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verifiedDate = None
        )
      )
    }

    "match a sole trader by names when UTR and trading name do not match" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("9999999999"),
        tradingName = Some("Response Trading Name"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        matched = Some("matched")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("1234567890"),
            tradingName = Some("Request Trading Name"),
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
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
          resourceRef = 14L,
          matchedToRequest = true,
          matched = Some("Y"),
          verified = Some("Y"),
          verificationNumber = Some("V1000000007"),
          taxTreatment = Some("net"),
          verifiedDate = Some(verifiedDate)
        )
      )
    }

    "not match a sole trader when one of the names differs" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("9999999999"),
        tradingName = Some("Response Trading Name"),
        foreName = Some("John"),
        middleName = Some("B"),
        surname = Some("Smith")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("1234567890"),
            tradingName = Some("Request Trading Name"),
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
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
          resourceRef = 14L,
          matchedToRequest = false,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verifiedDate = None
        )
      )
    }

    "not match a sole trader when all requested names are empty" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("9999999999"),
        tradingName = Some("Response Trading Name"),
        foreName = None,
        middleName = None,
        surname = None
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("1234567890"),
            tradingName = Some("Request Trading Name"),
            foreName = None,
            middleName = None,
            surname = None,
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
          resourceRef = 14L,
          matchedToRequest = false,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verifiedDate = None
        )
      )
    }

    "not match UTRs for an unsupported subcontractor type" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = cisResponseSubcontractor(
        utr = Some("1234567890"),
        tradingName = Some("Response Trading Name"),
        foreName = Some("Response"),
        middleName = None,
        surname = Some("Person")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("1234567890"),
            tradingName = Some("Request Trading Name"),
            foreName = Some("Request"),
            middleName = None,
            surname = Some("Person"),
            subcontractorType = Some("other")
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
          resourceRef = 14L,
          matchedToRequest = false,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verifiedDate = None
        )
      )
    }

    "succeed without warning when all subcontractors have the same first 11 characters in their verification numbers" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val first  = cisResponseSubcontractor(
        utr = Some("1111111111"),
        verificationNumber = Some("V10000000070001")
      )
      val second = cisResponseSubcontractor(
        utr = Some("2222222222"),
        verificationNumber = Some("V10000000070002")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 13L,
            utr = Some("1111111111"),
            subcontractorType = Some("soletrader")
          ),
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("2222222222"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      val results = mapper
        .mapAll(chrisResults = Seq(first, second), context = context, verifiedDate = verifiedDate)
        .futureValue

      results must have size 2
    }

    "succeed and log a warning when subcontractors have different first 11 characters in their verification numbers" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val first  = cisResponseSubcontractor(
        utr = Some("1111111111"),
        verificationNumber = Some("V10000000070001")
      )
      val second = cisResponseSubcontractor(
        utr = Some("2222222222"),
        verificationNumber = Some("V20000000070002")
      )

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 13L,
            utr = Some("1111111111"),
            subcontractorType = Some("soletrader")
          ),
          storedRequestedVerification(
            verificationResourceRef = 14L,
            utr = Some("2222222222"),
            subcontractorType = Some("soletrader")
          )
        )
      )

      val results = mapper
        .mapAll(chrisResults = Seq(first, second), context = context, verifiedDate = verifiedDate)
        .futureValue

      results must have size 2
    }

    "include missing requested subcontractors when CHRIS returns fewer responses" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val requested = Seq(
        storedRequestedVerification(
          verificationResourceRef = 1L,
          utr = Some("1111111111"),
          foreName = Some("A"),
          middleName = None,
          surname = Some("Person")
        ),
        storedRequestedVerification(
          verificationResourceRef = 2L,
          utr = Some("2222222222"),
          foreName = Some("B"),
          middleName = None,
          surname = Some("Person")
        ),
        storedRequestedVerification(
          verificationResourceRef = 3L,
          utr = Some("3333333333"),
          foreName = Some("C"),
          middleName = None,
          surname = Some("Person")
        ),
        storedRequestedVerification(
          verificationResourceRef = 4L,
          utr = Some("4444444444"),
          foreName = Some("D"),
          middleName = None,
          surname = Some("Person")
        ),
        storedRequestedVerification(
          verificationResourceRef = 5L,
          utr = Some("5555555555"),
          foreName = Some("E"),
          middleName = None,
          surname = Some("Person")
        ),
        storedRequestedVerification(
          verificationResourceRef = 6L,
          utr = Some("6666666666"),
          foreName = Some("F"),
          middleName = None,
          surname = Some("Person")
        )
      )

      val chrisResults = Seq(
        cisResponseSubcontractor(
          utr = Some("1111111111"),
          foreName = Some("A"),
          middleName = None,
          surname = Some("Person"),
          matched = Some("matched")
        ),
        cisResponseSubcontractor(
          utr = Some("9999999999"),
          foreName = Some("Unknown"),
          middleName = None,
          surname = Some("Person")
        )
      )

      val results = mapper
        .mapAll(
          chrisResults = chrisResults,
          context = storedVerificationContext(requestedVerifications = requested),
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(1L, 2L, 3L, 4L, 5L, 6L)

      results.head.matchedToRequest mustBe true
      results.head.matched mustBe Some("Y")
      results.head.verificationNumber mustBe Some("V1000000007")
      results.head.taxTreatment mustBe Some("net")

      results.tail.foreach { result =>
        result.matchedToRequest mustBe false
        result.matched mustBe None
        result.verified mustBe None
        result.verificationNumber mustBe None
        result.taxTreatment mustBe None
        result.verifiedDate mustBe None
      }
    }

    "include every requested subcontractor when CHRIS returns no results" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(verificationResourceRef = 13L, utr = Some("1111111111")),
          storedRequestedVerification(verificationResourceRef = 14L, utr = Some("2222222222"))
        )
      )

      val results = mapper
        .mapAll(chrisResults = Seq.empty, context = context, verifiedDate = verifiedDate)
        .futureValue

      results.map(_.resourceRef) mustBe Seq(13L, 14L)

      results.foreach { result =>
        result.matchedToRequest mustBe false
        result.matched mustBe None
        result.verified mustBe None
        result.verificationNumber mustBe None
        result.taxTreatment mustBe None
        result.verifiedDate mustBe None
      }
    }

    "preserve CHRIS response order and append missing requested subcontractors" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(verificationResourceRef = 13L, utr = Some("1111111111")),
          storedRequestedVerification(verificationResourceRef = 14L, utr = Some("2222222222")),
          storedRequestedVerification(verificationResourceRef = 15L, utr = Some("3333333333"))
        )
      )

      val chrisResults = Seq(
        cisResponseSubcontractor(utr = Some("3333333333"), verificationNumber = Some("V10000000070003")),
        cisResponseSubcontractor(utr = Some("1111111111"), verificationNumber = Some("V10000000070001"))
      )

      val results = mapper
        .mapAll(chrisResults = chrisResults, context = context, verifiedDate = verifiedDate)
        .futureValue

      results.map(_.resourceRef) mustBe Seq(15L, 13L, 14L)

      results.map(_.verificationNumber) mustBe Seq(
        Some("V10000000070003"),
        Some("V10000000070001"),
        None
      )

      results.map(_.matchedToRequest) mustBe Seq(true, true, false)
    }

    "use first CHRIS response when six requested subcontractors receive seven responses including a duplicate" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(verificationResourceRef = 11L, utr = Some("1111111111")),
          storedRequestedVerification(verificationResourceRef = 12L, utr = Some("2222222222")),
          storedRequestedVerification(verificationResourceRef = 13L, utr = Some("3333333333")),
          storedRequestedVerification(verificationResourceRef = 14L, utr = Some("4444444444")),
          storedRequestedVerification(verificationResourceRef = 15L, utr = Some("5555555555")),
          storedRequestedVerification(verificationResourceRef = 16L, utr = Some("6666666666"))
        )
      )

      val first = cisResponseSubcontractor(
        utr = Some("1111111111"),
        matched = Some("matched"),
        verificationNumber = Some("V10000000070001")
      )

      val duplicate = first.copy(
        verificationNumber = Some("V10000000070002"),
        taxTreatment = Some("gross"),
        matched = Some("unmatched")
      )

      val chrisResults = Seq(
        first,
        duplicate,
        cisResponseSubcontractor(utr = Some("2222222222"), verificationNumber = Some("V10000000070003")),
        cisResponseSubcontractor(utr = Some("3333333333"), verificationNumber = Some("V10000000070004")),
        cisResponseSubcontractor(utr = Some("4444444444"), verificationNumber = Some("V10000000070005")),
        cisResponseSubcontractor(utr = Some("5555555555"), verificationNumber = Some("V10000000070006")),
        cisResponseSubcontractor(utr = Some("6666666666"), verificationNumber = Some("V10000000070007"))
      )

      val results = mapper
        .mapAll(chrisResults = chrisResults, context = context, verifiedDate = verifiedDate)
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L, 13L, 14L, 15L, 16L)

      results.map(_.matchedToRequest) mustBe Seq.fill(6)(true)

      results.map(_.verificationNumber) mustBe Seq(
        Some("V10000000070001"),
        Some("V10000000070003"),
        Some("V10000000070004"),
        Some("V10000000070005"),
        Some("V10000000070006"),
        Some("V10000000070007")
      )

      results.head.matched mustBe Some("Y")
      results.head.taxTreatment mustBe Some("net")
      results.head.verifiedDate mustBe Some(verifiedDate)
    }

    "ignore duplicate CHRIS response when another subcontractor shares the same trading name" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 11L,
            utr = Some("1111111111"),
            tradingName = Some("Alice"),
            foreName = Some("Alice"),
            middleName = None,
            surname = Some("A")
          ),
          storedRequestedVerification(
            verificationResourceRef = 12L,
            utr = None,
            tradingName = Some("Alice"),
            foreName = Some("Bob"),
            middleName = None,
            surname = Some("B")
          )
        )
      )

      val first = cisResponseSubcontractor(
        utr = Some("1111111111"),
        tradingName = Some("Alice"),
        foreName = Some("Alice"),
        middleName = None,
        surname = Some("A"),
        matched = Some("matched"),
        verificationNumber = Some("V10000000070001")
      )

      val duplicate = first.copy(
        verificationNumber = Some("V10000000070002")
      )

      val third = cisResponseSubcontractor(
        utr = Some("2222222222"),
        tradingName = Some("Alice"),
        foreName = Some("Bob"),
        middleName = None,
        surname = Some("B"),
        matched = Some("matched"),
        verificationNumber = Some("V10000000070003")
      )

      val results = mapper
        .mapAll(
          chrisResults = Seq(first, duplicate, third),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L)

      results.map(_.matchedToRequest) mustBe Seq(true, true)

      results.map(_.verificationNumber) mustBe Seq(
        Some("V10000000070001"),
        Some("V10000000070003")
      )

      results.map(_.matched) mustBe Seq(Some("Y"), Some("Y"))
      results.map(_.verifiedDate) mustBe Seq(Some(verifiedDate), Some(verifiedDate))
    }

    "leave subcontractor unmatched when the only remaining CHRIS response is a duplicate" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 11L,
            utr = Some("1111111111"),
            tradingName = Some("Alice"),
            foreName = Some("Alice"),
            middleName = None,
            surname = Some("A")
          ),
          storedRequestedVerification(
            verificationResourceRef = 12L,
            utr = None,
            tradingName = Some("Alice"),
            foreName = Some("Bob"),
            middleName = None,
            surname = Some("B")
          )
        )
      )

      val first = cisResponseSubcontractor(
        utr = Some("1111111111"),
        tradingName = Some("Alice"),
        foreName = Some("Alice"),
        middleName = None,
        surname = Some("A"),
        matched = Some("matched"),
        verificationNumber = Some("V10000000070001")
      )

      val duplicate = first.copy(
        verificationNumber = Some("V10000000070002")
      )

      val results = mapper
        .mapAll(
          chrisResults = Seq(first, duplicate),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L)

      results.map(_.matchedToRequest) mustBe Seq(true, false)

      results.head.verificationNumber mustBe Some("V10000000070001")

      results(1).matched mustBe None
      results(1).verified mustBe None
      results(1).verificationNumber mustBe None
      results(1).taxTreatment mustBe None
      results(1).verifiedDate mustBe None
    }

    "use first requested subcontractor when multiple requests share the same trading name" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 11L,
            utr = None,
            tradingName = Some("ABC Construction"),
            foreName = Some("Alice"),
            middleName = None,
            surname = Some("A")
          ),
          storedRequestedVerification(
            verificationResourceRef = 12L,
            utr = None,
            tradingName = Some("ABC Construction"),
            foreName = Some("Bob"),
            middleName = None,
            surname = Some("B")
          )
        )
      )

      val chrisResult = cisResponseSubcontractor(
        utr = None,
        tradingName = Some("ABC Construction"),
        foreName = None,
        middleName = None,
        surname = None,
        verificationNumber = Some("V10000000070001")
      )

      val results = mapper
        .mapAll(
          chrisResults = Seq(chrisResult),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L)

      results.map(_.matchedToRequest) mustBe Seq(true, false)

      results.head.verificationNumber mustBe Some("V10000000070001")

      results(1).matched mustBe None
      results(1).verified mustBe None
      results(1).verificationNumber mustBe None
      results(1).taxTreatment mustBe None
      results(1).verifiedDate mustBe None
    }

    "do not remove CHRIS responses with identical names when UTR is missing" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 11L,
            utr = None,
            tradingName = Some("ABC Construction")
          ),
          storedRequestedVerification(
            verificationResourceRef = 12L,
            utr = None,
            tradingName = Some("ABC Construction")
          )
        )
      )

      val first = cisResponseSubcontractor(
        utr = None,
        tradingName = Some("ABC Construction"),
        verificationNumber = Some("V10000000070001")
      )

      val second = first.copy(
        verificationNumber = Some("V10000000070002")
      )

      val results = mapper
        .mapAll(
          chrisResults = Seq(first, second),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L)
      results.map(_.matchedToRequest) mustBe Seq(true, true)

      results.map(_.verificationNumber) mustBe Seq(
        Some("V10000000070001"),
        Some("V10000000070002")
      )
    }

    "do not treat responses with the same UTR but different identity fields as duplicates" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(
            verificationResourceRef = 11L,
            utr = Some("1111111111"),
            foreName = Some("Alice"),
            middleName = None,
            surname = Some("A")
          ),
          storedRequestedVerification(
            verificationResourceRef = 12L,
            utr = Some("1111111111"),
            foreName = Some("Bob"),
            middleName = None,
            surname = Some("B")
          )
        )
      )

      val first = cisResponseSubcontractor(
        utr = Some("1111111111"),
        foreName = Some("Alice"),
        middleName = None,
        surname = Some("A"),
        verificationNumber = Some("V10000000070001")
      )

      val second = cisResponseSubcontractor(
        utr = Some("1111111111"),
        foreName = Some("Bob"),
        middleName = None,
        surname = Some("B"),
        verificationNumber = Some("V10000000070002")
      )

      val results = mapper
        .mapAll(
          chrisResults = Seq(first, second),
          context = context,
          verifiedDate = verifiedDate
        )
        .futureValue

      results.map(_.resourceRef) mustBe Seq(11L, 12L)

      results.map(_.matchedToRequest) mustBe Seq(true, true)

      results.map(_.verificationNumber) mustBe Seq(
        Some("V10000000070001"),
        Some("V10000000070002")
      )
    }

    "ignore an extra CHRIS response after all requested subcontractors have matched" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val context = storedVerificationContext(
        requestedVerifications = Seq(
          storedRequestedVerification(verificationResourceRef = 13L, utr = Some("1111111111")),
          storedRequestedVerification(verificationResourceRef = 14L, utr = Some("2222222222"))
        )
      )

      val chrisResults = Seq(
        cisResponseSubcontractor(utr = Some("1111111111")),
        cisResponseSubcontractor(utr = Some("2222222222")),
        cisResponseSubcontractor(utr = Some("9999999999"))
      )

      val results = mapper
        .mapAll(chrisResults = chrisResults, context = context, verifiedDate = verifiedDate)
        .futureValue

      results.map(_.resourceRef) mustBe Seq(13L, 14L)
      results.map(_.matchedToRequest) mustBe Seq(true, true)
    }

    "return an empty result when there are no requested verifications and no CHRIS results" in {
      val mapper       = new VerificationResultMapper()
      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val result = mapper.mapAll(
        chrisResults = Seq.empty,
        context = storedVerificationContext(requestedVerifications = Seq.empty),
        verifiedDate = verifiedDate
      )

      result.futureValue mustBe Seq.empty[VerificationResult]
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
