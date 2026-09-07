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
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationCommitStatus
import play.api.libs.json.*

class FinalValidationCommitStatusSpec extends SpecBase {

  "FinalValidationCommitStatus" - {

    "must serialize and deserialize all valid status values" in {
      val statuses: Seq[(FinalValidationCommitStatus, String)] = Seq(
        (FinalValidationCommitStatus.Pending, "Pending"),
        (FinalValidationCommitStatus.NotRequired, "NotRequired"),
        (FinalValidationCommitStatus.Committed, "Committed"),
        (FinalValidationCommitStatus.Failed, "Failed")
      )

      statuses.foreach { case (status, jsonString) =>
        val json = JsString(jsonString)
        Json.toJson(status) mustBe json
        json.as[FinalValidationCommitStatus] mustBe status
      }
    }

    "must fail to deserialize an invalid status string" in {
      JsString("InvalidStatus").validate[FinalValidationCommitStatus] mustBe a[JsError]
    }
  }
}
