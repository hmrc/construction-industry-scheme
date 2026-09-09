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
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.{FinalValidationDraftIssue, FinalValidationSubcontractorDetails}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateFinalValidationDraftRequest, CreateFinalValidationDraftSubcontractor}

class CreateFinalValidationDraftRequestSpec extends SpecBase {

  "CreateFinalValidationDraftSubcontractor" - {
    "must serialize and deserialize correctly" in {
      val subcontractor = CreateFinalValidationDraftSubcontractor(
        subcontractorId = 1L,
        subbieResourceRef = 100L,
        baseVersion = Some(1),
        subcontractorType = Some("SoleTrader"),
        displayName = "John Doe",
        details = FinalValidationSubcontractorDetails(firstName = Some("John")),
        issues = Seq(FinalValidationDraftIssue("firstName", Some("Invalid format")))
      )

      Json.toJson(subcontractor).as[CreateFinalValidationDraftSubcontractor] mustBe subcontractor
    }
  }

  "CreateFinalValidationDraftRequest" - {
    "must serialize and deserialize correctly" in {
      val request = CreateFinalValidationDraftRequest(
        instanceId = "instance-123",
        context = "context-abc",
        subcontractors = Seq.empty
      )

      Json.toJson(request).as[CreateFinalValidationDraftRequest] mustBe request
    }
  }
}
