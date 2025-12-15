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
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, ContractorScheme, CreateContractorSchemeParams, EmployerReference, UpdateContractorSchemeParams}
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