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
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ProcessVerificationResponseFromChrisRequest, UpdateVerificationSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, StoredVerificationContext}
import uk.gov.hmrc.constructionindustryscheme.services.{VerificationFormPUpdateProcessor, VerificationResultMapper}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.{Instant, LocalDateTime}
import scala.concurrent.Future

class VerificationFormPUpdateProcessorSpec extends SpecBase {

  "VerificationFormPUpdateProcessor" - {

    "return Verification journey" in {
      val processor = new VerificationFormPUpdateProcessor(
        mock[FormpProxyConnector],
        mock[VerificationResultMapper]
      )

      processor.journey mustBe ChrisPollJourney.Verification
    }

    "update verification submission on initial accepted" in {
      val formpProxyConnector      = mock[FormpProxyConnector]
      val verificationResultMapper = mock[VerificationResultMapper]
      val processor                = new VerificationFormPUpdateProcessor(formpProxyConnector, verificationResultMapper)

      when(
        formpProxyConnector.updateVerificationSubmission(any[UpdateVerificationSubmissionRequest])(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      processor.handleInitialAccepted(sessionData(), submissionResult(ACCEPTED)).futureValue mustBe ()

      verify(formpProxyConnector).updateVerificationSubmission(any[UpdateVerificationSubmissionRequest])(
        any[HeaderCarrier]
      )
    }

    "process verification response from ChRIS on successful poll response" in {
      val formpProxyConnector      = mock[FormpProxyConnector]
      val verificationResultMapper = mock[VerificationResultMapper]
      val processor                = new VerificationFormPUpdateProcessor(formpProxyConnector, verificationResultMapper)

      val verifiedDate = LocalDateTime.parse("2026-06-19T10:02:00")

      val mappedResult = VerificationResult(
        resourceRef = 13L,
        matched = Some("Y"),
        verified = Some("N"),
        verificationNumber = "V1000000007",
        taxTreatment = "net",
        verifiedDate = verifiedDate
      )

      when(
        verificationResultMapper.mapAll(
          any[Seq[CisResponseSubcontractor]],
          any[StoredVerificationContext],
          any[LocalDateTime]
        )
      ).thenReturn(Future.successful(Seq(mappedResult)))

      when(
        formpProxyConnector.processVerificationResponseFromChris(any[ProcessVerificationResponseFromChrisRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.unit)

      processor
        .handlePollResponse(
          sessionData(),
          ChrisPollResponse(
            status = SUBMITTED,
            correlationId = "corr-123",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = Some("ir-mark"),
            lastMessageDate = None,
            acceptedTime = Some("2026-06-19T10:02:00"),
            cisResponseSubcontractors = Seq(
              CisResponseSubcontractor(
                utr = Some("1234567890"),
                partnershipUtr = None,
                tradingName = Some("Test Trading"),
                foreName = Some("John"),
                middleName = None,
                surname = Some("Smith"),
                nino = Some("AB123456C"),
                matched = Some("Y"),
                taxTreatment = Some("net"),
                verificationNumber = Some("V1000000007")
              )
            )
          )
        )
        .futureValue mustBe ()

      verify(verificationResultMapper).mapAll(
        any[Seq[CisResponseSubcontractor]],
        any[StoredVerificationContext],
        any[LocalDateTime]
      )

      verify(formpProxyConnector).processVerificationResponseFromChris(
        any[ProcessVerificationResponseFromChrisRequest]
      )(
        any[HeaderCarrier]
      )
    }

    "process verification response from ChRIS with expected request body" in {
      val formpProxyConnector      = mock[FormpProxyConnector]
      val verificationResultMapper = mock[VerificationResultMapper]
      val processor                = new VerificationFormPUpdateProcessor(formpProxyConnector, verificationResultMapper)

      val verifiedDate = LocalDateTime.parse("2026-06-19T10:02:00")

      val mappedResult = VerificationResult(
        resourceRef = 13L,
        matched = Some("Y"),
        verified = Some("N"),
        verificationNumber = "V1000000007",
        taxTreatment = "net",
        verifiedDate = verifiedDate
      )

      when(
        verificationResultMapper.mapAll(
          any[Seq[CisResponseSubcontractor]],
          any[StoredVerificationContext],
          any[LocalDateTime]
        )
      ).thenReturn(Future.successful(Seq(mappedResult)))

      when(
        formpProxyConnector.processVerificationResponseFromChris(any[ProcessVerificationResponseFromChrisRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.unit)

      processor
        .handlePollResponse(
          sessionData(),
          ChrisPollResponse(
            status = SUBMITTED,
            correlationId = "corr-123",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = Some("ir-mark"),
            lastMessageDate = None,
            acceptedTime = Some("2026-06-19T10:02:00"),
            cisResponseSubcontractors = Seq(
              CisResponseSubcontractor(
                utr = Some("1234567890"),
                partnershipUtr = None,
                tradingName = Some("Test Trading"),
                foreName = Some("John"),
                middleName = None,
                surname = Some("Smith"),
                nino = Some("AB123456C"),
                matched = Some("Y"),
                taxTreatment = Some("net"),
                verificationNumber = Some("V1000000007")
              )
            )
          )
        )
        .futureValue mustBe ()

      val requestCaptor =
        org.mockito.ArgumentCaptor.forClass(classOf[ProcessVerificationResponseFromChrisRequest])

      verify(formpProxyConnector).processVerificationResponseFromChris(requestCaptor.capture())(
        any[HeaderCarrier]
      )

      requestCaptor.getValue mustBe ProcessVerificationResponseFromChrisRequest(
        instanceId = "instance-123",
        verificationBatchResourceRef = 5L,
        acceptedTime = "2026-06-19T10:02:00",
        submissionStatus = SUBMITTED.toString,
        irMarkReceived = Some("ir-mark"),
        verificationResults = Seq(mappedResult)
      )
    }

    "process verification response from ChRIS when submitted with no receipt" in {
      val formpProxyConnector      = mock[FormpProxyConnector]
      val verificationResultMapper = mock[VerificationResultMapper]
      val processor                = new VerificationFormPUpdateProcessor(formpProxyConnector, verificationResultMapper)

      val verifiedDate = LocalDateTime.parse("2026-06-19T10:02:00")

      val mappedResult = VerificationResult(
        resourceRef = 13L,
        matched = Some("Y"),
        verified = Some("N"),
        verificationNumber = "V1000000007",
        taxTreatment = "net",
        verifiedDate = verifiedDate
      )

      when(
        verificationResultMapper.mapAll(
          any[Seq[CisResponseSubcontractor]],
          any[StoredVerificationContext],
          any[LocalDateTime]
        )
      ).thenReturn(Future.successful(Seq(mappedResult)))

      when(
        formpProxyConnector.processVerificationResponseFromChris(any[ProcessVerificationResponseFromChrisRequest])(
          any[HeaderCarrier]
        )
      ).thenReturn(Future.unit)

      processor
        .handlePollResponse(
          sessionData(),
          ChrisPollResponse(
            status = SUBMITTED_NO_RECEIPT,
            correlationId = "corr-123",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = None,
            lastMessageDate = None,
            acceptedTime = Some("2026-06-19T10:02:00"),
            cisResponseSubcontractors = Seq(
              CisResponseSubcontractor(
                utr = Some("1234567890"),
                partnershipUtr = None,
                tradingName = Some("Test Trading"),
                foreName = Some("John"),
                middleName = None,
                surname = Some("Smith"),
                nino = Some("AB123456C"),
                matched = Some("Y"),
                taxTreatment = Some("net"),
                verificationNumber = Some("V1000000007")
              )
            )
          )
        )
        .futureValue mustBe ()

      val requestCaptor =
        org.mockito.ArgumentCaptor.forClass(classOf[ProcessVerificationResponseFromChrisRequest])

      verify(formpProxyConnector).processVerificationResponseFromChris(requestCaptor.capture())(
        any[HeaderCarrier]
      )

      requestCaptor.getValue.irMarkReceived mustBe None
      requestCaptor.getValue.submissionStatus mustBe SUBMITTED_NO_RECEIPT.toString
    }

    "update verification submission on non-success poll response" in {
      val formpProxyConnector      = mock[FormpProxyConnector]
      val verificationResultMapper = mock[VerificationResultMapper]
      val processor                = new VerificationFormPUpdateProcessor(formpProxyConnector, verificationResultMapper)

      when(
        formpProxyConnector.updateVerificationSubmission(any[UpdateVerificationSubmissionRequest])(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      processor
        .handlePollResponse(
          sessionData(),
          ChrisPollResponse(
            status = DEPARTMENTAL_ERROR,
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

      verify(formpProxyConnector).updateVerificationSubmission(any[UpdateVerificationSubmissionRequest])(
        any[HeaderCarrier]
      )
    }

    "fail when verification context is missing" in {
      val processor = new VerificationFormPUpdateProcessor(
        mock[FormpProxyConnector],
        mock[VerificationResultMapper]
      )

      val ex = intercept[IllegalStateException] {
        processor.handleInitialAccepted(
          sessionData().copy(verificationContext = None),
          submissionResult(ACCEPTED)
        )
      }

      ex.getMessage mustBe "Verification context is missing for submissionId: sub-123"
    }
  }

  private def sessionData(): ChrisSubmissionSessionData =
    ChrisSubmissionSessionData(
      submissionId = "sub-123",
      instanceId = "instance-123",
      correlationId = "corr-123",
      lastMessageDate = Instant.parse("2026-06-19T10:00:00Z"),
      numPolls = 0,
      pollInterval = 10,
      pollUrl = "/poll/123",
      govTalkStatus = None,
      verificationContext = Some(
        StoredVerificationContext(
          verificationBatchResourceRef = 5L,
          hmrcMarkGenerated = "hmrc-mark",
          submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00"),
          actionIndicators = Seq.empty,
          requestedVerifications = Seq.empty
        )
      )
    )

  private def submissionResult(status: SubmissionStatus): SubmissionResult =
    SubmissionResult(
      status = status,
      rawXml = "<ack/>",
      meta = GovTalkMeta(
        qualifier = "response",
        function = "submit",
        className = "CISVERIFY",
        correlationId = "corr-123",
        gatewayTimestamp = None,
        responseEndPoint = ResponseEndPoint("/poll/123", 10),
        error = None
      )
    )
}
