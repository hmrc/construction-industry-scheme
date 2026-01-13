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
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import org.scalatest.freespec.AnyFreeSpec
import uk.gov.hmrc.constructionindustryscheme.connectors.{DatacacheProxyConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, Company, ContractorScheme, CreateContractorSchemeParams, EmployerReference, PrePopContractorBody, PrePopSubcontractor, PrepopKnownFacts, SoleTrader, UpdateContractorSchemeParams}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ApplyPrepopulationRequest, UpdateSchemeVersionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.{MonthlyReturnService, PrepopulationService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class PrepopulationServiceSpec extends SpecBase {

  "PrepopulationService.prepopulateContractorKnownFacts" - {

    "creates contractor scheme when none exists" in new Setup {
      val cis            = mkCis()
      val expectedAoRef  = "123AB456789"

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(formpProxy.createContractorScheme(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(1234))

      service.prepopulateContractorKnownFacts(instanceId, employerRef).futureValue

      val createCaptor = ArgumentCaptor.forClass(classOf[CreateContractorSchemeParams])
      verify(formpProxy).createContractorScheme(createCaptor.capture())(any[HeaderCarrier])

      val sent = createCaptor.getValue
      sent.instanceId mustBe instanceId
      sent.accountsOfficeReference mustBe expectedAoRef
      sent.taxOfficeNumber mustBe cis.taxOfficeNumber
      sent.taxOfficeReference mustBe cis.taxOfficeRef

      verify(formpProxy, never).updateContractorScheme(any())(any[HeaderCarrier])
    }

    "does nothing when existing scheme already matches CIS data" in new Setup {
      val cis         = mkCis()
      val aoRef       = "123AB456789"
      val existing    = mkExistingScheme(aoRef, cis.taxOfficeNumber, cis.taxOfficeRef)

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(existing)))

      service.prepopulateContractorKnownFacts(instanceId, employerRef).futureValue

      verify(formpProxy).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verify(formpProxy, never).createContractorScheme(any())(any[HeaderCarrier])
      verify(formpProxy, never).updateContractorScheme(any())(any[HeaderCarrier])
    }

    "updates existing scheme when AO ref or tax office details differ" in new Setup {
      val cis      = mkCis()
      val existing = mkExistingScheme(
        accountsOfficeReference = "OLDREF",
        taxOfficeNumber = "999",
        taxOfficeReference = "ZZ999"
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(existing)))
      when(formpProxy.updateContractorScheme(any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      service.prepopulateContractorKnownFacts(instanceId, employerRef).futureValue

      val updateCaptor = ArgumentCaptor.forClass(classOf[UpdateContractorSchemeParams])
      verify(formpProxy).updateContractorScheme(updateCaptor.capture())(any[HeaderCarrier])

      val sent = updateCaptor.getValue
      sent.schemeId mustBe existing.schemeId
      sent.instanceId mustBe instanceId
      sent.accountsOfficeReference mustBe "123AB456789"
      sent.taxOfficeNumber mustBe cis.taxOfficeNumber
      sent.taxOfficeReference mustBe cis.taxOfficeRef

      verify(formpProxy, never).createContractorScheme(any())(any[HeaderCarrier])
    }

    "propagates failure from MonthlyReturnService.getCisTaxpayer" in new Setup {
      val boom = UpstreamErrorResponse("datacache failure", 502)

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.prepopulateContractorKnownFacts(instanceId, employerRef).failed.futureValue
      ex mustBe boom

      verifyNoInteractions(formpProxy)
    }
  }

  "PrepopulationService.prepopulateContractorAndSubcontractors" - {

    "fails when no FORMP scheme exists for instanceId (F1 not run)" in new Setup {
      val cis = mkCis()

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))

      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val ex = service.prepopulateContractorAndSubcontractors(instanceId, employerRef).failed.futureValue
      ex mustBe a[IllegalStateException]
      ex.getMessage must include(s"No FORMP scheme found for instanceId=$instanceId")

      verify(formpProxy, never).applyPrepopulation(any())(any[HeaderCarrier])
      verify(formpProxy, never).updateContractorScheme(any())(any[HeaderCarrier])
      verify(formpProxy, never).updateSchemeVersion(any())(any[HeaderCarrier])
    }

    "when contractor prepop is None, updates scheme prepopCount + sets prePopSuccessful=N and bumps version" in new Setup {
      val cis = mkCis()
      val existing = mkExistingScheme("123AB456789", cis.taxOfficeNumber, cis.taxOfficeRef).copy(
        prePopCount = Some(1),
        version = Some(5),
        emailAddress = Some("test@example.com")
      )

      val expectedKnownFacts = PrepopKnownFacts(
        taxOfficeNumber = cis.taxOfficeNumber,
        taxOfficeReference = cis.taxOfficeRef,
        accountOfficeReference = "123AB456789"
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(existing)))

      when(datacacheProxy.getSchemePrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))
      when(datacacheProxy.getSubcontractorsPrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Seq.empty))

      when(formpProxy.updateContractorScheme(any())(any[HeaderCarrier]))
        .thenReturn(Future.unit)
      when(formpProxy.updateSchemeVersion(eqTo(UpdateSchemeVersionRequest(instanceId, 5)))(any[HeaderCarrier]))
        .thenReturn(Future.successful(6))

      service.prepopulateContractorAndSubcontractors(instanceId, employerRef).futureValue

      val updateCaptor = ArgumentCaptor.forClass(classOf[UpdateContractorSchemeParams])
      verify(formpProxy).updateContractorScheme(updateCaptor.capture())(any[HeaderCarrier])

      val sentUpdate = updateCaptor.getValue
      sentUpdate.schemeId mustBe existing.schemeId
      sentUpdate.instanceId mustBe instanceId
      sentUpdate.prePopCount mustBe Some(2)
      sentUpdate.prePopSuccessful mustBe Some("N")
      sentUpdate.emailAddress mustBe existing.emailAddress
      sentUpdate.version mustBe existing.version

      verify(formpProxy).updateSchemeVersion(eqTo(UpdateSchemeVersionRequest(instanceId, 5)))(any[HeaderCarrier])
      verify(formpProxy, never).applyPrepopulation(any())(any[HeaderCarrier])
    }

    "when contractor prepop exists, calls applyPrepopulation with mapped subcontractor types and returns Unit" in new Setup {
      val cis = mkCis()
      val existing = mkExistingScheme("123AB456789", cis.taxOfficeNumber, cis.taxOfficeRef).copy(
        prePopCount = Some(0),
        version = Some(7),
        emailAddress = Some("test@example.com"),
        displayWelcomePage = Some("Y")
      )

      val expectedKnownFacts = PrepopKnownFacts(
        taxOfficeNumber = cis.taxOfficeNumber,
        taxOfficeReference = cis.taxOfficeRef,
        accountOfficeReference = "123AB456789"
      )

      val contractorPrepop = PrePopContractorBody(
        schemeName = "ABC Construction Ltd",
        utr = "1234567890",
        response = 1
      )

      val subs = Seq(
        PrePopSubcontractor("S", "1111111111", "V1", "A", "Mr", "A", "B", "C"),
        PrePopSubcontractor("C", "2222222222", "V2", "B", "Ms", "D", "E", "F")
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(existing)))

      when(datacacheProxy.getSchemePrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(contractorPrepop)))
      when(datacacheProxy.getSubcontractorsPrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subs))

      when(formpProxy.applyPrepopulation(any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(8))

      service.prepopulateContractorAndSubcontractors(instanceId, employerRef).futureValue

      val reqCaptor = ArgumentCaptor.forClass(classOf[ApplyPrepopulationRequest])
      verify(formpProxy).applyPrepopulation(reqCaptor.capture())(any[HeaderCarrier])

      val sentReq = reqCaptor.getValue
      sentReq.schemeId mustBe existing.schemeId
      sentReq.instanceId mustBe instanceId
      sentReq.name mustBe "ABC Construction Ltd"
      sentReq.utr mustBe Some("1234567890")
      sentReq.prePopCount mustBe 1
      sentReq.prePopSuccessful mustBe "Y"
      sentReq.version mustBe 7
      sentReq.subcontractorTypes mustBe Seq(SoleTrader, Company)

      verify(formpProxy, never).updateContractorScheme(any())(any[HeaderCarrier])
      verify(formpProxy, never).updateSchemeVersion(any())(any[HeaderCarrier])
    }

    "throws IllegalArgumentException when subcontractor type is unknown" in new Setup {
      val cis = mkCis()
      val existing = mkExistingScheme("123AB456789", cis.taxOfficeNumber, cis.taxOfficeRef).copy(
        prePopCount = Some(0),
        version = Some(1)
      )

      val expectedKnownFacts = PrepopKnownFacts(
        taxOfficeNumber = cis.taxOfficeNumber,
        taxOfficeReference = cis.taxOfficeRef,
        accountOfficeReference = "123AB456789"
      )

      val contractorPrepop = PrePopContractorBody("ABC Construction Ltd", "1234567890", 1)
      val subs = Seq(
        PrePopSubcontractor("X", "1111111111", "V1", "A", "Mr", "A", "B", "C")
      )

      when(monthlyReturnService.getCisTaxpayer(eqTo(employerRef))(any[HeaderCarrier]))
        .thenReturn(Future.successful(cis))
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(existing)))

      when(datacacheProxy.getSchemePrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(contractorPrepop)))
      when(datacacheProxy.getSubcontractorsPrepopByKnownFacts(eqTo(expectedKnownFacts))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subs))

      val ex = service.prepopulateContractorAndSubcontractors(instanceId, employerRef).failed.futureValue
      ex mustBe a[IllegalArgumentException]
      ex.getMessage must include("Unknown TAXPAYER_TYPE")

      verify(formpProxy, never).applyPrepopulation(any())(any[HeaderCarrier])
    }
  }

  "PrepopulationService.getContractorScheme" - {

    "returns Some(ContractorScheme) when connector returns a scheme (happy path)" in new Setup {
      val scheme = mkExistingScheme(
        accountsOfficeReference = "123AB456789",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063"
      ).copy(
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd"),
        version = Some(1)
      )

      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(scheme)))

      val out = service.getContractorScheme(instanceId).futureValue
      out mustBe Some(scheme)

      verify(formpProxy).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(formpProxy)
    }

    "returns None when connector returns None" in new Setup {
      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val out = service.getContractorScheme(instanceId).futureValue
      out mustBe None

      verify(formpProxy).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(formpProxy)
    }

    "propagates failures from the connector" in new Setup {
      val boom = UpstreamErrorResponse("formp-proxy failed", 502)

      when(formpProxy.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(boom))

      val ex = service.getContractorScheme(instanceId).failed.futureValue
      ex mustBe boom

      verify(formpProxy).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(formpProxy)
    }
  }

  trait Setup {
    val monthlyReturnService: MonthlyReturnService = mock[MonthlyReturnService]
    val formpProxy: FormpProxyConnector = mock[FormpProxyConnector]
    val datacacheProxy: DatacacheProxyConnector = mock[DatacacheProxyConnector]

    val service = new PrepopulationService(monthlyReturnService, formpProxy, datacacheProxy)
    val employerRef = EmployerReference("163", "AB0063")
    val instanceId = "CIS-123"

    def mkCis(
               aoDistrict: Option[String] = Some("123"),
               aoPayType: Option[String] = Some("AB"),
               aoCheckCode: Option[String] = Some("45"),
               aoReference: Option[String] = Some("6789")
             ): CisTaxpayer =
      CisTaxpayer(
        uniqueId = instanceId,
        taxOfficeNumber = employerRef.taxOfficeNumber,
        taxOfficeRef = employerRef.taxOfficeReference,
        aoDistrict = aoDistrict,
        aoPayType = aoPayType,
        aoCheckCode = aoCheckCode,
        aoReference = aoReference,
        validBusinessAddr = None,
        correlation = None,
        ggAgentId = None,
        employerName1 = None,
        employerName2 = None,
        agentOwnRef = None,
        schemeName = Some("ABC Construction Ltd"),
        utr = Some("1234567890"),
        enrolledSig = None
      )

    def mkExistingScheme(
                          accountsOfficeReference: String,
                          taxOfficeNumber: String,
                          taxOfficeReference: String
                        ): ContractorScheme =
      ContractorScheme(
        schemeId = 999,
        instanceId = instanceId,
        accountsOfficeReference = accountsOfficeReference,
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference
      )
  }
}