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

package models.requests

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.Subcontractor
import uk.gov.hmrc.constructionindustryscheme.models.requests.{UpdateSubcontractorForEditRequest, UpdateVerificationForEditRequest}

import java.time.LocalDateTime

class UpdateSubcontractorForEditRequestSpec extends SpecBase {

  private val subcontractor =
    Subcontractor(
      subcontractorId = 123L,
      utr = Some("1234567890"),
      pageVisited = Some(1),
      partnerUtr = None,
      crn = Some("CRN123"),
      firstName = Some("John"),
      nino = Some("AA123456A"),
      secondName = Some("Middle"),
      surname = Some("Smith"),
      partnershipTradingName = None,
      tradingName = Some("John Smith Trading"),
      subcontractorType = Some("soletrader"),
      addressLine1 = Some("1 Test Street"),
      addressLine2 = Some("Test Area"),
      addressLine3 = Some("London"),
      addressLine4 = Some("Greater London"),
      country = Some("United Kingdom"),
      postcode = Some("AA1 1AA"),
      emailAddress = Some("john@example.com"),
      phoneNumber = Some("02071234567"),
      mobilePhoneNumber = Some("07123456789"),
      worksReferenceNumber = Some("WRN123"),
      createDate = Some(LocalDateTime.of(2026, 1, 1, 12, 0)),
      lastUpdate = Some(LocalDateTime.of(2026, 1, 2, 12, 0)),
      subbieResourceRef = Some(1001L),
      matched = Some("Y"),
      autoVerified = Some("N"),
      verified = Some("Y"),
      verificationNumber = Some("V123456"),
      taxTreatment = Some("Gross"),
      verificationDate = Some(LocalDateTime.of(2026, 1, 3, 12, 0)),
      version = Some(4),
      updatedTaxTreatment = Some("Gross"),
      lastMonthlyReturnDate = Some(LocalDateTime.of(2025, 12, 31, 12, 0)),
      pendingVerifications = Some(0)
    )

  "UpdateSubcontractorForEditRequest" - {

    "must serialise and deserialise when verificationForEdit is present" in {

      val request =
        UpdateSubcontractorForEditRequest(
          cisId = "CIS-123",
          subcontractor = subcontractor,
          verificationForEdit = Some(
            UpdateVerificationForEditRequest(
              verificationBatchResourceRef = 5001L,
              verificationResourceRef = 6001L
            )
          )
        )

      val json =
        Json.toJson(request)

      json.as[UpdateSubcontractorForEditRequest] mustBe request
    }

    "must serialise and deserialise when verificationForEdit is absent" in {

      val request =
        UpdateSubcontractorForEditRequest(
          cisId = "CIS-123",
          subcontractor = subcontractor,
          verificationForEdit = None
        )

      val json =
        Json.toJson(request)

      val result =
        json.as[UpdateSubcontractorForEditRequest]

      result mustBe request
      result.verificationForEdit mustBe None
    }

    "must round trip through JSON" in {

      val request =
        UpdateSubcontractorForEditRequest(
          cisId = "CIS-123",
          subcontractor = subcontractor,
          verificationForEdit = Some(
            UpdateVerificationForEditRequest(
              verificationBatchResourceRef = 5001L,
              verificationResourceRef = 6001L
            )
          )
        )

      val json =
        Json.toJson(request)

      val result =
        Json.fromJson[UpdateSubcontractorForEditRequest](json)

    }

    "must fail to deserialise when cisId is missing" in {

      val json =
        Json.obj(
          "subcontractor" -> Json.toJson(subcontractor)
        )

      json
        .validate[UpdateSubcontractorForEditRequest]
        .isError mustBe true
    }

    "must fail to deserialise when subcontractor is missing" in {

      val json =
        Json.obj(
          "cisId" -> "CIS-123"
        )

      json
        .validate[UpdateSubcontractorForEditRequest]
        .isError mustBe true
    }
  }
}
