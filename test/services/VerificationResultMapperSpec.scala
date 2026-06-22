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

    "map ChRIS result to verification result for sole trader" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = CisResponseSubcontractor(
        utr = Some("1234567890"),
        partnershipUtr = None,
        tradingName = None,
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        nino = Some("AB123456C"),
        matched = Some("N"),
        taxTreatment = Some("net"),
        verificationNumber = Some("V1000000007")
      )

      val context = StoredVerificationContext(
        verificationBatchResourceRef = 5L,
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00"),
        actionIndicators = Seq.empty,
        requestedVerifications = Seq(
          StoredRequestedVerification(
            verificationResourceRef = 13L,
            subcontractorId = 1L,
            subbieResourceRef = Some(13L),
            subcontractorName = "John Smith",
            actionIndicator = "verify",
            proceedVerification = true,
            foreName = Some("John"),
            middleName = Some("A"),
            surname = Some("Smith"),
            tradingName = None,
            utr = Some("1234567890"),
            nino = Some("AB123456C"),
            crn = None,
            subcontractorType = Some("soletrader"),
            partnershipUtr = None
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
          matched = Some("N"),
          verified = Some("Y"),
          verificationNumber = "V1000000007",
          taxTreatment = "net",
          verifiedDate = verifiedDate
        )
      )
    }

    "fail when no requested verification matches" in {
      val mapper = new VerificationResultMapper()

      val verifiedDate = LocalDateTime.parse("2017-04-06T08:46:08.081")

      val chrisResult = CisResponseSubcontractor(
        utr = Some("9999999999"),
        partnershipUtr = None,
        tradingName = None,
        foreName = Some("Jane"),
        middleName = None,
        surname = Some("Bloggs"),
        nino = None,
        matched = Some("Y"),
        taxTreatment = None,
        verificationNumber = None
      )

      val context = StoredVerificationContext(
        verificationBatchResourceRef = 5L,
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00"),
        actionIndicators = Seq.empty,
        requestedVerifications = Seq.empty
      )

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
}
