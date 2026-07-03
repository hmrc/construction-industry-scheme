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

  private case class Column(
    heading: String,
    width: Int,
    value: PollReportContent => String
  )

  private val Indent: String =
    "    "

  private val ColumnSeparator: String =
    " "

  private val Ellipsis: String =
    "..."

  private val SectionSeparator: String =
    "=========================================================================================================================================================================="

  private val Underline: String =
    "    ----------------------------------------------------------------------------------------------------------------------------------------------------------------------"

  private val DateTimeFormatter: DateTimeFormatter =
    java.time.format.DateTimeFormatter.ofPattern(
      "dd-MM-yy HH:mm:ss"
    )

  private val Columns: Seq[Column] =
    Seq(
      Column(
        heading = "USER",
        width = 15,
        value = _.user
      ),
      Column(
        heading = "SUBMISSION_TYPE",
        width = 18,
        value = _.submissionType
      ),
      Column(
        heading = "SUBMISSION_ID",
        width = 20,
        value = _.submissionId
      ),
      Column(
        heading = "GOVTALK_REQUEST_STATUS",
        width = 25,
        value = _.govTalkRequestStatus
      ),
      Column(
        heading = "CURRENT_RETURN_STATUS",
        width = 24,
        value = _.currentReturnStatus
      ),
      Column(
        heading = "EMP REFERENCE",
        width = 16,
        value = _.employerReference
      ),
      Column(
        heading = "CORRELATION ID",
        width = 32,
        value = _.correlationId
      ),
      Column(
        heading = "AGENT ID",
        width = 8,
        value = _.agentId
      )
    )

  private val Header: String =
    Indent +
      Columns
        .map { column =>
          formatField(
            column.heading,
            column.width
          )
        }
        .mkString(ColumnSeparator)

  def format(
    reportContent: Seq[PollReportContent],
    generatedAt: LocalDateTime
  ): String = {

    val rows =
      reportContent.map(formatRow)

    (
      Seq(
        SectionSeparator,
        s"BATCH POLLING RESULTS FOR ${generatedAt.format(DateTimeFormatter)}",
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
  ): String =
    Indent +
      Columns
        .map { column =>
          formatField(
            column.value(content),
            column.width
          )
        }
        .mkString(ColumnSeparator)

  private def formatField(
    value: String,
    width: Int
  ): String = {

    val safeValue =
      Option(value).getOrElse("")

    val displayValue =
      if (safeValue.length > width) {
        safeValue.take(width - Ellipsis.length) + Ellipsis
      } else {
        safeValue
      }

    displayValue.padTo(width, ' ')
  }
}
