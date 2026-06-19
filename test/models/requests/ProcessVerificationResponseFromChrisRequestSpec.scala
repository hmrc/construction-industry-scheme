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
import uk.gov.hmrc.constructionindustryscheme.models.VerificationResults
import uk.gov.hmrc.constructionindustryscheme.models.requests.ProcessVerificationResponseFromChrisRequest

class ProcessVerificationResponseFromChrisRequestSpec extends SpecBase {

  "ProcessVerificationResponseFromChrisRequest" - {
    "create an instance with the expected values" in {
      val verificationResult = VerificationResults(
        subbieResourceRef = 13L,
        matched = Some("Y"),
        verified = Some("N"),
        verificationNumber = Some("V1000000007"),
        taxTreatment = Some("net")
      )

      val request = ProcessVerificationResponseFromChrisRequest(
        instanceId = "1",
        verifBatchResourceRef = 5L,
        submittableStatus = "SUBMITTED",
        acceptedTime = Some("2017-04-06T08:46:08.081"),
        hmrcMarkGgis = Some("hmrc-mark"),
        verificationResults = Seq(verificationResult)
      )

      request.instanceId mustBe "1"
      request.verifBatchResourceRef mustBe 5L
      request.submittableStatus mustBe "SUBMITTED"
      request.acceptedTime mustBe Some("2017-04-06T08:46:08.081")
      request.hmrcMarkGgis mustBe Some("hmrc-mark")
      request.verificationResults mustBe Seq(verificationResult)
    }
  }
}
