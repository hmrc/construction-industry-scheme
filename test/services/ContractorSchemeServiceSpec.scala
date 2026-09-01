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
import uk.gov.hmrc.constructionindustryscheme.models.requests.{UpdateContractorSchemeRequest, UpdateContractorSchemeVersionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.UpdateContractorSchemeVersionResponse
import uk.gov.hmrc.constructionindustryscheme.services.ContractorSchemeService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContractorSchemeServiceSpec extends SpecBase {

  "updateScheme" - {

    val request = UpdateContractorSchemeRequest(
      schemeId = 123,
      instanceId = "abc-123",
      taxOfficeNumber = "163",
      taxOfficeReference = "AB0063",
      accountsOfficeReference = "123PA00123456",
      prePopCount = 1,
      prePopSuccessful = "Y",
      uniqueTaxReference = "1234567890",
      name = "ABC Construction Ltd",
      emailAddress = "test@example.com",
      version = 2
    )

    "delegates to FormpProxyConnector and returns Unit" in {
      val connector = mock[FormpProxyConnector]
      val service   = new ContractorSchemeService(connector)

      when(connector.updateContractorSchemeDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.updateScheme(request).futureValue mustBe ((): Unit)

      verify(connector).updateContractorSchemeDetails(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector = mock[FormpProxyConnector]
      val service   = new ContractorSchemeService(connector)

      when(connector.updateContractorSchemeDetails(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateScheme(request).failed.futureValue.getMessage must include("boom")
    }
  }

  "updateSchemeVersion" - {

    val request = UpdateContractorSchemeVersionRequest(
      currentVersion = 1,
      instanceId = "abc-123"
    )

    val response = UpdateContractorSchemeVersionResponse(newVersion = 2)

    "delegates to FormpProxyConnector and returns the response" in {
      val connector = mock[FormpProxyConnector]
      val service   = new ContractorSchemeService(connector)

      when(connector.updateContractorSchemeVersion(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      service.updateSchemeVersion(request).futureValue mustBe response

      verify(connector).updateContractorSchemeVersion(eqTo(request))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val connector = mock[FormpProxyConnector]
      val service   = new ContractorSchemeService(connector)

      when(connector.updateContractorSchemeVersion(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.updateSchemeVersion(request).failed.futureValue.getMessage must include("boom")
    }
  }
}
