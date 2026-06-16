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

import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.services.GetSubmissionsToPollService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class GetSubmissionsToPollServiceSpec extends AnyFreeSpec with Matchers with ScalaFutures with MockitoSugar {

  "GetSubmissionsToPollService getSubmissionsToPoll" - {

    "must return submissions from FormpProxyConnector" in new Setup {
      when(mockFormpProxyConnector.getBatchPollSubmissions()(using hc))
        .thenReturn(Future.successful(response))

      service.getSubmissionsToPoll().futureValue mustBe response

      verify(mockFormpProxyConnector).getBatchPollSubmissions()(using hc)
      verifyNoMoreInteractions(mockFormpProxyConnector)
    }

    "must propagate connector failure" in new Setup {
      val exception = new RuntimeException("connector failed")

      when(mockFormpProxyConnector.getBatchPollSubmissions()(using hc))
        .thenReturn(Future.failed(exception))

      service.getSubmissionsToPoll().failed.futureValue mustBe exception

      verify(mockFormpProxyConnector).getBatchPollSubmissions()(using hc)
      verifyNoMoreInteractions(mockFormpProxyConnector)
    }
  }

  private trait Setup {
    given hc: HeaderCarrier = HeaderCarrier()

    val mockFormpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]

    val service = new GetSubmissionsToPollService(mockFormpProxyConnector)

    val response: GetBatchPollSubmissionsResponse =
      GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(
          VerificationSubmissionToPoll(
            submissionId = 90001L,
            submissionType = "CISVERIFY",
            agentId = Some("A123456"),
            taxOfficeNumber = "123",
            taxOfficeReference = "ABC123",
            instanceId = "instance-verification-001",
            status = "SUBMITTED",
            verificationBatchResourceRef = 70001L
          )
        ),
        monthlyReturnSubmissions = Seq(
          MonthlyReturnSubmissionToPoll(
            submissionId = 90002L,
            submissionType = "CIS300MR",
            status = "SUBMITTED",
            taxOfficeNumber = "123",
            taxOfficeReference = "456789",
            taxYear = "2025-26",
            taxMonth = "06",
            instanceId = "instance-monthly-return-001",
            agentId = Some("A123456")
          )
        )
      )
  }
}
