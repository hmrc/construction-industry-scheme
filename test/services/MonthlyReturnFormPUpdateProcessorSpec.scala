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

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, ChrisPollJourney, GovTalkMeta, ResponseEndPoint, SUBMITTED, SubmissionResult, SubmissionStatus}
import uk.gov.hmrc.constructionindustryscheme.repositories.ChrisSubmissionSessionData
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnFormPUpdateProcessor

import java.time.Instant

class MonthlyReturnFormPUpdateProcessorSpec extends SpecBase {

  "MonthlyReturnFormPUpdateProcessor" - {

    "return MonthlyReturn journey" in {
      val processor = new MonthlyReturnFormPUpdateProcessor()

      processor.journey mustBe ChrisPollJourney.MonthlyReturn
    }

    "handle initial accepted as no-op" in {
      val processor = new MonthlyReturnFormPUpdateProcessor()
      val session   = ChrisSubmissionSessionData(
        submissionId = "sub-123",
        instanceId = "instance-123",
        correlationId = "corr-123",
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = "/poll/123",
        govTalkStatus = None
      )

      processor.handleInitialAccepted(session, mkSubmissionResult(ACCEPTED)).futureValue mustBe ()
    }

    "handle poll response as no-op" in {
      val processor = new MonthlyReturnFormPUpdateProcessor()
      val session   = ChrisSubmissionSessionData(
        submissionId = "sub-123",
        instanceId = "instance-123",
        correlationId = "corr-123",
        lastMessageDate = Instant.parse("2025-01-01T00:00:00Z"),
        numPolls = 0,
        pollInterval = 10,
        pollUrl = "/poll/123",
        govTalkStatus = None
      )

      processor
        .handlePollResponse(
          session,
          ChrisPollResponse(
            status = SUBMITTED,
            correlationId = "corr-123",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = None,
            lastMessageDate = None,
            acceptedTime = None
          )
        )
        .futureValue mustBe ()
    }
  }

  private def mkSubmissionResult(status: SubmissionStatus): SubmissionResult =
    SubmissionResult(
      status = status,
      rawXml = "<ack/>",
      meta = GovTalkMeta(
        qualifier = "response",
        function = "submit",
        className = "CIS300MR",
        correlationId = "corr-123",
        gatewayTimestamp = None,
        responseEndPoint = ResponseEndPoint("/poll/123", 10),
        error = None
      )
    )
}
