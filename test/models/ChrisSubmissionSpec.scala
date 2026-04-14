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
import org.mockito.Mockito.*
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.{ChRISSubmission, MonthlyReturnType}

class ChrisSubmissionSpec extends SpecBase with Matchers with MockitoSugar {

  private def fakeEnrolments(taxOfficeNumber: String, taxOfficeReference: String): Enrolments = {
    val identifiers = Seq(
      EnrolmentIdentifier("TaxOfficeNumber", taxOfficeNumber),
      EnrolmentIdentifier("TaxOfficeReference", taxOfficeReference)
    )
    val enrolment   = new Enrolment("HMRC-CIS-ORG", identifiers, "activated", None)
    Enrolments(Set(enrolment))
  }

  "buildPayload creates correct payload for non-agent request" in {
    val authRequest = mock[AuthenticatedRequest[_]]
    when(authRequest.enrolments).thenReturn(fakeEnrolments("123", "ABC456"))

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = false,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123",
      returnType = MonthlyReturnType.Nil,
      informationCorrect = "yes",
      inactivity = "yes",
      standard = None
    )

    val payload = ChRISSubmission.buildPayload(submissionRequest, authRequest)

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "123"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "ABC456"
    (payload.envelope \\ "PeriodEnd").text                                                          shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text                                                                shouldBe "1234567890"
    (payload.envelope \\ "AOref").text                                                              shouldBe "123/AB456"
  }

  "buildPayload creates correct payload for agent request" in {
    val authRequest = mock[AuthenticatedRequest[_]]
    when(authRequest.enrolments).thenReturn(Enrolments(Set.empty)) // ignored for agent

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = true,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123",
      returnType = MonthlyReturnType.Nil,
      informationCorrect = "yes",
      inactivity = "yes",
      standard = None
    )

    val payload = ChRISSubmission.buildPayload(submissionRequest, authRequest)

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "999"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "XYZ123"
    (payload.envelope \\ "PeriodEnd").text                                                          shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text                                                                shouldBe "1234567890"
    (payload.envelope \\ "AOref").text                                                              shouldBe "123/AB456"
    (payload.envelope \\ "Inactivity").text                                                         shouldBe "yes"
  }

  "buildPayload throws for non-agent request when CIS enrolment identifiers are missing" in {
    val authRequest = mock[AuthenticatedRequest[_]]
    when(authRequest.enrolments).thenReturn(Enrolments(Set.empty))

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = false,
      clientTaxOfficeNumber = "",
      clientTaxOfficeRef = "",
      returnType = MonthlyReturnType.Nil,
      informationCorrect = "yes",
      inactivity = "no",
      standard = None
    )

    val thrown = intercept[IllegalStateException] {
      ChRISSubmission.buildPayload(submissionRequest, authRequest)
    }

    thrown.getMessage should include("Missing CIS enrolment identifiers")
  }

  "parsePeriodEnd throws for invalid date" in {
    val thrown = intercept[IllegalArgumentException] {
      ChRISSubmission.parsePeriodEnd("wrong-format")
    }
    thrown.getMessage should include("Invalid monthYear")
  }

  "extractTaxOfficeFromCisEnrolment returns None if identifiers missing" in {
    val enrolments = Enrolments(Set.empty)
    ChRISSubmission.extractTaxOfficeFromCisEnrolment(enrolments) shouldBe None
  }

//  "buildEnvelope should build envelope with correlationId, keys, periodEnd, sender and include cisReturn" in {
//    val cisReturn: Node = <CISreturn>
//      <Test>ok</Test>
//    </CISreturn>
//
//    val xml: Elem = ChRISSubmission.buildEnvelope(
//      taxOfficeNumber = "123",
//      taxOfficeReference = "ABC456",
//      correlationId = "corr-id",
//      gatewayTimestamp = "2025-05-01T12:00:00",
//      periodEnd = "2025-05-05",
//      sender = "Company",
//      cisReturn = cisReturn
//    )
//
//    (xml \\ "CorrelationID").text mustBe "corr-id"
//
//    val keys = xml \\ "Key"
//    keys.find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("") mustBe "123"
//    keys.find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") mustBe "ABC456"
//
//    (xml \\ "PeriodEnd").text mustBe "2025-05-05"
//    (xml \\ "Sender").text mustBe "Company"
//
//    (xml \\ "CISreturn" \\ "Test").text mustBe "ok"
//  }
}
