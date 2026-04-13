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

package uk.gov.hmrc.constructionindustryscheme.models

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDateTime

final case class VerificationBatch(
  verificationBatchId: Long,
  schemeId: Long,
  verificationsCounter: Option[Long],
  verifBatchResourceRef: Option[Long],
  proceedSession: Option[String],
  confirmArrangement: Option[String],
  confirmCorrect: Option[String],
  status: Option[String],
  verificationNumber: Option[String],
  createDate: Option[LocalDateTime],
  lastUpdate: Option[LocalDateTime],
  version: Option[Int]
)

object VerificationBatch:
  given format: OFormat[VerificationBatch] = Json.format[VerificationBatch]
