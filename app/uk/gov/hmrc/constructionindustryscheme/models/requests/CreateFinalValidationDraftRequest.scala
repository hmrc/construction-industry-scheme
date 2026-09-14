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

package uk.gov.hmrc.constructionindustryscheme.models.requests

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.*

case class CreateFinalValidationDraftSubcontractor(
  subcontractorId: Long,
  subbieResourceRef: Long,
  baseVersion: Option[Int],
  subcontractorType: Option[String],
  displayName: String,
  details: FinalValidationSubcontractorDetails,
  issues: Seq[FinalValidationDraftIssue]
)

object CreateFinalValidationDraftSubcontractor {
  given format: Format[CreateFinalValidationDraftSubcontractor] = Json.format[CreateFinalValidationDraftSubcontractor]
}

case class CreateFinalValidationDraftRequest(
  instanceId: String,
  context: String,
  subcontractors: Seq[CreateFinalValidationDraftSubcontractor]
)

object CreateFinalValidationDraftRequest {
  given format: Format[CreateFinalValidationDraftRequest] = Json.format[CreateFinalValidationDraftRequest]
}
