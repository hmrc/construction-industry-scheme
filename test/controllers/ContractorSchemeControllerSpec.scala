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

package controllers

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND, NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, JSON, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.ContractorSchemeController
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, UpdateContractorSchemeParams}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ContractorValidationRequest, UpdateContractorSchemeVersionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.{ContractorValidationResponse, UpdateContractorSchemeVersionResponse}
import uk.gov.hmrc.constructionindustryscheme.services.{ContractorSchemeService, ContractorValidationService}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class ContractorSchemeControllerSpec extends SpecBase {

  private def controller(
    service: ContractorSchemeService,
    validationService: ContractorValidationService = mock[ContractorValidationService]
  ): ContractorSchemeController =
    new ContractorSchemeController(fakeAuthAction(), service, validationService, cc)

  "updateSchemeVersion" - {

    val url = "/scheme/version-update"

    val validRequest = UpdateContractorSchemeVersionRequest(
      currentVersion = 1,
      instanceId = "abc-123"
    )

    val response = UpdateContractorSchemeVersionResponse(newVersion = 2)

    "returns 200 OK with the new version when service succeeds" in {
      val service = mock[ContractorSchemeService]

      when(service.updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val result =
        controller(service).updateSchemeVersion()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("newVersion" -> 2)

      verify(service).updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val service = mock[ContractorSchemeService]

      val result =
        controller(service).updateSchemeVersion()(
          FakeRequest(POST, url)
            .withBody(Json.obj("instanceId" -> "abc-123"))
            .withHeaders(CONTENT_TYPE -> JSON)
        )

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(service)
    }

    "returns upstream status and message when service fails with UpstreamErrorResponse" in {
      val service = mock[ContractorSchemeService]

      when(service.updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)))

      val result =
        controller(service).updateSchemeVersion()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "formp failed"

      verify(service).updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 500 InternalServerError when service fails with an unexpected exception" in {
      val service = mock[ContractorSchemeService]

      when(service.updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller(service).updateSchemeVersion()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"

      verify(service).updateSchemeVersion(eqTo(validRequest))(any[HeaderCarrier])
    }
  }

  "validateContractorDetails" - {

    val url          = "/contractor-validation/validate"
    val validRequest = ContractorValidationRequest(instanceId = "abc-123")

    val scheme = ContractorScheme(
      schemeId = 1,
      instanceId = "abc-123",
      accountsOfficeReference = "123PX00123456",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456",
      utr = Some("2234567890"),
      name = Some("ACME Ltd"),
      emailAddress = Some("test@example.com")
    )

    val validationResponse = ContractorValidationResponse(
      utrValid = true,
      schemeNameValid = true,
      emailAddressValid = true,
      scheme = scheme
    )

    "returns 200 OK with validation response when service returns Some" in {
      val service           = mock[ContractorSchemeService]
      val validationService = mock[ContractorValidationService]

      when(validationService.validateContractorDetails(eqTo("abc-123"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(Some(validationResponse)))

      val result =
        controller(service, validationService).validateContractorDetails()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe OK
      (contentAsJson(result) \ "utrValid").as[Boolean] mustBe true

      verify(validationService).validateContractorDetails(eqTo("abc-123"))(any[HeaderCarrier])
    }

    "returns 404 when service returns None" in {
      val service           = mock[ContractorSchemeService]
      val validationService = mock[ContractorValidationService]

      when(validationService.validateContractorDetails(eqTo("abc-123"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      val result =
        controller(service, validationService).validateContractorDetails()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe NOT_FOUND
      (contentAsJson(result) \ "message").as[String] mustBe "Scheme not found"
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val service           = mock[ContractorSchemeService]
      val validationService = mock[ContractorValidationService]

      val result =
        controller(service, validationService).validateContractorDetails()(
          FakeRequest(POST, url)
            .withBody(Json.obj())
            .withHeaders(CONTENT_TYPE -> JSON)
        )

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(validationService)
    }

    "returns upstream status when service fails with UpstreamErrorResponse" in {
      val service           = mock[ContractorSchemeService]
      val validationService = mock[ContractorValidationService]

      when(validationService.validateContractorDetails(eqTo("abc-123"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)))

      val result =
        controller(service, validationService).validateContractorDetails()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "formp failed"
    }

    "returns 500 when service fails with an unexpected exception" in {
      val service           = mock[ContractorSchemeService]
      val validationService = mock[ContractorValidationService]

      when(validationService.validateContractorDetails(eqTo("abc-123"))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller(service, validationService).validateContractorDetails()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
    }
  }

  "updateScheme" - {

    val url = "/scheme/update"

    val validParams = UpdateContractorSchemeParams(
      schemeId = 1,
      instanceId = "abc-123",
      accountsOfficeReference = "123PX00123456",
      taxOfficeNumber = "123",
      taxOfficeReference = "AB456",
      utr = Some("2234567890"),
      name = Some("ACME Ltd"),
      emailAddress = Some("test@example.com")
    )

    "returns 204 NoContent when service succeeds" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validParams))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validParams)
        )

      status(result) mustBe NO_CONTENT

      verify(service).updateScheme(eqTo(validParams))(any[HeaderCarrier])
    }

    "returns 400 BadRequest when JSON is invalid" in {
      val service = mock[ContractorSchemeService]

      val result =
        controller(service).updateScheme()(
          FakeRequest(POST, url)
            .withBody(Json.obj("instanceId" -> "abc-123"))
            .withHeaders(CONTENT_TYPE -> JSON)
        )

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(service)
    }

    "returns upstream status when service fails with UpstreamErrorResponse" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validParams))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validParams)
        )

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "formp failed"
    }

    "returns 500 when service fails with an unexpected exception" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validParams))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validParams)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"
    }
  }
}
