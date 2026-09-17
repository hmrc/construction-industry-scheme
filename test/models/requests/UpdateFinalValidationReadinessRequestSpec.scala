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
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationDraftIssue
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateFinalValidationReadinessRequest

class UpdateFinalValidationReadinessRequestSpec extends SpecBase {

  "UpdateFinalValidationReadinessRequest" - {
    "must serialize and deserialize correctly" in {
      val request = UpdateFinalValidationReadinessRequest(
        subcontractorId = 1L,
        issues = Seq(FinalValidationDraftIssue("firstName", Some("Field is required")))
      )

      Json.toJson(request).as[UpdateFinalValidationReadinessRequest] mustBe request
    }
  }
}
