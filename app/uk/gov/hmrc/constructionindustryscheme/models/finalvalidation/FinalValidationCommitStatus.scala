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

sealed trait FinalValidationCommitStatus

object FinalValidationCommitStatus {

  case object Pending     extends FinalValidationCommitStatus
  case object NotRequired extends FinalValidationCommitStatus
  case object Committed   extends FinalValidationCommitStatus
  case object Failed      extends FinalValidationCommitStatus

  given format: Format[FinalValidationCommitStatus] =
    new Format[FinalValidationCommitStatus] {

      override def writes(status: FinalValidationCommitStatus): JsValue =
        status match {
          case Pending     => JsString("Pending")
          case NotRequired => JsString("NotRequired")
          case Committed   => JsString("Committed")
          case Failed      => JsString("Failed")
        }

      override def reads(json: JsValue): JsResult[FinalValidationCommitStatus] =
        json.validate[String].flatMap {
          case "Pending"     => JsSuccess(Pending)
          case "NotRequired" => JsSuccess(NotRequired)
          case "Committed"   => JsSuccess(Committed)
          case "Failed"      => JsSuccess(Failed)
          case other         => JsError(s"Invalid Final Validation commit status: $other")
        }
    }
}
