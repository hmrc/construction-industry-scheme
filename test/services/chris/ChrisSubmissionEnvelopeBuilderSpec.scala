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

package services.chris

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisSubmissionEnvelopeBuilder

class ChrisSubmissionEnvelopeBuilderSpec
  extends SpecBase
    with Matchers
    with MockitoSugar {

  def fakeEnrolments(taxOfficeNumber: String, taxOfficeReference: String): Enrolments = {
    val identifiers = Seq(
      new EnrolmentIdentifier("TaxOfficeNumber", taxOfficeNumber),
      new EnrolmentIdentifier("TaxOfficeReference", taxOfficeReference)
    )
    val enrolment = new Enrolment("HMRC-CIS-ORG", identifiers, "activated", None)
    Enrolments(Set(enrolment))
  }

  "buildPayload creates correct payload for non-agent request" in {
    val authRequest = mock[AuthenticatedRequest[_]]
    val enrolments = fakeEnrolments("123", "ABC456")
    when(authRequest.enrolments).thenReturn(enrolments)

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      informationCorrect = "yes",
      inactivity = "no",
      monthYear = "2025-05",
      email = "test@test.com",
      isAgent = false,
      clientTaxOfficeNumber = "",
      clientTaxOfficeRef = ""
    )

    val correlationId = "test-corr-id"

    val payload = ChrisSubmissionEnvelopeBuilder.buildPayload(submissionRequest, authRequest, correlationId)

    payload.correlationId shouldBe correlationId
    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("") shouldBe "123"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "ABC456"
    (payload.envelope \\ "PeriodEnd").text shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text shouldBe "1234567890"
    (payload.envelope \\ "AOref").text shouldBe "123/AB456"
  }

  "buildPayload creates correct payload for agent request" in {
    val authRequest = mock[AuthenticatedRequest[_]]
    // Irrelevant enrolments for agent flow, but still default empty
    when(authRequest.enrolments).thenReturn(Enrolments(Set.empty))

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      informationCorrect = "yes",
      inactivity = "yes",
      monthYear = "2025-05",
      email = "test@test.com",
      isAgent = true,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123"
    )

    val correlationId = "agent-corr-id"

    val payload = ChrisSubmissionEnvelopeBuilder.buildPayload(submissionRequest, authRequest, correlationId)

    payload.correlationId shouldBe correlationId
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("") shouldBe "999"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "XYZ123"
    (payload.envelope \\ "PeriodEnd").text shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text shouldBe "1234567890"
    (payload.envelope \\ "AOref").text shouldBe "123/AB456"
    (payload.envelope \\ "Inactivity").text shouldBe "yes"
  }

  "parsePeriodEnd throws for invalid date" in {
    val thrown = intercept[IllegalArgumentException] {
      ChrisSubmissionEnvelopeBuilder.parsePeriodEnd("wrong-format")
    }
    thrown.getMessage should include ("Invalid monthYear")
  }

  "extractTaxOfficeFromCisEnrolment returns None if identifiers missing" in {
    val enrolments = Enrolments(Set.empty)
    ChrisSubmissionEnvelopeBuilder.extractTaxOfficeFromCisEnrolment(enrolments) shouldBe None
  }

}