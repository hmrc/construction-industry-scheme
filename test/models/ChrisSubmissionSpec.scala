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
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisSubmissionRequest
import uk.gov.hmrc.constructionindustryscheme.models.{ChRISSubmission, MonthlyReturnType}

class ChrisSubmissionSpec extends SpecBase with Matchers with MockitoSugar {

  "buildPayload creates correct payload for non-agent request" in {

    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = false,
      isResubmission = false,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123",
      returnType = MonthlyReturnType.Nil,
      informationCorrect = "yes",
      inactivity = "yes",
      standard = None
    )

    val payload = ChRISSubmission.buildPayload(submissionRequest)

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "999"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "XYZ123"
    (payload.envelope \\ "PeriodEnd").text                                                          shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text                                                                shouldBe "1234567890"
    (payload.envelope \\ "AOref").text                                                              shouldBe "123/AB456"
  }

  "buildPayload creates correct payload for agent request" in {
    val submissionRequest = ChrisSubmissionRequest(
      utr = "1234567890",
      aoReference = "123/AB456",
      monthYear = "2025-05",
      email = Some("test@test.com"),
      isAgent = true,
      isResubmission = false,
      clientTaxOfficeNumber = "999",
      clientTaxOfficeRef = "XYZ123",
      returnType = MonthlyReturnType.Nil,
      informationCorrect = "yes",
      inactivity = "yes",
      standard = None
    )

    val payload = ChRISSubmission.buildPayload(submissionRequest)

    payload.irMark.length should be > 0

    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("")    shouldBe "999"
    (payload.envelope \\ "Key").find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") shouldBe "XYZ123"
    (payload.envelope \\ "PeriodEnd").text                                                          shouldBe "2025-05-05"
    (payload.envelope \\ "UTR").text                                                                shouldBe "1234567890"
    (payload.envelope \\ "AOref").text                                                              shouldBe "123/AB456"
    (payload.envelope \\ "Inactivity").text                                                         shouldBe "yes"
  }

  "parsePeriodEnd throws for invalid date" in {
    val thrown = intercept[IllegalArgumentException] {
      ChRISSubmission.parsePeriodEnd("wrong-format")
    }
    thrown.getMessage should include("Invalid monthYear")
  }

}
