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
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.constructionindustryscheme.connectors.{DatacacheProxyConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class MonthlyReturnServiceSpec
  extends SpecBase {

  var setup: Setup = _
  override def beforeEach(): Unit = {
    setup = new Setup {}
  }

  "getCisTaxpayer" - {

    "returns cis taxpayer when datacache succeeds" in {
      val s = setup
      import s._

      val taxpayer = mkTaxpayer()
      when(datacacheProxy.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(taxpayer))

      val out = service.getCisTaxpayer(employerRef).futureValue
      out mustBe taxpayer

      verify(datacacheProxy).getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier])
      verifyNoInteractions(formpProxy)
    }

    "propagates failure from datacache" in {
      val s = setup
      import s._

      val boom = UpstreamErrorResponse("rds-datacache proxy error", 502)

      when(datacacheProxy.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getCisTaxpayer(employerRef).failed.futureValue
      ex mustBe boom

      verify(datacacheProxy).getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier])
      verifyNoInteractions(formpProxy)
    }
  }

  "getAllMonthlyReturnsByCisId" - {

    "returns wrapper when formp succeeds" in {
      val s = setup
      import s._

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(returnsFixture))

      val out = service.getAllMonthlyReturnsByCisId(cisInstanceId).futureValue
      out mustBe returnsFixture

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp" in {
      val s = setup
      import s._

      val boom = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getAllMonthlyReturnsByCisId(cisInstanceId).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  "createNilMonthlyReturn" - {

    "orchestrates calls in sequence and completes" in {
      val s = setup; import s._

      val payload = NilMonthlyReturnRequest("abc-123", 2024, 3, Some("option1"), Some("confirmed"))

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(returnsFixture))
      when(formpProxy.createMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(formpProxy.updateSchemeVersion(eqTo(payload.instanceId), any[Int])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))
      when(formpProxy.updateMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.createNilMonthlyReturn(payload).futureValue

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verify(formpProxy).createMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verify(formpProxy).updateSchemeVersion(eqTo(payload.instanceId), any[Int])(any[HeaderCarrier])
      verify(formpProxy).updateMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  trait Setup {
    val datacacheProxy = mock[DatacacheProxyConnector]
    val formpProxy = mock[FormpProxyConnector]
    val service = new MonthlyReturnService(datacacheProxy, formpProxy)

    val employerRef = EmployerReference("123", "AB456")
    val cisInstanceId = "abc-123"
    val returnsFixture = UserMonthlyReturns(Seq(MonthlyReturn(66666L, 2025, 1)))
  }
}
