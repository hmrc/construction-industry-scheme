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
    with MockitoSugar{

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()

  case class Ctx() {
    val datacache = mock[DatacacheProxyConnector]            
    val formp     = mock[FormpProxyConnector]                
    val service   = new MonthlyReturnService(datacache, formp)

    val er         = EmployerReference("123", "AB456")
    val instanceId = "abc-123"

    val sampleWrapper: UserMonthlyReturns = UserMonthlyReturns(
      Seq(MonthlyReturn(66666L, 2025, 1))
    )
  }

  "MonthlyReturnService.retrieveMonthlyReturns" - {

    "calls rds-datacache then formp and returns the wrapper (happy path)" in {
      val c = Ctx()                                          // ✅ NEW: fresh mocks

      when(c.datacache.getInstanceId(eqTo(c.er))(any[HeaderCarrier]))
        .thenReturn(Future.successful(c.instanceId))
      when(c.formp.getMonthlyReturns(eqTo(c.instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(c.sampleWrapper))

      val out = c.service.retrieveMonthlyReturns(c.er).futureValue
      out mustBe c.sampleWrapper

      verify(c.datacache).getInstanceId(eqTo(c.er))(any[HeaderCarrier])
      verify(c.formp).getMonthlyReturns(eqTo(c.instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(c.datacache, c.formp)
    }

    "propagates rds-datacache failure and does NOT call formp" in {
      val c = Ctx()                                          
      val boom = UpstreamErrorResponse("SP1 failed", 500)

      when(c.datacache.getInstanceId(eqTo(c.er))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = c.service.retrieveMonthlyReturns(c.er).failed.futureValue
      ex mustBe boom

      verify(c.datacache).getInstanceId(eqTo(c.er))(any[HeaderCarrier])
      verifyNoInteractions(c.formp)                           
    }

    "propagates formp failure when rds-datacache succeeds" in {
      val c = Ctx()                                          
      val boom = UpstreamErrorResponse("SP2 failed", 500)

      when(c.datacache.getInstanceId(eqTo(c.er))(any[HeaderCarrier]))
        .thenReturn(Future.successful(c.instanceId))
      when(c.formp.getMonthlyReturns(eqTo(c.instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = c.service.retrieveMonthlyReturns(c.er).failed.futureValue
      ex mustBe boom

      verify(c.datacache).getInstanceId(eqTo(c.er))(any[HeaderCarrier])
      verify(c.formp).getMonthlyReturns(eqTo(c.instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(c.datacache, c.formp)
    }
  }
}
