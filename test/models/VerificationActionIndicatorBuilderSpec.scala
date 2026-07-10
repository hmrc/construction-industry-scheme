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
import uk.gov.hmrc.constructionindustryscheme.models.{VerificationActionIndicator, VerificationActionIndicatorBuilder}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}

class VerificationActionIndicatorBuilderSpec extends SpecBase {

  "VerificationActionIndicatorBuilder" - {
    "build action indicators from verification request" in {
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
        subcontractors = Seq.empty,
        verifications = Seq(
          VerificationDetails(
            subcontractorName = "Subcontractor 1",
            verificationResourceRef = "13",
            proceedVerification = true
          ),
          VerificationDetails(
            subcontractorName = "Subcontractor 2",
            verificationResourceRef = "14",
            proceedVerification = false
          )
        )
      )

      VerificationActionIndicatorBuilder.buildEither(request) mustBe Right(
        Seq(
          VerificationActionIndicator(13L, "verify"),
          VerificationActionIndicator(14L, "match")
        )
      )
    }

    "return an error when verificationResourceRef is invalid" in {
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
        subcontractors = Seq.empty,
        verifications = Seq(
          VerificationDetails(
            subcontractorName = "Subcontractor 1",
            verificationResourceRef = "not-a-long",
            proceedVerification = true
          )
        )
      )

      VerificationActionIndicatorBuilder.buildEither(request) mustBe
        Left("Invalid long value for verificationResourceRef: 'not-a-long'")
    }

    "throw an exception from build when verificationResourceRef is invalid" in {
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
        subcontractors = Seq.empty,
        verifications = Seq(
          VerificationDetails(
            subcontractorName = "Subcontractor 1",
            verificationResourceRef = "invalid",
            proceedVerification = true
          )
        )
      )

      val exception = intercept[IllegalArgumentException] {
        VerificationActionIndicatorBuilder.build(request)
      }

      exception.getMessage mustBe "Invalid long value for verificationResourceRef: 'invalid'"
    }
  }
}
