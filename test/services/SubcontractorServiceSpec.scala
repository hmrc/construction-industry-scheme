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
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class SubcontractorServiceSpec extends SpecBase {

  val cisId = "1"

  "createAndUpdateSubcontractor" - {

    val soleTraderRequest: CreateAndUpdateSubcontractorRequest =
      CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
        cisId = cisId,
        tradingName = Some("trading Name"),
        firstName = Some("John"),
        surname = Some("Smith"),
        country = Some("United Kingdom")
      )

    val companyRequest: CreateAndUpdateSubcontractorRequest =
      CreateAndUpdateSubcontractorRequest.CompanyRequest(
        cisId = cisId,
        tradingName = Some("ACME Ltd"),
        crn = Some("CRN123"),
        country = Some("United Kingdom")
      )

    val partnershipRequest: CreateAndUpdateSubcontractorRequest =
      CreateAndUpdateSubcontractorRequest.PartnershipRequest(
        cisId = cisId,
        partnershipTradingName = Some("My Partnership"),
        partnerUtr = Some("9999999999"),
        tradingName = Some("Nominated Partner"),
        country = Some("United Kingdom")
      )

    "delegates to FormpProxyConnector and returns Unit (sole trader)" in {
      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(soleTraderRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(soleTraderRequest).futureValue mustBe ((): Unit)
      verify(formpProxyConnector).createAndUpdateSubcontractor(eqTo(soleTraderRequest))(any[HeaderCarrier])
    }

    "delegates to FormpProxyConnector and returns Unit (company)" in {
      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(companyRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(companyRequest).futureValue mustBe ((): Unit)
      verify(formpProxyConnector).createAndUpdateSubcontractor(eqTo(companyRequest))(any[HeaderCarrier])
    }

    "delegates to FormpProxyConnector and returns Unit (partnership)" in {
      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(partnershipRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createAndUpdateSubcontractor(partnershipRequest).futureValue mustBe ((): Unit)
      verify(formpProxyConnector).createAndUpdateSubcontractor(eqTo(partnershipRequest))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {
      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.createAndUpdateSubcontractor(eqTo(soleTraderRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.createAndUpdateSubcontractor(soleTraderRequest).failed.futureValue.getMessage must include("boom")
    }
  }

  "getSubcontractorUTRs" - {

    val cisId = "cis-123"

    "delegates to FormpProxyConnector and returns response" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      val subcontractorUTRs: Seq[String] = Seq("1111111111", "2222222222")

      when(formpProxyConnector.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subcontractorUTRs))

      service.getSubcontractorUTRs(cisId).futureValue mustBe subcontractorUTRs
      verify(formpProxyConnector).getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier])
    }

    "propagates failures from FormpProxyConnector" in {

      val formpProxyConnector: FormpProxyConnector = mock[FormpProxyConnector]
      val service                                  = new SubcontractorService(formpProxyConnector)

      when(formpProxyConnector.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      service.getSubcontractorUTRs(cisId).failed.futureValue.getMessage must include("boom")
    }
  }

}
