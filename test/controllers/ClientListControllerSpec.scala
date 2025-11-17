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
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.controllers.ClientListController
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.{Failed, InProgress, InitiateDownload, Succeeded}
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.*
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ClientListControllerSpec extends SpecBase {

  "ClientListController.start" - {

    "return 200 OK with {\"result\":\"succeeded\"} when service.process completes successfully" in {
      val mockService = mock[ClientListService]

      when(mockService.process(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Succeeded))

      val controller =
        new ClientListController(fakeAuthAction(), mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("result" -> "succeeded")

      verify(mockService, times(1)).process(any[String])(any[HeaderCarrier])
    }

    "return 200 OK with {\"result\":\"in-progress\"} when service.process throws ClientListDownloadInProgressException" in {
      val mockService = mock[ClientListService]

      when(mockService.process(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InProgress))

      val controller =
        new ClientListController(fakeAuthAction(), mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("result" -> "in-progress")
    }

    "return 200 OK with {\"result\":\"failed\"} when service.process throws ClientListDownloadFailedException" in {
      val mockService = mock[ClientListService]

      when(mockService.process(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Failed))

      val controller =
        new ClientListController(fakeAuthAction(), mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("result" -> "failed")
    }

    "return 200 Ok with {\"result\":\"system-error\"} when service.process throws SystemException" in {
      val mockService = mock[ClientListService]

      when(mockService.process(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(InitiateDownload))

      val controller =
        new ClientListController(fakeAuthAction(), mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("result" -> "initiate-download")
    }

    "return 500 InternalServerError with {\"result\":\"system-error\"} when service.process fails with NoBusinessIntervalsException" in {
      val mockService = mock[ClientListService]

      when(mockService.process(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.failed(NoBusinessIntervalsException("no business intervals")))

      val controller =
        new ClientListController(fakeAuthAction(), mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("result" -> "system-error")

      verify(mockService, times(1)).process(any[String])(any[HeaderCarrier])
    }

    "return 400 BadRequest with \"Missing credentialId\" when no credentialId is available" in {
      val mockService = mock[ClientListService]

      val controller =
        new ClientListController(noEnrolmentReferenceAuthAction, mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj("message" -> "Missing credentialId")

      verify(mockService, never()).process(any[String])(any[HeaderCarrier])
    }
  }
}