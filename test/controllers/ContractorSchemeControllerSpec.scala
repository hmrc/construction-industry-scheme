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
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, JSON, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.ContractorSchemeController
import uk.gov.hmrc.constructionindustryscheme.models.requests.{UpdateContractorSchemeRequest, UpdateContractorSchemeVersionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.UpdateContractorSchemeVersionResponse
import uk.gov.hmrc.constructionindustryscheme.services.ContractorSchemeService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class ContractorSchemeControllerSpec extends SpecBase {

  private def controller(service: ContractorSchemeService): ContractorSchemeController =
    new ContractorSchemeController(fakeAuthAction(), service, cc)

  "updateScheme" - {

    val url = "/scheme/update"

    val validRequest = UpdateContractorSchemeRequest(
      schemeId = 123,
      instanceId = "abc-123",
      taxOfficeNumber = "163",
      taxOfficeReference = "AB0063",
      accountsOfficeReference = "123PA00123456",
      prePopCount = 1,
      prePopSuccessful = "Y",
      uniqueTaxReference = "1234567890",
      name = "ABC Construction Ltd",
      emailAddress = "test@example.com",
      version = 2
    )

    "returns 204 NoContent when service succeeds" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe NO_CONTENT

      verify(service).updateScheme(eqTo(validRequest))(any[HeaderCarrier])
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

    "returns upstream status and message when service fails with UpstreamErrorResponse" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("formp failed", BAD_GATEWAY, BAD_GATEWAY)))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "formp failed"

      verify(service).updateScheme(eqTo(validRequest))(any[HeaderCarrier])
    }

    "returns 500 InternalServerError when service fails with an unexpected exception" in {
      val service = mock[ContractorSchemeService]

      when(service.updateScheme(eqTo(validRequest))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        controller(service).updateScheme()(
          fakeRequest.withBody(validRequest)
        )

      status(result) mustBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "message").as[String] mustBe "Unexpected error"

      verify(service).updateScheme(eqTo(validRequest))(any[HeaderCarrier])
    }
  }

  "updateSchemeVersion" - {

    val url = "/scheme/version-update"

    val validRequest = UpdateContractorSchemeVersionRequest(
      currentVersion = 1,
      instanceId = "abc-123"
    )

    val validJson: JsValue =
      Json.obj(
        "currentVersion" -> 1,
        "instanceId"     -> "abc-123"
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
}
