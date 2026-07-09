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
import uk.gov.hmrc.constructionindustryscheme.models.{SubcontractorCurrentVerification, VerificationActionIndicator, VerificationSubmissionContext, VerificationSubmissionContextBuilder}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}
import uk.gov.hmrc.constructionindustryscheme.repositories.StoredRequestedVerification

import java.time.LocalDateTime

class VerificationSubmissionContextBuilderSpec extends SpecBase {

  "VerificationSubmissionContextBuilder" - {
    "build verification submission context" in {
      val submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00")

      val request = ChrisVerificationRequest(
        instanceId = "1",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456",
        contractorUTR = "1234567890",
        contractorAORef = "123PA12345678",
        verificationBatchId = "10910",
        verificationBatchResourceRef = "5",
        emailRecipient = Some("test@test.com"),
        subcontractors = Seq(
          SubcontractorCurrentVerification(
            subcontractorId = 1L,
            subbieResourceRef = Some(13L),
            firstName = Some("John"),
            secondName = Some("A"),
            surname = Some("Smith"),
            tradingName = Some("Test Trading"),
            utr = Some("1234567890"),
            nino = Some("AB123456C"),
            crn = None,
            partnerUtr = None,
            partnershipTradingName = None,
            subcontractorType = Some("soletrader"),
            addressLine1 = None,
            addressLine2 = None,
            addressLine3 = None,
            addressLine4 = None,
            country = None,
            postcode = None,
            worksReferenceNumber = None
          )
        ),
        verifications = Seq(
          VerificationDetails(
            subcontractorName = "John Smith",
            verificationResourceRef = "13",
            proceedVerification = true
          )
        )
      )

      VerificationSubmissionContextBuilder.build(
        request = request,
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = submissionRequestDate
      ) mustBe Right(
        VerificationSubmissionContext(
          hmrcMarkGenerated = "hmrc-mark",
          submissionRequestDate = submissionRequestDate,
          verificationBatchResourceRef = 5L,
          actionIndicators = Seq(
            VerificationActionIndicator(
              verificationResourceRef = 13L,
              actionIndicator = "verify"
            )
          ),
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
              tradingName = Some("Test Trading"),
              utr = Some("1234567890"),
              nino = Some("AB123456C"),
              crn = None,
              partnershipUtr = None,
              subcontractorType = Some("soletrader")
            )
          )
        )
      )
    }

    "return an error when verificationBatchResourceRef is invalid" in {
      val request = ChrisVerificationRequest(
        instanceId = "1",
        isAgent = false,
        clientTaxOfficeNumber = "123",
        clientTaxOfficeRef = "AB456",
        contractorUTR = "1234567890",
        contractorAORef = "123PA12345678",
        verificationBatchId = "10910",
        verificationBatchResourceRef = "invalid",
        emailRecipient = Some("test@test.com"),
        subcontractors = Seq.empty,
        verifications = Seq.empty
      )

      VerificationSubmissionContextBuilder.build(
        request = request,
        hmrcMarkGenerated = "hmrc-mark",
        submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00")
      ) mustBe Left("Invalid long value for verificationBatchResourceRef: 'invalid'")
    }
  }
}
