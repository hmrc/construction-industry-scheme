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

package uk.gov.hmrc.constructionindustryscheme.models.response

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.constructionindustryscheme.models.*

import java.time.{LocalDateTime, ZoneOffset}

case class GetSubmittedMonthlyReturnsDataResponse(
  scheme: SchemeData,
  monthlyReturnId: Long,
  taxYear: Int,
  taxMonth: Int,
  nilReturnIndicator: String,
  monthlyReturnItems: Seq[MonthlyReturnItem],
  submission: SubmissionData
)

object GetSubmittedMonthlyReturnsDataResponse {
  given format: OFormat[GetSubmittedMonthlyReturnsDataResponse] = Json.format[GetSubmittedMonthlyReturnsDataResponse]

  def fromProxyResponse(
    response: GetSubmittedMonthlyReturnsDataProxyResponse
  ): GetSubmittedMonthlyReturnsDataResponse =
    (response.monthlyReturn.headOption, response.submission.headOption) match {
      case (Some(monthlyReturn), Some(submission)) =>
        GetSubmittedMonthlyReturnsDataResponse(
          scheme = SchemeData(
            taxOfficeNumber = response.scheme.taxOfficeNumber,
            taxOfficeReference = response.scheme.taxOfficeReference,
            name = response.scheme.name.getOrElse("No name provided")
          ),
          monthlyReturnId = monthlyReturn.monthlyReturnId,
          taxYear = monthlyReturn.taxYear,
          taxMonth = monthlyReturn.taxMonth,
          nilReturnIndicator = monthlyReturn.nilReturnIndicator.getOrElse("N"),
          monthlyReturnItems = response.monthlyReturnItems,
          submission = SubmissionData(
            submissionId = submission.submissionId,
            submissionType = Some(submission.submissionType),
            activeObjectId = submission.activeObjectId,
            status = submission.status,
            hmrcMarkGenerated = submission.hmrcMarkGenerated,
            hmrcMarkGgis = submission.hmrcMarkGgis,
            emailRecipient = submission.emailRecipient,
            acceptedTime = submission.acceptedTime.map(x => LocalDateTime.parse(x).toInstant(ZoneOffset.UTC))
          )
        )

      case _ =>
        throw new RuntimeException("Missing monthlyReturn or submission data")
    }
}
