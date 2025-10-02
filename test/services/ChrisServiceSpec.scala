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
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.constructionindustryscheme.connectors.ChrisConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.ChrisService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, HttpResponse}

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
      val s = setup;
      import s._

      when(connector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(200, "<Ack/>")))

      val out = service.submitNilMonthlyReturn(chrisRequest, authRequest).futureValue
      out.status mustBe 200
      out.body mustBe "<Ack/>"

      verify(connector).submitEnvelope(any[Elem])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "fails the future with RuntimeException when connector returns non-2xx (e.g. 400)" in {
      val s = setup;
      import s._

      when(connector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
        .thenReturn(Future.successful(HttpResponse(400, "bad request")))

      val ex = service.submitNilMonthlyReturn(chrisRequest, authRequest).failed.futureValue
      ex mustBe a[RuntimeException]
      ex.getMessage must include("ChRIS submission failed status=400")
      ex.getMessage must include("bad request")

      verify(connector).submitEnvelope(any[Elem])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }

    "propagates a failure from the connector (e.g. UpstreamErrorResponse)" in {
      val s = setup;
      import s._

      val boom = UpstreamErrorResponse("upstream failed", 502)
      when(connector.submitEnvelope(any[Elem])(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.submitNilMonthlyReturn(chrisRequest, authRequest).failed.futureValue
      ex mustBe boom

      verify(connector).submitEnvelope(any[Elem])(any[HeaderCarrier])
      verifyNoMoreInteractions(connector)
    }
  }

  trait Setup {
    val connector: ChrisConnector = mock[ChrisConnector]
    val service: ChrisService = new ChrisService(connector)

    val authRequest: AuthenticatedRequest[_] = createAuthReq()
    val chrisRequest: ChrisSubmissionRequest = createChrisRequest()
  }

  }
