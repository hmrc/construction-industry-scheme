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

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.PollReportContent
import uk.gov.hmrc.constructionindustryscheme.services.GeneratePollReportService

import java.time.LocalDateTime

class GeneratePollReportServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  "GeneratePollReportService generatePollReport" - {

    "must generate and log a report when report content is provided" in new Setup {

      service
        .generatePollReport(
          reportContent = Seq(reportContent),
          generatedAt = generatedAt
        )
        .futureValue mustBe ()
    }

    "must generate and log an empty report when no content is provided" in new Setup {

      service
        .generatePollReport(
          reportContent = Seq.empty,
          generatedAt = generatedAt
        )
        .futureValue mustBe ()
    }

    "must support the one-argument method used by BatchPollerService" in new Setup {

      service
        .generatePollReport(Seq(reportContent))
        .futureValue mustBe ()
    }

    "must complete successfully for recoverable error content" in new Setup {

      val recoverableErrorContent =
        reportContent.copy(
          recoverableError = true
        )

      service
        .generatePollReport(
          reportContent = Seq(recoverableErrorContent),
          generatedAt = generatedAt
        )
        .futureValue mustBe ()
    }

    "must complete successfully when content contains an oversized field" in new Setup {

      val oversizedContent =
        reportContent.copy(
          user = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        )

      service
        .generatePollReport(
          reportContent = Seq(oversizedContent),
          generatedAt = generatedAt
        )
        .futureValue mustBe ()
    }
  }

  private trait Setup {

    val service =
      new GeneratePollReportService()

    val generatedAt: LocalDateTime =
      LocalDateTime.of(
        2026, 5, 5, 14, 25, 38
      )

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
