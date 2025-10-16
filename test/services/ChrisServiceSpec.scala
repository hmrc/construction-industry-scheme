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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.ArgumentCaptor
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.constructionindustryscheme.connectors.ChrisConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.{AuditService, ChrisService}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.xml.Elem

class ChrisServiceSpec
  extends SpecBase {

    var setup: Setup = _

    override def beforeEach(): Unit = {
      setup = new Setup {}
    }

    "submitNilMonthlyReturn" - {

      "returns HttpResponse when connector returns 2xx" in {
        val s = setup
        import s._

        when(mockConnector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(200, "<Ack/>")))

        val out = service.submitNilMonthlyReturn(chrisRequest, authRequest).futureValue
        out.status mustBe 200
        out.body mustBe "<Ack/>"

        verify(mockConnector).submitEnvelope(any[Elem])(any[HeaderCarrier])
        verifyNoMoreInteractions(mockConnector)
      }

      "fails the future with RuntimeException when connector returns non-2xx (e.g. 400)" in {
        val s = setup
        import s._

        when(mockConnector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(400, "<FailureResponse/>")))

        val ex = service.submitNilMonthlyReturn(chrisRequest, authRequest).failed.futureValue
        ex mustBe a[RuntimeException]
        ex.getMessage must include("ChRIS submission failed status=400")
        ex.getMessage must include("FailureResponse")

        verify(mockConnector).submitEnvelope(any[Elem])(any[HeaderCarrier])
        verifyNoMoreInteractions(mockConnector)
      }

      "propagates a failure from the connector (e.g. UpstreamErrorResponse)" in {
        val s = setup
        import s._

        val boom = UpstreamErrorResponse("upstream failed", 502)
        when(mockConnector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
          .thenReturn(Future.failed(boom))

        val ex = service.submitNilMonthlyReturn(chrisRequest, authRequest).failed.futureValue
        ex mustBe boom

        verify(mockConnector).submitEnvelope(any[Elem])(any[HeaderCarrier])
        verifyNoMoreInteractions(mockConnector)
      }

      "fails with IllegalStateException when CIS enrolment is missing" in {
        val s = setup
        import s._
        val authReqWithoutEnrol = authRequestWith(Enrolments(Set.empty))

        val ex = intercept[IllegalStateException] {
          service.submitNilMonthlyReturn(chrisRequest, authReqWithoutEnrol).futureValue
        }

        ex.getMessage must include("Missing CIS enrolment identifiers")
        verifyNoInteractions(mockConnector)
      }

      "exclude inactivity from generated xml when inactivity is no in ChrisSubmissionRequest" in {
        val s = setup
        import s._

        when(mockConnector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
          .thenReturn(Future.successful(HttpResponse(200, "<Ack/>")))

        val requestInactivityNo = createChrisRequest().copy(inactivity = "no")
        service.submitNilMonthlyReturn(requestInactivityNo, authRequest).futureValue

        val captor: ArgumentCaptor[Elem] = ArgumentCaptor.forClass(classOf[Elem])
        verify(mockConnector).submitEnvelope(captor.capture())(any[HeaderCarrier])

        val sentXml: Elem = captor.getValue

        val inactivity = sentXml \\ "Declarations" \ "Inactivity"
        inactivity.isEmpty mustBe true
      }
    }

    trait Setup {
      val mockConnector: ChrisConnector = mock[ChrisConnector]
      val mockAuditService: AuditService = mock[AuditService]
      val service: ChrisService = new ChrisService(mockConnector, mockAuditService)

      val authRequest: AuthenticatedRequest[_] = createAuthReq()
      val chrisRequest: ChrisSubmissionRequest = createChrisRequest()

      def authRequestWith(enrolment: Enrolments): AuthenticatedRequest[_] = {
        val request = mock[AuthenticatedRequest[_]]
        when(request.enrolments).thenReturn(enrolment)
        request
      }
    }

  }
