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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.connectors.{DatacacheProxyConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{EmployerReference, MonthlyReturn, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

class MonthlyReturnServiceSpec
  extends AnyFreeSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

  "getInstanceId" - {

    "returns instance id when datacache succeeds" in new Setup {
      when(datacacheProxy.getInstanceId(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cisInstanceId))

      val out = service.getInstanceId(employerRef).futureValue
      out mustBe cisInstanceId

      verify(datacacheProxy).getInstanceId(eqTo(employerRef))(any[HeaderCarrier])
      verifyNoInteractions(formpProxy)
    }

    "propagates failure from datacache" in new Setup {
      val boom = UpstreamErrorResponse("rds-datacache proxy error", 502)

      when(datacacheProxy.getInstanceId(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getInstanceId(employerRef).failed.futureValue
      ex mustBe boom

      verify(datacacheProxy).getInstanceId(eqTo(employerRef))(any[HeaderCarrier])
      verifyNoInteractions(formpProxy)
    }
  }

  "getAllMonthlyReturnsByCisId" - {

    "returns wrapper when formp succeeds" in new Setup {
      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(returnsFixture))

      val out = service.getAllMonthlyReturnsByCisId(cisInstanceId).futureValue
      out mustBe returnsFixture

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp" in new Setup {
      val boom = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getAllMonthlyReturnsByCisId(cisInstanceId).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
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
