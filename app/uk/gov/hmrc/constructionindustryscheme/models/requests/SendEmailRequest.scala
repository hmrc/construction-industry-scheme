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

package uk.gov.hmrc.constructionindustryscheme.models.requests

import play.api.libs.json.{Json, OFormat}

sealed trait SendEmailRequest {
  def to: List[String]
  def templateId: String
  def parameters: Map[String, String]
}

object SendEmailRequest {}

final case class NilMonthlyReturnOrgSuccessEmail(
  to: List[String],
  templateId: String,
  parameters: Map[String, String]
) extends SendEmailRequest

object NilMonthlyReturnOrgSuccessEmail {
  private val TemplateId = "dtr_cis_nil_monthly_return_org_success"

  implicit val format: OFormat[NilMonthlyReturnOrgSuccessEmail] =
    Json.format[NilMonthlyReturnOrgSuccessEmail]

  def apply(email: String, month: String, year: String): NilMonthlyReturnOrgSuccessEmail =
    NilMonthlyReturnOrgSuccessEmail(
      to = List(email),
      templateId = TemplateId,
      parameters = Map(
        "month" -> month,
        "year"  -> year
      )
    )
}
