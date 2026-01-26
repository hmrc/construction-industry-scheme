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
import uk.gov.hmrc.constructionindustryscheme.models.SoleTrader
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubcontractorRequest, UpdateSubcontractorRequest}
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class SubcontractorServiceSpec extends SpecBase {

  val schemeId          = 1
  val subbieResourceRef = 10

  "createSubcontractor" - {

    "delegates to FormpProxyConnector and returns response" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      val request = CreateSubcontractorRequest(schemeId, SoleTrader, 0)

      when(formpProxyConnector.createSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subbieResourceRef))

      service.createSubcontractor(request).futureValue mustBe subbieResourceRef
      verify(formpProxyConnector).createSubcontractor(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      val request = CreateSubcontractorRequest(schemeId, SoleTrader, 0)

      when(formpProxyConnector.createSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createSubcontractor(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "updateSubcontractor" - {

    val request =
      UpdateSubcontractorRequest(schemeId = schemeId, subbieResourceRef = 10, tradingName = Some("trading Name"))

    "delegates to FormpProxyConnector and returns response" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.updateSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.updateSubcontractor(request).futureValue mustBe ()
      verify(formpProxyConnector).updateSubcontractor(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.updateSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateSubcontractor(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "getSubcontractorUTRs" - {

    val cisId = "cis-123"

    "delegates to FormpProxyConnector and returns response" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service = new SubcontractorService(formpProxyConnector)

      val subcontractorUTRs: Seq[String] = Seq("1111111111", "2222222222")

      when(formpProxyConnector.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subcontractorUTRs))

      service.getSubcontractorUTRs(cisId).futureValue mustBe subcontractorUTRs
      verify(formpProxyConnector).getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.getSubcontractorUTRs(cisId).failed.futureValue.getMessage must include("boom")
    }
  }

}
