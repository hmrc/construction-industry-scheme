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

import java.time.Instant
import scala.concurrent.Future
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import uk.gov.hmrc.constructionindustryscheme.models.response.GetGovTalkStatusResponse
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, ChrisSubmissionSessionRepository}
import uk.gov.hmrc.constructionindustryscheme.services.ChrisSubmissionSessionStore

class ChrisSubmissionSessionStoreSpec extends SpecBase with ScalaFutures {

  var setup: Setup                = new Setup {}
  override def beforeEach(): Unit =
    setup = new Setup {}

  "get" - {
    "delegates to repository" in {
      val s = setup
      import s._

      when(repo.get(submissionId))
        .thenReturn(Future.successful(Some(sessionData)))

      val result = store.get(submissionId).futureValue

      result mustBe Some(sessionData)
    }
  }

  "delete" - {
    "delegates to repository" in {
      val s = setup
      import s._

      when(repo.delete(submissionId))
        .thenReturn(Future.successful(true))

      val result = store.delete(submissionId).futureValue

      result mustBe true
    }
  }

  "saveInitialAck" - {
    "creates and upserts new session data" in {
      val s = setup
      import s._

      when(repo.upsert(expectedInitialAckData))
        .thenReturn(Future.successful(()))

      val result =
        store
          .saveInitialAck(
            submissionId = submissionId,
            instanceId = instanceId,
            correlationId = correlationId,
            pollInterval = pollInterval,
            pollUrl = pollUrl,
            lastMessageDate = lastMessageDate
          )
          .futureValue

      result mustBe ()
    }
  }

  "saveGovTalkStatus" - {
    "updates existing session with govTalkStatus" in {
      val s = setup
      import s._

      val updated = sessionData.copy(govTalkStatus = Some(govTalkStatus))

      when(repo.get(submissionId))
        .thenReturn(Future.successful(Some(sessionData)))
      when(repo.upsert(updated))
        .thenReturn(Future.successful(()))

      val result = store.saveGovTalkStatus(submissionId, govTalkStatus).futureValue

      result mustBe ()
    }

    "fails when no existing session is found" in {
      val s = setup
      import s._

      when(repo.get(submissionId))
        .thenReturn(Future.successful(None))

      val ex = store.saveGovTalkStatus(submissionId, govTalkStatus).failed.futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe s"No existing session found for submissionId: $submissionId"
    }
  }

  "updateAfterPoll" - {
    "updates existing session and increments numPolls" in {
      val s = setup
      import s._

      val updatedCorrelationId   = "corr-999"
      val updatedLastMessageDate = Instant.parse("2025-01-02T00:00:00Z")
      val updatedPollInterval    = 20
      val updatedPollUrl         = "/poll/999"

      val updated = sessionData.copy(
        correlationId = updatedCorrelationId,
        lastMessageDate = updatedLastMessageDate,
        numPolls = sessionData.numPolls + 1,
        pollInterval = updatedPollInterval,
        pollUrl = updatedPollUrl
      )

      when(repo.get(submissionId))
        .thenReturn(Future.successful(Some(sessionData)))
      when(repo.upsert(updated))
        .thenReturn(Future.successful(()))

      val result =
        store
          .updateAfterPoll(
            submissionId = submissionId,
            correlationId = updatedCorrelationId,
            lastMessageDate = updatedLastMessageDate,
            pollInterval = updatedPollInterval,
            pollUrl = updatedPollUrl
          )
          .futureValue

      result mustBe ()
    }

    "fails when no existing session is found" in {
      val s = setup
      import s._

      when(repo.get(submissionId))
        .thenReturn(Future.successful(None))

      val ex =
        store
          .updateAfterPoll(
            submissionId = submissionId,
            correlationId = "corr-999",
            lastMessageDate = Instant.parse("2025-01-02T00:00:00Z"),
            pollInterval = 20,
            pollUrl = "/poll/999"
          )
          .failed
          .futureValue

      ex mustBe a[RuntimeException]
      ex.getMessage mustBe s"No existing session found for submissionId: $submissionId"
    }
  }

  trait Setup {
    val repo  = mock[ChrisSubmissionSessionRepository]
    val store = new ChrisSubmissionSessionStore(repo)

    val submissionId    = "sub-123"
    val instanceId      = "instance-123"
    val correlationId   = "corr-123"
    val lastMessageDate = Instant.parse("2025-01-01T00:00:00Z")
    val pollInterval    = 10
    val pollUrl         = "/poll/123"

    val govTalkStatus = mock[GetGovTalkStatusResponse]

    val sessionData = ChrisSubmissionSessionData(
      submissionId = submissionId,
      instanceId = instanceId,
      correlationId = correlationId,
      lastMessageDate = lastMessageDate,
      numPolls = 3,
      pollInterval = pollInterval,
      pollUrl = pollUrl,
      govTalkStatus = None
    )

    val expectedInitialAckData = ChrisSubmissionSessionData(
      submissionId = submissionId,
      instanceId = instanceId,
      correlationId = correlationId,
      lastMessageDate = lastMessageDate,
      numPolls = 0,
      pollInterval = pollInterval,
      pollUrl = pollUrl,
      govTalkStatus = None
    )
  }
}
