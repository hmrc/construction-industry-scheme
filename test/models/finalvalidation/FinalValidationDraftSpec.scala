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

class FinalValidationDraftSpec extends SpecBase {

  private val completeSubcontractor = FinalValidationDraftSubcontractor(
    subcontractorId = 1L,
    subbieResourceRef = 100L,
    baseVersion = None,
    subcontractorType = None,
    displayName = "Subcontractor 1",
    base = FinalValidationSubcontractorDetails(),
    proposed = FinalValidationSubcontractorDetails(),
    changedTargets = Set.empty,
    issues = Seq.empty,
    readiness = FinalValidationReadiness.Complete,
    commitStatus = FinalValidationCommitStatus.Committed
  )

  private val incompleteSubcontractor = completeSubcontractor.copy(
    subcontractorId = 2L,
    readiness = FinalValidationReadiness.Incomplete
  )

  "FinalValidationDraft" - {

    "subcontractor" - {
      "must return matching subcontractor when ID exists" in {
        val draft = FinalValidationDraft(Seq(completeSubcontractor, incompleteSubcontractor))
        draft.subcontractor(1L) mustBe Some(completeSubcontractor)
      }

      "must return None when ID does not exist" in {
        val draft = FinalValidationDraft(Seq(completeSubcontractor))
        draft.subcontractor(999L) mustBe None
      }
    }

    "allComplete" - {
      "must return true when all subcontractors are Complete" in {
        val draft = FinalValidationDraft(Seq(completeSubcontractor))
        draft.allComplete mustBe true
      }

      "must return true when subcontractors list is empty" in {
        val draft = FinalValidationDraft(Seq.empty)
        draft.allComplete mustBe true
      }

      "must return false when any subcontractor is Incomplete" in {
        val draft = FinalValidationDraft(Seq(completeSubcontractor, incompleteSubcontractor))
        draft.allComplete mustBe false
      }
    }

    "must serialize and deserialize correctly" in {
      val draft = FinalValidationDraft(Seq(completeSubcontractor))
      Json.toJson(draft).as[FinalValidationDraft] mustBe draft
    }
  }
}
