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

package services.chris

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.services.chris.PollReportStatusMapper

final class PollReportStatusMapperSpec extends AnyFreeSpec with Matchers {

  private def pollResponse(
                            status: SubmissionStatus,
                            govTalkErrorStatus: Option[GovTalkErrorStatus] = None
                          ): ChrisPollResponse =
    ChrisPollResponse(
      status = status,
      correlationId = "corr-123",
      pollUrl = None,
      pollInterval = None,
      error = None,
      irMarkReceived = None,
      lastMessageDate = None,
      acceptedTime = None,
      govTalkErrorStatus = govTalkErrorStatus
    )

  "submissionTableStatus" - {

    "must return the poll status unchanged for every outcome" in {
      val statuses = Seq(ACCEPTED, SUBMITTED, SUBMITTED_NO_RECEIPT, DEPARTMENTAL_ERROR, STARTED, FATAL_ERROR)

      statuses.foreach { status =>
        PollReportStatusMapper.submissionTableStatus(pollResponse(status)) mustBe status.toString
      }
    }
  }

  "reportStatus" - {

    "must mirror the submission table status for non-recoverable outcomes" in {
      val statuses = Seq(ACCEPTED, SUBMITTED, SUBMITTED_NO_RECEIPT, DEPARTMENTAL_ERROR, FATAL_ERROR)

      statuses.foreach { status =>
        PollReportStatusMapper.reportStatus(pollResponse(status)) mustBe status.toString
      }
    }

    "must return DEPARTMENTAL_ERROR when the response is a departmental error" in {
      val response =
        pollResponse(
          status = DEPARTMENTAL_ERROR,
          govTalkErrorStatus = Some(GovTalkErrorStatus.DepartmentalError("Departmental error"))
        )

      PollReportStatusMapper.reportStatus(response) mustBe "DEPARTMENTAL_ERROR"
    }

    "must return FATAL_ERROR when the response is a recoverable error" in {
      Seq("3000", "2005", "1000").foreach { errorCode =>
        val response =
          pollResponse(
            status = STARTED,
            govTalkErrorStatus = Some(GovTalkErrorStatus.RecoverableError(errorCode, "Recoverable ChRIS error"))
          )

        PollReportStatusMapper.reportStatus(response) mustBe "FATAL_ERROR"
        PollReportStatusMapper.submissionTableStatus(response) mustBe "STARTED"
      }
    }

    "must return FATAL_ERROR when the status is STARTED but the GovTalk error status is missing" in {
      PollReportStatusMapper.reportStatus(pollResponse(STARTED)) mustBe "FATAL_ERROR"
    }

    "must return FATAL_ERROR when a recoverable error code arrives on a FATAL_ERROR status" in {
      val response =
        pollResponse(
          status = FATAL_ERROR,
          govTalkErrorStatus = Some(GovTalkErrorStatus.RecoverableError("3000", "Recoverable ChRIS error"))
        )

      PollReportStatusMapper.reportStatus(response) mustBe "FATAL_ERROR"
    }
  }
}