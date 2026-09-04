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

package uk.gov.hmrc.constructionindustryscheme.models.finalvalidation

import play.api.libs.json.*

sealed trait FinalValidationReadiness { def key: String }

object FinalValidationReadiness {
  case object Incomplete extends FinalValidationReadiness { val key = "Incomplete" }
  case object Complete extends FinalValidationReadiness { val key = "Complete" }

  given format: Format[FinalValidationReadiness] = new Format[FinalValidationReadiness] {

    override def reads(json: JsValue): JsResult[FinalValidationReadiness] =
      json.validate[String].flatMap {
        case Incomplete.key => JsSuccess(Incomplete)
        case Complete.key   => JsSuccess(Complete)
        case other          => JsError(s"Unknown FinalValidationDraftStatus: $other")
      }

    override def writes(value: FinalValidationReadiness): JsValue =
      JsString(value.key)
  }

}
