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

import com.google.inject.Inject
import uk.gov.hmrc.constructionindustryscheme.models.response.GetGovTalkStatusResponse
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, ChrisSubmissionSessionRepository}

import java.time.Instant
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ChrisSubmissionSessionStore @Inject() (
  repo: ChrisSubmissionSessionRepository
)(using ec: ExecutionContext) {

  def get(submissionId: String): Future[Option[ChrisSubmissionSessionData]] =
    repo.get(submissionId)

  def delete(submissionId: String): Future[Boolean] =
    repo.delete(submissionId)

  def saveGovTalkStatus(submissionId: String, govTalkStatus: GetGovTalkStatusResponse): Future[Unit] =
    repo.get(submissionId).flatMap {
      case Some(existing) =>
        repo.upsert(
          existing.copy(
            govTalkStatus = Some(govTalkStatus)
          )
        )

      case None =>
        Future.failed(new RuntimeException(s"No existing session found for submissionId: $submissionId"))
    }

  def saveInitialAck(
    submissionId: String,
    instanceId: String,
    correlationId: String,
    pollInterval: Int,
    pollUrl: String,
    lastMessageDate: Instant = Instant.now
  ): Future[Unit] =
    repo.upsert(
      ChrisSubmissionSessionData(
        submissionId = submissionId,
        instanceId = instanceId,
        correlationId = correlationId,
        lastMessageDate = lastMessageDate,
        numPolls = 0,
        pollInterval = pollInterval,
        pollUrl = pollUrl,
        govTalkStatus = None
      )
    )

  def updateAfterPoll(
    submissionId: String,
    correlationId: String,
    lastMessageDate: Instant,
    pollInterval: Int,
    pollUrl: String
  ): Future[Unit] =
    repo.get(submissionId).flatMap {
      case Some(existing) =>
        repo.upsert(
          existing.copy(
            correlationId = correlationId,
            lastMessageDate = lastMessageDate,
            numPolls = existing.numPolls + 1,
            pollInterval = pollInterval,
            pollUrl = pollUrl
          )
        )

      case None =>
        Future.failed(new RuntimeException(s"No existing session found for submissionId: $submissionId"))
    }
}
