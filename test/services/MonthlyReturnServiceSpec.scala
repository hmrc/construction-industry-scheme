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
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.*
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

  "updateNilMonthlyReturn" - {

    "delegates to formp connector and returns Unit" in {
      val s = setup; import s._

      val payload = NilMonthlyReturnRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )

      when(formpProxy.updateNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result = service.updateNilMonthlyReturn(payload).futureValue
      result mustBe ()

      verify(formpProxy).updateNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp connector" in {
      val s = setup; import s._

      val payload = NilMonthlyReturnRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        decInformationCorrect = "Y",
        decNilReturnNoPayments = "Y"
      )
      val boom    = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.updateNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.updateNilMonthlyReturn(payload).failed.futureValue
      ex mustBe boom

      verify(formpProxy).updateNilMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  "createMonthlyReturn" - {

    "delegates to formp connector and returns Unit" in {
      val s = setup
      import s._

      val payload = MonthlyReturnRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      when(formpProxy.createMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result = service.createMonthlyReturn(payload).futureValue
      result mustBe ()

      verify(formpProxy).createMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp connector" in {
      val s = setup
      import s._

      val payload = MonthlyReturnRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      val boom = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.createMonthlyReturn(eqTo(payload))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.createMonthlyReturn(payload).failed.futureValue
      ex mustBe boom

      verify(formpProxy).createMonthlyReturn(eqTo(payload))(any[HeaderCarrier])
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

  "getMonthlyReturnForEdit" - {

    "returns the response from formp" in {
      val s = setup
      import s._

      val request = GetMonthlyReturnForEditRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      val expected = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq.empty,
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(formpProxy.getMonthlyReturnForEdit(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.successful(expected))

      val out = service.getMonthlyReturnForEdit(request).futureValue
      out mustBe expected

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(request))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp" in {
      val s = setup
      import s._

      val request = GetMonthlyReturnForEditRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1
      )

      val boom = UpstreamErrorResponse("formp proxy failure", 500)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(request))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getMonthlyReturnForEdit(request).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(request))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  "syncMonthlyReturnItems" - {

    "computes create/delete diffs and calls formp sync endpoint" in {
      val s = setup
      import s._

      val req = SelectedSubcontractorsRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        selectedSubcontractorIds = Seq(1L, 2L, 3L)
      )

      val subs = Seq(
        mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L)),
        mkSubcontractor(subcontractorId = 2L, subbieResourceRef = Some(20L)),
        mkSubcontractor(subcontractorId = 3L, subbieResourceRef = Some(30L))
      )

      val items = Seq(
        mkMonthlyReturnItem(itemResourceReference = Some(10L)),
        mkMonthlyReturnItem(itemResourceReference = Some(99L)),
        mkMonthlyReturnItem(itemResourceReference = Some(99L)) // include duplicate to prove distinct handling
      )

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = subs,
        monthlyReturnItems = items,
        submission = Seq.empty
      )

      val editReq = GetMonthlyReturnForEditRequest(instanceId = cisInstanceId, taxYear = 2025, taxMonth = 1)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(editResponse))

      val expectedSyncReq = SyncMonthlyReturnItemsRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(20L, 30L),
        deleteResourceReferences = Seq(99L)
      )

      when(formpProxy.syncMonthlyReturnItems(eqTo(expectedSyncReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val out = service.syncMonthlyReturnItems(req).futureValue
      out mustBe ()

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier])
      verify(formpProxy).syncMonthlyReturnItems(eqTo(expectedSyncReq))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "fails with 400 when selectedSubcontractorIds contains an ID not present in edit.subcontractors" in {
      val s = setup
      import s._

      val req = SelectedSubcontractorsRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        selectedSubcontractorIds = Seq(999L)
      )

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(
          mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L))
        ),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val editReq = GetMonthlyReturnForEditRequest(instanceId = cisInstanceId, taxYear = 2025, taxMonth = 1)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(editResponse))

      val ex = service.syncMonthlyReturnItems(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
      ex.getMessage must include("Subcontractor IDs not found")

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier])
      verify(formpProxy, never()).syncMonthlyReturnItems(any[SyncMonthlyReturnItemsRequest])(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "propagates failure from formp sync endpoint" in {
      val s = setup
      import s._

      val req = SelectedSubcontractorsRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        selectedSubcontractorIds = Seq(1L)
      )

      val subs = Seq(
        mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L))
      )

      val items = Seq.empty

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = subs,
        monthlyReturnItems = items,
        submission = Seq.empty
      )

      val editReq = GetMonthlyReturnForEditRequest(instanceId = cisInstanceId, taxYear = 2025, taxMonth = 1)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(editResponse))

      val expectedSyncReq = SyncMonthlyReturnItemsRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        createResourceReferences = Seq(10L),
        deleteResourceReferences = Seq.empty
      )

      val boom = UpstreamErrorResponse("formp proxy failure", 502)

      when(formpProxy.syncMonthlyReturnItems(eqTo(expectedSyncReq))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.syncMonthlyReturnItems(req).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier])
      verify(formpProxy).syncMonthlyReturnItems(eqTo(expectedSyncReq))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }
  }

  "updateMonthlyReturnItem" - {

    "builds proxy request using resource ref + verification number and calls formp.updateMonthlyReturnItem" in {
      val s = setup
      import s._

      val req = UpdateMonthlyReturnItemRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 1L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      val subcontractor = mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L))
        .copy(verificationNumber = Some("V123456"))

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(subcontractor),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val editReq = GetMonthlyReturnForEditRequest(instanceId = cisInstanceId, taxYear = 2025, taxMonth = 1)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(editResponse))

      val expectedProxyReq = UpdateMonthlyReturnItemProxyRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        amendment = "N",
        itemResourceReference = 10L,
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240",
        subcontractorName = "Tyne Test Ltd",
        verificationNumber = "V123456"
      )

      when(formpProxy.updateMonthlyReturnItem(eqTo(expectedProxyReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      service.updateMonthlyReturnItem(req).futureValue mustBe ()

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier])
      verify(formpProxy).updateMonthlyReturnItem(eqTo(expectedProxyReq))(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "fail with 400 when subcontractorId is not found in edit response" in {
      val s = setup
      import s._

      val req = UpdateMonthlyReturnItemRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 999L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(
          mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L))
            .copy(verificationNumber = Some("V123456"))
        ),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      val editReq = GetMonthlyReturnForEditRequest(instanceId = cisInstanceId, taxYear = 2025, taxMonth = 1)

      when(formpProxy.getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier]))
        .thenReturn(Future.successful(editResponse))

      val ex = service.updateMonthlyReturnItem(req).failed.futureValue
      ex mustBe a[UpstreamErrorResponse]
      ex.asInstanceOf[UpstreamErrorResponse].statusCode mustBe 400
      ex.getMessage must include("Subcontractor ID not found")

      verify(formpProxy).getMonthlyReturnForEdit(eqTo(editReq))(any[HeaderCarrier])
      verify(formpProxy, never()).updateMonthlyReturnItem(any[UpdateMonthlyReturnItemProxyRequest])(any[HeaderCarrier])
      verifyNoInteractions(datacacheProxy)
    }

    "fail when subcontractor resource reference is missing" in {
      val s = setup
      import s._

      val req = UpdateMonthlyReturnItemRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 1L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      val subcontractor =
        mkSubcontractor(subcontractorId = 1L, subbieResourceRef = None)
          .copy(verificationNumber = Some("V123"))

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(subcontractor),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(formpProxy.getMonthlyReturnForEdit(any())(any()))
        .thenReturn(Future.successful(editResponse))

      service.updateMonthlyReturnItem(req).failed.futureValue

      verify(formpProxy, never()).updateMonthlyReturnItem(any())(any())
    }

    "fail when verification number is missing" in {
      val s = setup
      import s._

      val req = UpdateMonthlyReturnItemRequest(
        instanceId = cisInstanceId,
        taxYear = 2025,
        taxMonth = 1,
        subcontractorId = 1L,
        subcontractorName = "Tyne Test Ltd",
        totalPayments = "1200",
        costOfMaterials = "500",
        totalDeducted = "240"
      )

      val subcontractor =
        mkSubcontractor(subcontractorId = 1L, subbieResourceRef = Some(10L))
          .copy(verificationNumber = None)

      val editResponse = GetMonthlyReturnForEditResponse(
        scheme = Seq.empty,
        monthlyReturn = Seq.empty,
        subcontractors = Seq(subcontractor),
        monthlyReturnItems = Seq.empty,
        submission = Seq.empty
      )

      when(formpProxy.getMonthlyReturnForEdit(any())(any()))
        .thenReturn(Future.successful(editResponse))

      service.updateMonthlyReturnItem(req).failed.futureValue

      verify(formpProxy, never()).updateMonthlyReturnItem(any())(any())
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

  private def mkSubcontractor(subcontractorId: Long, subbieResourceRef: Option[Long]): Subcontractor =
    Subcontractor(
      subcontractorId = subcontractorId,
      utr = None,
      pageVisited = None,
      partnerUtr = None,
      crn = None,
      firstName = None,
      nino = None,
      secondName = None,
      surname = None,
      partnershipTradingName = None,
      tradingName = None,
      subcontractorType = None,
      addressLine1 = None,
      addressLine2 = None,
      addressLine3 = None,
      addressLine4 = None,
      country = None,
      postCode = None,
      emailAddress = None,
      phoneNumber = None,
      mobilePhoneNumber = None,
      worksReferenceNumber = None,
      createDate = None,
      lastUpdate = None,
      subbieResourceRef = subbieResourceRef,
      matched = None,
      autoVerified = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verificationDate = None,
      version = None,
      updatedTaxTreatment = None,
      lastMonthlyReturnDate = None,
      pendingVerifications = None
    )

  private def mkMonthlyReturnItem(itemResourceReference: Option[Long]): MonthlyReturnItem =
    MonthlyReturnItem(
      monthlyReturnId = 1L,
      monthlyReturnItemId = 1L,
      totalPayments = None,
      costOfMaterials = None,
      totalDeducted = None,
      unmatchedTaxRateIndicator = None,
      subcontractorId = None,
      subcontractorName = None,
      verificationNumber = None,
      itemResourceReference = itemResourceReference
    )
}
