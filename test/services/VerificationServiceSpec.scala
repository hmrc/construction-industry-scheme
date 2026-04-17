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
import uk.gov.hmrc.constructionindustryscheme.models.response.GetNewestVerificationBatchResponse
import uk.gov.hmrc.constructionindustryscheme.services.VerificationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class VerificationServiceSpec extends SpecBase {

  "VerificationService#getNewestVerificationBatch" - {

    val instanceId = "abc-123"

    val response = GetNewestVerificationBatchResponse(
      subcontractors = Seq.empty,
      verificationBatch = Seq.empty,
      verifications = Seq.empty,
      submission = Seq.empty,
      monthlyReturn = Seq.empty
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
}
