/*
 * Copyright 2025 HM Revenue & Customs
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
import org.apache.pekko.Done
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.eq as eqTo
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, EmailConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{BuiltSubmissionPayload, GovTalkError, GovTalkMeta, ResponseEndPoint, SUBMITTED, SubmissionResult, SubmissionStatus, SuccessEmailParams}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateAndTrackSubmissionRequest, NilMonthlyReturnOrgSuccessEmail, SendEmailRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.xml.Elem

final class SubmissionServiceSpec extends SpecBase {

  private def setup: Setup = new Setup {}

  "createAndTrackSubmission" - {

    "delegates to FormpProxyConnector and returns submissionId" in {
      val s = setup; import s._

      val req = CreateAndTrackSubmissionRequest("123", 2024, 4)

      when(formpProxyConnector.createAndTrackSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.successful("sub-123"))

      service.createAndTrackSubmission(req).futureValue mustBe "sub-123"
      verify(formpProxyConnector).createAndTrackSubmission(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = CreateAndTrackSubmissionRequest("123", 2024, 4)

      when(formpProxyConnector.createAndTrackSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createAndTrackSubmission(req).failed.futureValue.getMessage must include ("boom")
    }
  }

  "updateSubmission" - {

    "delegates to FormpProxyConnector and completes" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123", taxYear = 2024, taxMonth = 4,
        hmrcMarkGenerated = Some("Dj5TVJDyRYCn9zta5EdySeY4fyA="), submittableStatus = "ACCEPTED"
      )

      when(formpProxyConnector.updateSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.updateSubmission(req).futureValue
      verify(formpProxyConnector).updateSubmission(eqTo(req))(any[HeaderCarrier])
      verifyNoInteractions(chrisConnector)
    }

    "propagates failures from FormpProxyConnector" in {
      val s = setup; import s._

      val req = UpdateSubmissionRequest(
        instanceId = "123", taxYear = 2024, taxMonth = 4, submittableStatus = "REJECTED"
      )

      when(formpProxyConnector.updateSubmission(eqTo(req))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new IllegalStateException("nope")))

      service.updateSubmission(req).failed.futureValue.getMessage must include ("nope")
    }
  }

  "submitToChris" - {

    "passes envelope + correlationId to ChrisConnector and returns SubmissionResult" in {
      val s = setup; import s._

      val payload  = mkPayload(corrId = "ABCDEF1234567890ABCDEF1234567890")
      val expected = mkResult(SUBMITTED, corrId = payload.correlationId)

      when(chrisConnector.submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      service.submitToChris(payload).futureValue mustBe expected

      verify(chrisConnector).submitEnvelope(
        eqTo(payload.envelope), eqTo(payload.correlationId)
      )(any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }

    "propagates failures from ChrisConnector" in {
      val s = setup; import s._

      val payload = mkPayload()

      when(chrisConnector.submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("downstream boom")))

      service.submitToChris(payload).failed.futureValue.getMessage must include ("downstream boom")
    }


    "on SUBMITTED and successEmail present, sends success email and returns result" in {
      val s = setup; import s._

      val payload = mkPayload()
      val chrisRes = mkResult(SUBMITTED, corrId = payload.correlationId)

      when(chrisConnector.submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(chrisRes))

      val params = SuccessEmailParams(to = "test@test.com", monthYear = "2025-09")

      val expectedEmail: SendEmailRequest =
        NilMonthlyReturnOrgSuccessEmail("test@test.com", "September", "2025")

      when(emailConnector.send(eqTo(expectedEmail))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Done))

      val out = service.submitToChris(payload, Some(params)).futureValue
      out mustBe chrisRes

      verify(chrisConnector).submitEnvelope(eqTo(payload.envelope), eqTo(payload.correlationId))(any[HeaderCarrier])
      verify(emailConnector).send(eqTo(expectedEmail))(any[HeaderCarrier])
      verifyNoInteractions(formpProxyConnector)
    }
  }

  trait Setup {
    val chrisConnector: ChrisConnector = mock[ChrisConnector]
    val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
    val emailConnector: EmailConnector = mock[EmailConnector]

    val service = new SubmissionService(chrisConnector, formpProxyConnector, emailConnector)

    def mkPayload(
                   corrId: String = "CID-123",
                   envelope: Elem = <GovTalkMessage/>
                 ): BuiltSubmissionPayload =
      BuiltSubmissionPayload(envelope = envelope, correlationId = corrId, irMark = "IRMK")

    def mkResult(
                  status: SubmissionStatus,
                  corrId: String = "CID-123",
                  pollSecs: Int = 15,
                  ts: String = "2025-01-01T00:00:00Z",
                  err: Option[GovTalkError] = None
                ): SubmissionResult =
      SubmissionResult(
        status = status,
        rawXml = "<ack/>",
        meta   = GovTalkMeta(
          qualifier        = "response",
          function         = "submit",
          className        = "CIS300MR",
          correlationId    = corrId,
          gatewayTimestamp = ts,
          responseEndPoint = ResponseEndPoint("/poll", pollSecs),
          error            = err
        )
      )
  }

}

