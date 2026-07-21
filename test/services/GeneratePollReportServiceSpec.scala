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

import java.time.{Clock, LocalDateTime, ZoneId}

class GeneratePollReportServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures {

  "GeneratePollReportService.generatePollReport" - {

    "generate and log a populated report successfully" in new Setup {

      service
        .generatePollReport(
          reportContent = Seq(reportContent)
        )
        .futureValue mustBe ()
    }

    "generate and log an empty report successfully" in new Setup {

      service
        .generatePollReport(
          reportContent = Seq.empty
        )
        .futureValue mustBe ()
    }

    "generate and log recoverable-error report content successfully" in new Setup {

      val recoverableErrorContent =
        PollReportContent.forRecoverableError(
          user = reportContent.user,
          submissionType = reportContent.submissionType,
          submissionId = reportContent.submissionId,
          govTalkRequestStatus = reportContent.govTalkRequestStatus,
          employerReference = reportContent.employerReference,
          correlationId = reportContent.correlationId,
          agentId = reportContent.agentId
        )

      service
        .generatePollReport(
          reportContent = Seq(recoverableErrorContent)
        )
        .futureValue mustBe ()
    }
  }

  private trait Setup {

    val generatedAt: LocalDateTime =
      LocalDateTime.of(
        2026, 5, 5, 14, 25, 38
      )

    val zoneId: ZoneId =
      ZoneId.of("Europe/London")

    val clock: Clock =
      Clock.fixed(
        generatedAt.atZone(zoneId).toInstant,
        zoneId
      )

    val service =
      new GeneratePollReportService(clock)

    val reportContent =
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
