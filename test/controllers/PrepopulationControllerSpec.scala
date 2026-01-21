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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.*
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, OK, PRECONDITION_FAILED}
import play.api.libs.json.Json
import play.api.test.Helpers.{JSON, contentAsJson, contentType, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.PrepopulationController
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, EmployerReference}
import uk.gov.hmrc.constructionindustryscheme.services.PrepopulationService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class PrepopulationControllerSpec extends SpecBase {

  private val taxOfficeNumber    = "163"
  private val taxOfficeReference = "AB0063"
  private val instanceId         = "CIS-123"

  private def newControllerAndMocks() = {
    val mockService = mock[PrepopulationService]
    val authAction  = noEnrolmentReferenceAuthAction
    val controller  = new PrepopulationController(authAction, mockService, cc)
    (controller, mockService)
  }

  "PrepopulationController" - {

    "prepopulateContractorKnownFacts" - {

      "must return 204 NoContent when service succeeds (happy path)" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorKnownFacts(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(Future.unit)

        val result =
          controller
            .prepopulateContractorKnownFacts(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe NO_CONTENT

        verify(mockService).prepopulateContractorKnownFacts(
          eqTo(instanceId),
          eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
        )(any[HeaderCarrier])
        verifyNoMoreInteractions(mockService)
      }

      "must return 412 PreconditionFailed when service fails with NOT_FOUND" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorKnownFacts(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(UpstreamErrorResponse("not found", NOT_FOUND)))

        val result =
          controller
            .prepopulateContractorKnownFacts(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe PRECONDITION_FAILED
        (contentAsJson(result) \ "message").as[String] mustBe "CIS taxpayer not found"
      }

      "must map other UpstreamErrorResponse to same status with message" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorKnownFacts(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(
          Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY))
        )

        val result =
          controller
            .prepopulateContractorKnownFacts(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "must return 500 InternalServerError with 'Unexpected error' on unknown exception" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorKnownFacts(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("something bad happened")))

        val result =
          controller
            .prepopulateContractorKnownFacts(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }

    "prepopulateContractorAndSubcontractors" - {

      "must return 204 NoContent when service succeeds (happy path)" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorAndSubcontractors(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(Future.unit)

        val result =
          controller
            .prepopulateContractorAndSubcontractors(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe NO_CONTENT

        verify(mockService).prepopulateContractorAndSubcontractors(
          eqTo(instanceId),
          eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
        )(any[HeaderCarrier])
        verifyNoMoreInteractions(mockService)
      }

      "must map UpstreamErrorResponse to same status with message" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorAndSubcontractors(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(
          Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY))
        )

        val result =
          controller
            .prepopulateContractorAndSubcontractors(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe BAD_GATEWAY
        (contentAsJson(result) \ "message").as[String] must include("boom from upstream")
      }

      "must return 500 InternalServerError with 'Unexpected error' on unknown exception" in {
        val (controller, mockService) = newControllerAndMocks()

        when(
          mockService.prepopulateContractorAndSubcontractors(
            eqTo(instanceId),
            eqTo(EmployerReference(taxOfficeNumber, taxOfficeReference))
          )(any[HeaderCarrier])
        ).thenReturn(Future.failed(new RuntimeException("something bad happened")))

        val result =
          controller
            .prepopulateContractorAndSubcontractors(taxOfficeNumber, taxOfficeReference, instanceId)(fakeRequest)

        status(result) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
      }
    }
  }

  "getContractorScheme" - {

    "must return 200 OK with ContractorScheme JSON when service returns Some(scheme)" in {
      val (controller, mockService) = newControllerAndMocks()

      val scheme = ContractorScheme(
        schemeId = 999,
        instanceId = instanceId,
        accountsOfficeReference = "111111111",
        taxOfficeNumber = "163",
        taxOfficeReference = "AB0063",
        utr = Some("1234567890"),
        name = Some("ABC Construction Ltd"),
        emailAddress = Some("test@test.com"),
        displayWelcomePage = Some("Y"),
        prePopCount = Some(5),
        prePopSuccessful = Some("Y"),
        version = Some(1)
      )

      when(mockService.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(scheme)))

      val result = controller.getContractorScheme(instanceId)(fakeRequest)

      status(result) mustBe OK
      contentType(result) mustBe Some(JSON)
      contentAsJson(result) mustBe Json.toJson(scheme)

      verify(mockService).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(mockService)
    }

    "must return 404 NotFound when service returns None" in {
      val (controller, mockService) = newControllerAndMocks()

      when(mockService.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result = controller.getContractorScheme(instanceId)(fakeRequest)

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some(JSON)
      (contentAsJson(result) \ "message").as[String] mustBe "Scheme not found"

      verify(mockService).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(mockService)
    }

    "must map UpstreamErrorResponse to same status with message" in {
      val (controller, mockService) = newControllerAndMocks()

      when(mockService.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("boom from upstream", BAD_GATEWAY)))

      val result = controller.getContractorScheme(instanceId)(fakeRequest)

      status(result) mustBe BAD_GATEWAY
      contentType(result) mustBe Some(JSON)
      (contentAsJson(result) \ "message").as[String] must include("boom from upstream")

      verify(mockService).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(mockService)
    }

    "must return 500 InternalServerError with 'Unexpected error' on unknown exception" in {
      val (controller, mockService) = newControllerAndMocks()

      when(mockService.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("something bad happened")))

      val result = controller.getContractorScheme(instanceId)(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentType(result) mustBe Some(JSON)
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"

      verify(mockService).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
      verifyNoMoreInteractions(mockService)
    }
  }
}
