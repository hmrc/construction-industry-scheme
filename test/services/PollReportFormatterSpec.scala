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

    "generate an empty report using the required structure" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq.empty,
          generatedAt = generatedAt
        )

      result mustBe Seq(
        sectionSeparator,
        "BATCH POLLING RESULTS FOR 05-05-26 14:25:38",
        expectedHeader,
        underline,
        "",
        underline,
        sectionSeparator
      ).mkString(System.lineSeparator())
    }

    "generate a populated report with columns aligned to the headings" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq(reportContent),
          generatedAt = generatedAt
        )

      val lines =
        result.linesIterator.toSeq

      lines(2) mustBe expectedHeader
      lines(4) mustBe expectedRow
    }

    "truncate an oversized value to width minus three followed by ellipsis" in new Setup {

      val oversizedContent =
        reportContent.copy(
          user = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(oversizedContent),
          generatedAt = generatedAt
        )

      val reportRow =
        result.linesIterator
          .find(_.contains("CIS300MR"))
          .getOrElse(
            fail("Expected populated report row")
          )

      // USER width = 15: first 12 characters + "..."
      reportRow must include(
        "    ABCDEFGHIJKL... CIS300MR"
      )

      reportRow must not include
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    "not truncate a value equal to the configured width" in new Setup {

      val exactWidthContent =
        reportContent.copy(
          user = "123456789012345"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(exactWidthContent),
          generatedAt = generatedAt
        )

      result must include("123456789012345")
      result must not include "123456789012..."
    }

    "truncate every column according to its configured width" in new Setup {

      val oversizedContent =
        reportContent.copy(
          user = "1234567890123456",
          submissionType = "1234567890123456789",
          submissionId = "123456789012345678901",
          govTalkRequestStatus = "12345678901234567890123456",
          currentReturnStatus = "1234567890123456789012345",
          employerReference = "12345678901234567",
          correlationId = "123456789012345678901234567890123",
          agentId = "123456789"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(oversizedContent),
          generatedAt = generatedAt
        )

      val reportRow =
        result.linesIterator
          .drop(4)
          .next()

      reportRow must include("123456789012...")
      reportRow must include("123456789012345...")
      reportRow must include("12345678901234567...")
      reportRow must include("1234567890123456789012...")
      reportRow must include("123456789012345678901...")
      reportRow must include("1234567890123...")
      reportRow must include("12345678901234567890123456789...")
      reportRow must include("12345...")
    }

    "generate a row for each supplied report-content item" in new Setup {

      val secondRow =
        reportContent.copy(
          submissionId = "90003",
          submissionType = "CISVERIFY"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(
            reportContent,
            secondRow
          ),
          generatedAt = generatedAt
        )

      result must include("90002")
      result must include("90003")
      result must include("CIS300MR")
      result must include("CISVERIFY")
    }
  }

  private trait Setup {

    val generatedAt: LocalDateTime =
      LocalDateTime.of(
        2026, 5, 5, 14, 25, 38
      )

    val sectionSeparator: String =
      "=========================================================================================================================================================================="

    val underline: String =
      "    ----------------------------------------------------------------------------------------------------------------------------------------------------------------------"

    val expectedHeader: String =
      "    USER            SUBMISSION_TYPE    SUBMISSION_ID        GOVTALK_REQUEST_STATUS    CURRENT_RETURN_STATUS    EMP REFERENCE    CORRELATION ID                   AGENT ID"

    val expectedRow: String =
      "    ONLINE          CIS300MR           90002                SUBMITTED                 POLLING                  123/456789       correlation-id-001               A123456 "

    val reportContent: PollReportContent =
      PollReportContent(
        user = "ONLINE",
        submissionType = "CIS300MR",
        submissionId = "90002",
        govTalkRequestStatus = "SUBMITTED",
        currentReturnStatus = "POLLING",
        employerReference = "123/456789",
        correlationId = "correlation-id-001",
        agentId = "A123456"
      )
  }
}
