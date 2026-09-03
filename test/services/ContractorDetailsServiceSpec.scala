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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.UpdateContractorSchemeParams
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateSchemeVersionRequest
import uk.gov.hmrc.constructionindustryscheme.services.ContractorDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContractorDetailsServiceSpec extends AnyWordSpec with Matchers with MockitoSugar with ScalaFutures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val updateContractorSchemeParams =
    UpdateContractorSchemeParams(
      schemeId = 123,
      instanceId = "cisId",
      accountsOfficeReference = "123 PA 87654321",
      taxOfficeNumber = "123",
      taxOfficeReference = "45678",
      utr = Some("1234567890"),
      name = Some("Scheme ABC"),
      emailAddress = Some("test@mail.com"),
      version = Some(7)
    )

  "ContractorDetailsService.submitContractorDetails" should {

    "update scheme version and then update contractor scheme with new version" in {

      val mockConnector =
        mock[FormpProxyConnector]

      when(
        mockConnector.updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(8))

      when(
        mockConnector.updateContractorScheme(
          eqTo(updateContractorSchemeParams.copy(version = Some(8)))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val service =
        new ContractorDetailsService(mockConnector)

      val result =
        service.submitContractorDetails(updateContractorSchemeParams).futureValue

      result mustBe (())

      verify(mockConnector)
        .updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])

      verify(mockConnector)
        .updateContractorScheme(
          eqTo(updateContractorSchemeParams.copy(version = Some(8)))
        )(any[HeaderCarrier])
    }

    "use version 0 when request version is missing" in {

      val mockConnector =
        mock[FormpProxyConnector]

      val requestWithoutVersion =
        updateContractorSchemeParams.copy(version = None)

      when(
        mockConnector.updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 0))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(1))

      when(
        mockConnector.updateContractorScheme(
          eqTo(requestWithoutVersion.copy(version = Some(1)))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val service =
        new ContractorDetailsService(mockConnector)

      val result =
        service.submitContractorDetails(requestWithoutVersion).futureValue

      result mustBe (())

      verify(mockConnector)
        .updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 0))
        )(any[HeaderCarrier])

      verify(mockConnector)
        .updateContractorScheme(
          eqTo(requestWithoutVersion.copy(version = Some(1)))
        )(any[HeaderCarrier])
    }

    "not update contractor scheme when updating scheme version fails" in {

      val mockConnector =
        mock[FormpProxyConnector]

      val exception =
        new RuntimeException("update scheme version failed")

      when(
        mockConnector.updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(exception))

      val service =
        new ContractorDetailsService(mockConnector)

      val result =
        service.submitContractorDetails(updateContractorSchemeParams).failed.futureValue

      result mustBe exception

      verify(mockConnector)
        .updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])

      verify(mockConnector, never())
        .updateContractorScheme(
          any[UpdateContractorSchemeParams]
        )(any[HeaderCarrier])
    }

    "fail when updating contractor scheme fails" in {

      val mockConnector =
        mock[FormpProxyConnector]

      val exception =
        new RuntimeException("update contractor scheme failed")

      when(
        mockConnector.updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(8))

      when(
        mockConnector.updateContractorScheme(
          eqTo(updateContractorSchemeParams.copy(version = Some(8)))
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(exception))

      val service =
        new ContractorDetailsService(mockConnector)

      val result =
        service.submitContractorDetails(updateContractorSchemeParams).failed.futureValue

      result mustBe exception

      verify(mockConnector)
        .updateSchemeVersion(
          eqTo(UpdateSchemeVersionRequest("cisId", 7))
        )(any[HeaderCarrier])

      verify(mockConnector)
        .updateContractorScheme(
          eqTo(updateContractorSchemeParams.copy(version = Some(8)))
        )(any[HeaderCarrier])
    }
  }
}
