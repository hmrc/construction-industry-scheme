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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.mockito.{ArgumentCaptor, ArgumentMatchers}
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.controllers.ChrisSubmissionController
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisSubmissionRequest
import uk.gov.hmrc.constructionindustryscheme.services.ChrisService
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisSubmissionResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ChrisSubmissionControllerSpec extends SpecBase {

  "ChrisSubmissionController.submitNilMonthlyReturn" - {

    "return 200 with success JSON when service returns ChrisSubmissionResponse (happy path)" in new SetupAuth {
      val validJson: JsValue = Json.obj(
        "utr" -> "1234567890",
        "aoReference" -> "123/AB456",
        "informationCorrect" -> "yes",
        "inactivity" -> "yes",
        "monthYear" -> "2025-09"
      )

      val expectedDto = ChrisSubmissionRequest(
        utr = "1234567890",
        aoReference = "123/AB456",
        informationCorrect = "yes",
        inactivity = "yes",
        monthYear = "2025-09"
      )

      when(mockChrisService.submitNilMonthlyReturn(any[ChrisSubmissionRequest], any())(any[HeaderCarrier]))
        .thenReturn(Future.successful(ChrisSubmissionResponse(200, "<Ack/>", "Pyy1LRJh053AE+nuyp0GJR7oESw=")))

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, "/cis/chris").withBody(validJson).withHeaders(CONTENT_TYPE -> "application/json")

      val result: Future[Result] = controller.submitNilMonthlyReturn(request)

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "success").as[Boolean] mustBe true
      (json \ "status").as[Int] mustBe 200
      (json \ "body").as[String] mustBe "<Ack/>"
      (json \ "irMark").as[String] mustBe "Pyy1LRJh053AE+nuyp0GJR7oESw="

      val dtoCaptor: ArgumentCaptor[ChrisSubmissionRequest] =
        ArgumentCaptor.forClass(classOf[ChrisSubmissionRequest])
      verify(mockChrisService).submitNilMonthlyReturn(dtoCaptor.capture(), any())(any[HeaderCarrier])
      dtoCaptor.getValue mustBe expectedDto
    }

    "return 400 with validation errors when JSON is invalid" in new SetupAuth {
      val invalidJson: JsValue = Json.obj(
        "utr" -> 123, // wrong format
        "monthYear" -> "2025-09"
      )

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, "/cis/chris").withBody(invalidJson).withHeaders(CONTENT_TYPE -> "application/json")

      val result: Future[Result] = controller.submitNilMonthlyReturn(request)

      status(result) mustBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "message").isDefined mustBe true
      // Optional: assert error keys exist
      json.toString must include("utr")
      json.toString must include("aoReference")

      verifyNoInteractions(mockChrisService)
    }

    "return 500 with message when service fails (e.g. RuntimeException)" in new SetupAuth {
      val validJson: JsValue = Json.obj(
        "utr" -> "1234567890",
        "aoReference" -> "123/AB456",
        "informationCorrect" -> "yes",
        "inactivity" -> "yes",
        "monthYear" -> "2025-09"
      )

      when(mockChrisService.submitNilMonthlyReturn(any[ChrisSubmissionRequest], any())(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, "/cis/chris").withBody(validJson).withHeaders(CONTENT_TYPE -> "application/json")

      val result: Future[Result] = controller.submitNilMonthlyReturn(request)

      status(result) mustBe INTERNAL_SERVER_ERROR
      val json = contentAsJson(result)
      (json \ "success").as[Boolean] mustBe false
      (json \ "message").as[String] must include("boom")
    }
  }

  private trait BaseSetup {
    val mockChrisService: ChrisService = mock[ChrisService]
    implicit val ec: ExecutionContext = cc.executionContext
    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  private trait SetupAuth extends BaseSetup {
    private val auth = fakeAuthAction(ton = "123", tor = "AB456")
    val controller = new ChrisSubmissionController(auth, mockChrisService, cc)
  }

}
