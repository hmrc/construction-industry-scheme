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

package services

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent
import uk.gov.hmrc.constructionindustryscheme.services.PollReportFormatter

import java.time.LocalDateTime

class PollReportFormatterSpec extends AnyFreeSpec with Matchers {

  "PollReportFormatter.format" - {

    "generate an empty report using the required structure" in {
      val generatedAt =
        LocalDateTime.of(2026, 8, 18, 15, 0, 0)

      val header =
        "| " +
          Seq(
            "USER".padTo(15, ' '),
            "SUBMISSION_TYPE".padTo(18, ' '),
            "SUBMISSION_ID".padTo(20, ' '),
            "GOVTALK_REQUEST_STATUS".padTo(25, ' '),
            "CURRENT_RETURN_STATUS".padTo(24, ' '),
            "EMP REFERENCE".padTo(16, ' '),
            "CORRELATION ID".padTo(32, ' '),
            "AGENT ID".padTo(8, ' ')
          ).mkString(" | ") +
          " |"

      val separator =
        "=" * header.length

      val underline =
        "-" * header.length

      val expectedReport =
        Seq(
          separator,
          "BATCH POLLING RESULTS FOR 18-08-26 15:00:00",
          header,
          underline,
          "",
          underline,
          separator
        ).mkString(System.lineSeparator())

      PollReportFormatter.format(
        Seq.empty,
        generatedAt
      ) mustBe expectedReport
    }

    "generate a populated report with columns aligned to the heading" in {
      val generatedAt =
        LocalDateTime.of(2026, 8, 18, 15, 0, 0)

      val reportContent =
        Seq(
          PollReportContent(
            user = "872",
            submissionType = "VERIFICATIONS",
            submissionId = "471524635",
            govTalkRequestStatus = "ACCEPTED",
            currentReturnStatus = "-",
            employerReference = "754/EZ00047",
            correlationId = "(not polled)",
            agentId = "-"
          ),
          PollReportContent(
            user = "14",
            submissionType = "MONTHLY_RETURN",
            submissionId = "471574312",
            govTalkRequestStatus = "ACCEPTED",
            currentReturnStatus = "ACCEPTED",
            employerReference = "313/AO313",
            correlationId = "70F60B9B909C4522BB2BE112EB95C220",
            agentId = "-"
          )
        )

      val header =
        "| " +
          Seq(
            "USER".padTo(15, ' '),
            "SUBMISSION_TYPE".padTo(18, ' '),
            "SUBMISSION_ID".padTo(20, ' '),
            "GOVTALK_REQUEST_STATUS".padTo(25, ' '),
            "CURRENT_RETURN_STATUS".padTo(24, ' '),
            "EMP REFERENCE".padTo(16, ' '),
            "CORRELATION ID".padTo(32, ' '),
            "AGENT ID".padTo(8, ' ')
          ).mkString(" | ") +
          " |"

      val firstRow =
        "| " +
          Seq(
            "872".padTo(15, ' '),
            "VERIFICATIONS".padTo(18, ' '),
            "471524635".padTo(20, ' '),
            "ACCEPTED".padTo(25, ' '),
            "-".padTo(24, ' '),
            "754/EZ00047".padTo(16, ' '),
            "(not polled)".padTo(32, ' '),
            "-".padTo(8, ' ')
          ).mkString(" | ") +
          " |"

      val secondRow =
        "| " +
          Seq(
            "14".padTo(15, ' '),
            "MONTHLY_RETURN".padTo(18, ' '),
            "471574312".padTo(20, ' '),
            "ACCEPTED".padTo(25, ' '),
            "ACCEPTED".padTo(24, ' '),
            "313/AO313".padTo(16, ' '),
            "70F60B9B909C4522BB2BE112EB95C220".padTo(32, ' '),
            "-".padTo(8, ' ')
          ).mkString(" | ") +
          " |"

      val separator =
        "=" * header.length

      val underline =
        "-" * header.length

      val expectedReport =
        Seq(
          separator,
          "BATCH POLLING RESULTS FOR 18-08-26 15:00:00",
          header,
          underline,
          firstRow,
          secondRow,
          "",
          underline,
          separator
        ).mkString(System.lineSeparator())

      PollReportFormatter.format(
        reportContent,
        generatedAt
      ) mustBe expectedReport
    }

    "must create underline and separator with the same length as the header row" in {
      val report =
        PollReportFormatter.format(
          Seq.empty,
          LocalDateTime.of(2026, 8, 18, 15, 0, 0)
        )

      val lines =
        report.linesIterator.toSeq

      val sectionSeparator =
        lines.head

      val header =
        lines(2)

      val underline =
        lines(3)

      sectionSeparator.length mustBe header.length
      underline.length mustBe header.length
      underline mustBe "-" * header.length
      sectionSeparator mustBe "=" * header.length
    }

    "generate rows with fixed column widths and visible separators" in {
      val generatedAt =
        LocalDateTime.of(2026, 8, 18, 15, 0, 0)

      val report =
        PollReportFormatter.format(
          Seq(
            PollReportContent(
              user = "872",
              submissionType = "VERIFICATIONS",
              submissionId = "471524635",
              govTalkRequestStatus = "ACCEPTED",
              currentReturnStatus = "-",
              employerReference = "754/EZ00047",
              correlationId = "(not polled)",
              agentId = "-"
            ),
            PollReportContent(
              user = "14",
              submissionType = "MONTHLY_RETURN",
              submissionId = "471574312",
              govTalkRequestStatus = "ACCEPTED",
              currentReturnStatus = "ACCEPTED",
              employerReference = "313/AO313",
              correlationId = "70F60B9B909C4522BB2BE112EB95C220",
              agentId = "-"
            )
          ),
          generatedAt
        )

      val lines =
        report.linesIterator.toSeq

      val header =
        lines(2)

      val firstRow =
        lines(4)

      val secondRow =
        lines(5)

      firstRow.length mustBe header.length
      secondRow.length mustBe header.length

      firstRow  must include("| 872             | VERIFICATIONS")
      secondRow must include("| 14              | MONTHLY_RETURN")

      firstRow.count(_ == '|') mustBe header.count(_ == '|')
      secondRow.count(_ == '|') mustBe header.count(_ == '|')
    }

    "truncate values longer than the allowed column width" in {
      val generatedAt =
        LocalDateTime.of(2026, 8, 18, 15, 0, 0)

      val report =
        PollReportFormatter.format(
          Seq(
            PollReportContent(
              user = "1234567890123456",
              submissionType = "MONTHLY_RETURN_VALUE_TOO_LONG",
              submissionId = "123456789012345678901",
              govTalkRequestStatus = "GOVTALK_REQUEST_STATUS_TOO_LONG",
              currentReturnStatus = "CURRENT_RETURN_STATUS_TOO_LONG",
              employerReference = "123/REFERENCE_TOO_LONG",
              correlationId = "123456789012345678901234567890123",
              agentId = "AGENT-ID-TOO-LONG"
            )
          ),
          generatedAt
        )

      val lines =
        report.linesIterator.toSeq

      val header =
        lines(2)

      val row =
        lines(4)

      val expectedRow =
        "| " +
          Seq(
            "123456789012...".padTo(15, ' '),
            "MONTHLY_RETURN_...".padTo(18, ' '),
            "12345678901234567...".padTo(20, ' '),
            "GOVTALK_REQUEST_STATUS...".padTo(25, ' '),
            "CURRENT_RETURN_STATUS...".padTo(24, ' '),
            "123/REFERENCE...".padTo(16, ' '),
            "12345678901234567890123456789...".padTo(32, ' '),
            "AGENT...".padTo(8, ' ')
          ).mkString(" | ") +
          " |"

      row mustBe expectedRow
      row.length mustBe header.length
    }
  }
}
