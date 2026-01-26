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
import uk.gov.hmrc.constructionindustryscheme.models.response.{CreateNilMonthlyReturnResponse, UnsubmittedMonthlyReturnsResponse, UnsubmittedMonthlyReturnsRow}
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UnsubmittedMonthlyReturns, UserMonthlyReturns}
import uk.gov.hmrc.constructionindustryscheme.services.MonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDateTime
import scala.concurrent.Future

class MonthlyReturnServiceSpec extends SpecBase {

  var setup: Setup                = new Setup {}
  override def beforeEach(): Unit =
    setup = new Setup {}

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

    "calls single endpoint and returns status response" in {
      val s = setup; import s._

      val payload          = NilMonthlyReturnRequest("abc-123", 2024, 3, "Y", "Y")
      val expectedResponse = CreateNilMonthlyReturnResponse("STARTED")

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(returnsFixture))
      when(formpProxy.createNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(expectedResponse))

      val result = service.createNilMonthlyReturn(payload).futureValue

      result mustBe expectedResponse
      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verify(formpProxy).createNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "returns status response from existing monthly return when duplicate exist" in {
      val s = setup; import s._

      val payload        = NilMonthlyReturnRequest("abc-123", 2025, 1, "Y", "Y")
      val existingReturn = MonthlyReturn(
        monthlyReturnId = 66666L,
        taxYear = 2025,
        taxMonth = 1,
        nilReturnIndicator = Some("Y"),
        decEmpStatusConsidered = Some("Y"),
        decAllSubsVerified = Some("Y"),
        decInformationCorrect = Some("Y"),
        decNoMoreSubPayments = Some("Y"),
        decNilReturnNoPayments = Some("Y"),
        status = Some("STARTED"),
        lastUpdate = Some(java.time.LocalDateTime.now()),
        amendment = Some("N"),
        supersededBy = None
      )

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(UserMonthlyReturns(Seq(existingReturn))))

      val result = service.createNilMonthlyReturn(payload).futureValue

      result mustBe CreateNilMonthlyReturnResponse("STARTED")
      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verify(formpProxy, never).createNilMonthlyReturn(any[NilMonthlyReturnRequest])(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp proxy" in {
      val s = setup; import s._

      val payload = NilMonthlyReturnRequest("abc-123", 2024, 3, "Y", "Y")
      val boom    = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(returnsFixture))
      when(formpProxy.createNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.createNilMonthlyReturn(payload).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verify(formpProxy).createNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  "getUnsubmittedMonthlyReturns" - {

    "maps formp monthly returns into response rows" in {
      val s = setup; import s._

      val last = Some(LocalDateTime.parse("2025-01-01T00:00:00"))

      val unsubmitted = UnsubmittedMonthlyReturns(
        scheme = ContractorScheme(
          schemeId = 1,
          instanceId = cisInstanceId,
          accountsOfficeReference = "123PA00123456",
          taxOfficeNumber = "163",
          taxOfficeReference = "AB0063"
        ),
        monthlyReturn = Seq(
          MonthlyReturn(1L, 2025, 1, nilReturnIndicator = Some("Y"), status = Some("PENDING"), lastUpdate = last),
          MonthlyReturn(2L, 2025, 2, nilReturnIndicator = Some("N"), status = Some("REJECTED"), lastUpdate = None)
        )
      )

      when(formpProxy.getUnsubmittedMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(unsubmitted))

      val out = service.getUnsubmittedMonthlyReturns(cisInstanceId).futureValue

      out mustBe UnsubmittedMonthlyReturnsResponse(
        unsubmittedCisReturns = Seq(
          UnsubmittedMonthlyReturnsRow(2025, 1, "Nil", "Awaiting confirmation", last),
          UnsubmittedMonthlyReturnsRow(2025, 2, "Standard", "Failed", None)
        )
      )

      verify(formpProxy).getUnsubmittedMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp" in {
      val s = setup; import s._

      val boom = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.getUnsubmittedMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getUnsubmittedMonthlyReturns(cisInstanceId).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getUnsubmittedMonthlyReturns(eqTo(cisInstanceId))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  trait Setup {
    val datacacheProxy = mock[DatacacheProxyConnector]
    val formpProxy     = mock[FormpProxyConnector]
    val service        = new MonthlyReturnService(datacacheProxy, formpProxy)

    val employerRef    = EmployerReference("123", "AB456")
    val cisInstanceId  = "abc-123"
    val returnsFixture = UserMonthlyReturns(Seq(MonthlyReturn(66666L, 2025, 1)))
  }
}
