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

package models.finalvalidation

import base.SpecBase
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.*

class FinalValidationDraftSubcontractorSpec extends SpecBase {

  "FinalValidationDraftSubcontractor" - {

    "must serialize and deserialize correctly" in {
      val subcontractor = FinalValidationDraftSubcontractor(
        subcontractorId = 10L,
        subbieResourceRef = 20L,
        baseVersion = Some(1),
        subcontractorType = Some("SoleTrader"),
        displayName = "John Doe",
        base = FinalValidationSubcontractorDetails(firstName = Some("John")),
        proposed = FinalValidationSubcontractorDetails(firstName = Some("Johnny")),
        changedTargets = Set("firstName"),
        issues = Seq(FinalValidationDraftIssue("firstName", Some("Invalid format"))),
        readiness = FinalValidationReadiness.Incomplete,
        commitStatus = FinalValidationCommitStatus.Pending
      )

      Json.toJson(subcontractor).as[FinalValidationDraftSubcontractor] mustBe subcontractor
    }
  }
}
