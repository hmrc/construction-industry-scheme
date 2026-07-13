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
import play.api.libs.json.{JsNull, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateVerificationSubmissionRequest

import java.time.LocalDateTime

class UpdateVerificationSubmissionRequestSpec extends SpecBase {

  "UpdateVerificationSubmissionRequest" - {

    "must read from JSON" in {
      val json = Json.obj(
        "instanceId"                   -> "1",
        "verificationBatchResourceRef" -> 2001L,
        "submittableStatus"            -> "SUBMITTED",
        "submissionRequestDate"        -> "2026-06-15T03:30:52",
        "hmrcMarkGenerated"            -> "hmrc-mark",
        "govtalkErrorCode"             -> JsNull,
        "govtalkErrorType"             -> JsNull,
        "govtalkErrorMessage"          -> JsNull
      )

      val result = json.as[UpdateVerificationSubmissionRequest]

      result.instanceId mustBe "1"
      result.verificationBatchResourceRef mustBe 2001L
      result.submittableStatus mustBe "SUBMITTED"
      result.hmrcMarkGenerated mustBe Some("hmrc-mark")
      result.submissionRequestDate.value mustBe LocalDateTime.parse("2026-06-15T03:30:52")
      result.govtalkErrorCode mustBe None
      result.govtalkErrorType mustBe None
      result.govtalkErrorMessage mustBe None
    }

    "must write to JSON" in {
      val model = UpdateVerificationSubmissionRequest(
        instanceId = "1",
        verificationBatchResourceRef = 2001L,
        submittableStatus = "SUBMITTED",
        submissionRequestDate = Some(LocalDateTime.parse("2026-06-15T03:30:52")),
        hmrcMarkGenerated = Some("hmrc-mark")
      )

      val json = Json.toJson(model)

      (json \ "instanceId").as[String] mustBe "1"
      (json \ "verificationBatchResourceRef").as[Long] mustBe 2001L
      (json \ "submittableStatus").as[String] mustBe "SUBMITTED"
      (json \ "submissionRequestDate").as[LocalDateTime] mustBe LocalDateTime.parse("2026-06-15T03:30:52")
      (json \ "hmrcMarkGenerated").as[String] mustBe "hmrc-mark"
    }
  }
}
