/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryscheme.models.audit

import play.api.libs.json.{Format, JsObject, JsValue, Json, OWrites}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

trait AuditEventModel {
  private val auditSource: String = "construction-industry-scheme"
  val auditType: String
  val detailJson: JsValue

  def extendedDataEvent: ExtendedDataEvent =
    ExtendedDataEvent(auditSource = auditSource, auditType = auditType, detail = detailJson)
}

case class MonthlyNilReturnRequestEvent(payload: JsValue) extends AuditEventModel {
  override val auditType: String = "monthlyNilReturnRequest"
  override val detailJson: JsValue = Json.toJson(this)
}

case class MonthlyNilReturnResponseEvent(response: AuditResponseReceivedModel) extends AuditEventModel {
  override val auditType: String = "monthlyNilReturnResponse"
  override val detailJson: JsValue = Json.toJson(this)
}

object MonthlyNilReturnRequestEvent {
  implicit val formats: Format[MonthlyNilReturnRequestEvent] = Json.format[MonthlyNilReturnRequestEvent]
}

object MonthlyNilReturnResponseEvent {
  implicit val formats: Format[MonthlyNilReturnResponseEvent] = Json.format[MonthlyNilReturnResponseEvent]
}