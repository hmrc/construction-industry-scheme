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

  "PollReportFormatter format" - {

    "must generate an empty report using the required structure" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq.empty,
          generatedAt = generatedAt
        )

      result mustBe Seq(
        sectionSeparator,
        "BATCH POLLING RESULTS FOR 05-05-26 14:25:38",
        header,
        underline,
        "",
        underline,
        sectionSeparator
      ).mkString(System.lineSeparator())
    }

    "must populate the report using the supplied F9 content" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq(reportContent),
          generatedAt = generatedAt
        )

      result must include(
        "BATCH POLLING RESULTS FOR 05-05-26 14:25:38"
      )

      result must include("ONLINE")
      result must include("CIS300MR")
      result must include("90002")
      result must include("SUBMITTED")
      result must include("POLLING")
      result must include("123/456789")
      result must include("correlation-id-001")
      result must include("A123456")
    }

    "must set current return status to FATAL ERROR for a recoverable ChRIS error" in new Setup {

      val recoverableErrorContent =
        reportContent.copy(
          currentReturnStatus = "POLLING",
          recoverableError = true
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(recoverableErrorContent),
          generatedAt = generatedAt
        )

      val reportRow =
        result.linesIterator
          .find(_.contains("90002"))
          .getOrElse(
            fail("Expected report row for submission 90002")
          )

      reportRow must include("FATAL ERROR")
      reportRow must not include "POLLING"
    }

    "must truncate a field longer than its configured width" in new Setup {

      val longUserContent =
        reportContent.copy(
          user = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(longUserContent),
          generatedAt = generatedAt
        )

      /*
       * USER width is 15:
       * first 12 characters + "..."
       */
      result must include("ABCDEFGHIJKL...")
      result must not include "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    }

    "must not truncate a field equal to its configured width" in new Setup {

      val maximumWidthContent =
        reportContent.copy(
          user = "123456789012345"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(maximumWidthContent),
          generatedAt = generatedAt
        )

      result must include("123456789012345")
    }

    "must not truncate a field shorter than its configured width" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq(reportContent),
          generatedAt = generatedAt
        )

      result must include("ONLINE")
      result must not include "ONLINE..."
    }

    "must truncate each field using its configured width" in new Setup {

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
          .find(_.contains("123456789012..."))
          .getOrElse(
            fail("Expected formatted report row")
          )

      reportRow must include("123456789012...")
      reportRow must include("123456789012345...")
      reportRow must include("12345678901234567...")
      reportRow must include("1234567890123456789012...")
      reportRow must include("123456789012345678901...")
      reportRow must include("1234567890123...")
      reportRow must include("12345678901234567890123456789...")
      reportRow must include("12345...")
    }

    "must generate a row for each supplied submission" in new Setup {

      val secondReportContent =
        reportContent.copy(
          submissionType = "CISVERIFY",
          submissionId = "90003",
          correlationId = "correlation-id-002"
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(
            reportContent,
            secondReportContent
          ),
          generatedAt = generatedAt
        )

      result must include("90002")
      result must include("90003")
      result must include("CIS300MR")
      result must include("CISVERIFY")
      result must include("correlation-id-001")
      result must include("correlation-id-002")
    }

    "must handle a null field value without writing null into the report" in new Setup {

      val contentWithNullAgentId =
        reportContent.copy(
          agentId = null
        )

      val result =
        PollReportFormatter.format(
          reportContent = Seq(contentWithNullAgentId),
          generatedAt = generatedAt
        )

      result must not include "null"
    }

    "must retain the required separators and headings" in new Setup {

      val result =
        PollReportFormatter.format(
          reportContent = Seq(reportContent),
          generatedAt = generatedAt
        )

      val lines =
        result.linesIterator.toSeq

      lines.head mustBe sectionSeparator
      lines(1) mustBe "BATCH POLLING RESULTS FOR 05-05-26 14:25:38"
      lines(2) mustBe header
      lines(3) mustBe underline
      lines.last mustBe sectionSeparator
    }
  }

  private trait Setup {

    val generatedAt: LocalDateTime =
      LocalDateTime.of(
        2026, 5, 5, 14, 25, 38
      )

    val sectionSeparator: String =
      "=========================================================================================================================================================================="

    val header: String =
      "    USER            SUBMISSION_TYPE    SUBMISSION_ID        GOVTALK_REQUEST_STATUS    CURRENT_RETURN_STATUS    EMP REFERENCE    CORRELATION ID                   AGENT ID"

    val underline: String =
      "    ----------------------------------------------------------------------------------------------------------------------------------------------------------------------"

    val reportContent: PollReportContent =
      PollReportContent(
        user = "ONLINE",
        submissionType = "CIS300MR",
        submissionId = "90002",
        govTalkRequestStatus = "SUBMITTED",
        currentReturnStatus = "POLLING",
        employerReference = "123/456789",
        correlationId = "correlation-id-001",
        agentId = "A123456",
        recoverableError = false
      )
  }
}
