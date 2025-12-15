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
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, PRECONDITION_FAILED}
import play.api.test.Helpers.{contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.PrepopulationController
import uk.gov.hmrc.constructionindustryscheme.models.EmployerReference
import uk.gov.hmrc.constructionindustryscheme.services.PrepopulationService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

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
}
