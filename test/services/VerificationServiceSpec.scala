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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.{CreateVerifications, DeleteVerifications}
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.services.VerificationService
import uk.gov.hmrc.http.HeaderCarrier
import java.time.LocalDateTime

import scala.concurrent.Future

final class VerificationServiceSpec extends SpecBase {

  "VerificationService#getNewestVerificationBatch" - {

    val instanceId = "abc-123"

    val response = GetNewestVerificationBatchResponse(
      scheme = None,
      subcontractors = Seq.empty,
      verificationBatch = None,
      verifications = Seq.empty,
      submission = None,
      monthlyReturn = None,
      monthlyReturnSubmission = None
    )

    "delegates to FormpProxyConnector and returns response" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.getNewestVerificationBatch(instanceId).futureValue mustBe response

      verify(connector).getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.getNewestVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.getNewestVerificationBatch(instanceId).failed.futureValue.getMessage must include("boom")
    }
  }

  "VerificationService#getCurrentVerificationBatch" - {

    val instanceId = "abc-123"

    val response = GetCurrentVerificationBatchResponse(
      subcontractors = Seq.empty,
      verificationBatch = None,
      verifications = Seq.empty
    )

    "delegates to FormpProxyConnector and returns response" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.getCurrentVerificationBatch(instanceId).futureValue mustBe response

      verify(connector).getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.getCurrentVerificationBatch(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.getCurrentVerificationBatch(instanceId).failed.futureValue.getMessage must include("boom")
    }
  }

  "VerificationService#createVerificationBatchAndVerifications" - {

    val request = CreateVerificationBatchAndVerificationsRequest(
      instanceId = "abc-123",
      verificationResourceReferences = Seq(1L, 2L),
      actionIndicator = Some("A")
    )

    val response = CreateVerificationBatchAndVerificationsResponse(
      verificationBatchResourceReference = 10L
    )

    "delegates to FormpProxyConnector and returns response" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.createVerificationBatchAndVerifications(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.createVerificationBatchAndVerifications(request).futureValue mustBe response

      verify(connector).createVerificationBatchAndVerifications(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.createVerificationBatchAndVerifications(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createVerificationBatchAndVerifications(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "VerificationService#modifyVerifications" - {

    val request = ModifyVerificationsRequest(
      instanceId = "abc-123",
      deleteVerifications = Some(
        DeleteVerifications(
          verificationResourceReferences = Seq(111L, 222L)
        )
      ),
      createVerifications = Some(
        CreateVerifications(
          verificationBatchResourceRef = 10L,
          verificationResourceReferences = Seq(333L, 444L)
        )
      )
    )

    "delegates to FormpProxyConnector and returns response" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.modifyVerifications(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.modifyVerifications(request).futureValue mustBe ()

      verify(connector).modifyVerifications(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.modifyVerifications(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.modifyVerifications(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "VerificationService#createSubmissionForVerification" - {

    val request = CreateSubmissionAndUpdateVerificationsRequest(
      instanceId = "abc-123",
      verificationBatchId = 99L,
      verificationBatchResourceRef = 10L,
      emailRecipient = "ops@example.com",
      irMarkGenerated = Some("IR_MARK"),
      verifications = Seq(
        VerificationToUpdate("ACME", 111L, "Y"),
        VerificationToUpdate("BETA", 222L, "N")
      ),
      agentId = None
    )

    val response = CreateSubmissionAndUpdateVerificationsResponse(submissionId = 555L)

    "delegates to FormpProxyConnector and returns response" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.createSubmissionForVerification(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.createSubmissionAndUpdateVerifications(request).futureValue mustBe response

      verify(connector).createSubmissionForVerification(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.createSubmissionForVerification(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createSubmissionAndUpdateVerifications(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "VerificationService#processVerificationResponseFromChris" - {

    val request = ProcessVerificationResponseFromChrisRequest(
      instanceId = "abc-123",
      verificationBatchResourceRef = 77L,
      acceptedTime = "2026-06-15T10:05:00Z",
      submissionStatus = "ACCEPTED",
      irMarkReceived = "IR_MARK_RECEIVED",
      verificationResults = Seq(
        VerificationResult(
          resourceRef = 111L,
          matched = Some("Y"),
          verified = Some("Y"),
          verificationNumber = "V123456",
          taxTreatment = "NET",
          verifiedDate = LocalDateTime.of(2026, 6, 15, 10, 5, 0)
        ),
        VerificationResult(
          resourceRef = 222L,
          matched = Some("N"),
          verified = Some("N"),
          verificationNumber = "V654321",
          taxTreatment = "GROSS",
          verifiedDate = LocalDateTime.of(2026, 6, 15, 10, 6, 0)
        )
      )
    )

    "delegates to FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.processVerificationResponseFromChris(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.processVerificationResponseFromChris(request).futureValue mustBe ()

      verify(connector).processVerificationResponseFromChris(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                        = new VerificationService(connector)

      when(connector.processVerificationResponseFromChris(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.processVerificationResponseFromChris(request).failed.futureValue.getMessage must include("boom")
    }
  }
}
