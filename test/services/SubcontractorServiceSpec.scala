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
import uk.gov.hmrc.constructionindustryscheme.models.{SoleTrader, SubcontractorType}
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class SubcontractorServiceSpec extends SpecBase {

  val cisId = "1"

  "updateSubcontractor" - {

    val request =
      CreateAndUpdateSubcontractorRequest(
        cisId = cisId,
        subcontractorType = SoleTrader,
        tradingName = Some("trading Name")
      )

    "delegates to FormpProxyConnector and returns response" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(request).futureValue mustBe ()
      verify(formpProxyConnector).createAndUpdateSubcontractor(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createAndUpdateSubcontractor(request).failed.futureValue.getMessage must include("boom")
    }
  }
}
