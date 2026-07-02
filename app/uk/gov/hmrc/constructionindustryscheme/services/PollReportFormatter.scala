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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PollReportFormatter {

  private val SectionSeparator: String =
    "=========================================================================================================================================================================="

  private val ReportTitle: String =
    "BATCH POLLING RESULTS FOR"

  private val Header: String =
    "    USER            SUBMISSION_TYPE    SUBMISSION_ID        GOVTALK_REQUEST_STATUS    CURRENT_RETURN_STATUS    EMP REFERENCE    CORRELATION ID                   AGENT ID"

  private val Underline: String =
    "    ----------------------------------------------------------------------------------------------------------------------------------------------------------------------"

  private val FatalErrorStatus: String =
    "FATAL ERROR"

  private val Ellipsis: String =
    "..."

  private val ColumnSeparator: String =
    " "

  private val ReportDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss")

  private val UserWidth                 = 15
  private val SubmissionTypeWidth       = 18
  private val SubmissionIdWidth         = 20
  private val GovTalkRequestStatusWidth = 25
  private val CurrentReturnStatusWidth  = 24
  private val EmployerReferenceWidth    = 16
  private val CorrelationIdWidth        = 32
  private val AgentIdWidth              = 8

  def format(
    reportContent: Seq[PollReportContent],
    generatedAt: LocalDateTime
  ): String = {

    val rows =
      reportContent.map(formatRow)

    (
      Seq(
        SectionSeparator,
        s"$ReportTitle ${generatedAt.format(ReportDateTimeFormatter)}",
        Header,
        Underline
      ) ++
        rows ++
        Seq(
          "",
          Underline,
          SectionSeparator
        )
    ).mkString(System.lineSeparator())
  }

  private def formatRow(
    content: PollReportContent
  ): String = {

    val currentReturnStatus =
      if (content.recoverableError) {
        FatalErrorStatus
      } else {
        content.currentReturnStatus
      }

    val fields =
      Seq(
        formatField(content.user, UserWidth),
        formatField(content.submissionType, SubmissionTypeWidth),
        formatField(content.submissionId, SubmissionIdWidth),
        formatField(
          content.govTalkRequestStatus,
          GovTalkRequestStatusWidth
        ),
        formatField(
          currentReturnStatus,
          CurrentReturnStatusWidth
        ),
        formatField(
          content.employerReference,
          EmployerReferenceWidth
        ),
        formatField(
          content.correlationId,
          CorrelationIdWidth
        ),
        formatField(
          content.agentId,
          AgentIdWidth
        )
      )

    "    " + fields.mkString(ColumnSeparator)
  }

  private def formatField(
    value: String,
    width: Int
  ): String = {

    val safeValue =
      Option(value).getOrElse("")

    val formattedValue =
      if (safeValue.length > width) {
        safeValue.take(width - Ellipsis.length) + Ellipsis
      } else {
        safeValue
      }

    formattedValue.padTo(width, ' ')
  }
}
