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
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisVerificationRequest

class CisVerificationSubmissionSpec extends SpecBase with Matchers with MockitoSugar {

  private def fakeEnrolments(taxOfficeNumber: String, taxOfficeReference: String): Enrolments = {
    val identifiers = Seq(
      EnrolmentIdentifier("TaxOfficeNumber", taxOfficeNumber),
      EnrolmentIdentifier("TaxOfficeReference", taxOfficeReference)
    )
    val enrolment   = new Enrolment("HMRC-CIS-ORG", identifiers, "activated", None)
    Enrolments(Set(enrolment))
  }

  private val subcontractor = SubcontractorCurrentVerification(
    subcontractorId = 1L,
    subbieResourceRef = Some(10L),
    firstName = Some("John"),
    secondName = Some("Q"),
    surname = Some("Smith"),
    tradingName = Some("ACME"),
    utr = Some("1111111111"),
    nino = Some("AA123456A"),
    crn = Some("AC012345"),
    partnerUtr = Some("5860920998"),
    partnershipTradingName = Some("ACME trading"),
    subcontractorType = Some("soletrader"),
    addressLine1 = Some("Line 1"),
    addressLine2 = Some("Line 2"),
    addressLine3 = Some("Line 3"),
    addressLine4 = Some("Line 4"),
    country = Some("UK"),
    postcode = Some("NE1 1AA"),
    worksReferenceNumber = Some("WRN123")
  )

  "buildPayload creates correct payload for non-agent request" in {

    val enrolments = fakeEnrolments("123", "ABC456")

    val request = ChrisVerificationRequest(
      instanceId = "id-1",
      contractorUTR = "1234567890",
      contractorAORef = "123/AB456",
      verificationBatchId = "batch-1",
      verificationBatchResourceRef = "batch-ref",
      emailRecipient = Some("test@test.com"),
      subcontractors = Seq(subcontractor),
      verifications = Seq.empty,
      isAgent = false,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123"
    )

    val payload =
      CisVerificationSubmission.buildPayload(request, enrolments)

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "123"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "ABC456"

    (payload.envelope \\ "Contractor" \\ "UTR").text shouldBe "1234567890"
    (payload.envelope \\ "AOref").text               shouldBe "123/AB456"

    (payload.envelope \\ "CISrequest").nonEmpty    shouldBe true
    (payload.envelope \\ "Subcontractor").nonEmpty shouldBe true
  }

  "buildPayload creates correct payload for agent request" in {

    val request = ChrisVerificationRequest(
      instanceId = "id-1",
      contractorUTR = "1234567890",
      contractorAORef = "123/AB456",
      verificationBatchId = "batch-1",
      verificationBatchResourceRef = "batch-ref",
      emailRecipient = Some("test@test.com"),
      subcontractors = Seq(subcontractor),
      verifications = Seq.empty,
      isAgent = true,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123"
    )

    val payload =
      CisVerificationSubmission.buildPayload(request, fakeEnrolments("ignored", "ignored"))

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "999"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "XYZ123"
  }

  "buildPayload throws for non-agent request when CIS enrolment identifiers are missing" in {

    val request = ChrisVerificationRequest(
      instanceId = "id-1",
      contractorUTR = "1234567890",
      contractorAORef = "123/AB456",
      verificationBatchId = "batch-1",
      verificationBatchResourceRef = "batch-ref",
      emailRecipient = None,
      subcontractors = Seq(subcontractor),
      verifications = Seq.empty,
      isAgent = false,
      clientTaxOfficeNumber = "",
      clientTaxOfficeRef = ""
    )

    val thrown = intercept[IllegalStateException] {
      CisVerificationSubmission.buildPayload(request, Enrolments(Set.empty))
    }

    thrown.getMessage should include("Missing CIS enrolment identifiers")
  }
}
