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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, verifyNoInteractions, when}
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, JSON, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.SubcontractorController
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.CreateSubcontractorResponse
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class SubcontractorControllerSpec extends SpecBase with EitherValues {
  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  private def mockController(
                              subcontractorService: SubcontractorService,
                            auth: AuthAction = fakeAuthAction(),
                          ): SubcontractorController =
    new SubcontractorController(auth, subcontractorService, cc)

  val createSubcontractorUrl = "/cis/subcontractor/create"

  val validCreateJson: JsValue = Json.toJson(
    CreateSubcontractorRequest(
      schemeId = 1,
      subcontractorType = "trader",
      currentVersion = 0
    )
  )

  "createSubcontractor" - {
    "returns 201 with subcontractor response when service returns data" in {
      val service = mock[SubcontractorService]
      val controller = mockController(service)

      val response = CreateSubcontractorResponse(subbieResourceRef = 10)
      val responseJson: JsValue = Json.toJson(response)

      when(service.createSubcontractor(any[CreateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req = FakeRequest(POST, createSubcontractorUrl)
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubcontractor()(req)

      status(result) mustBe CREATED
      contentAsJson(result) mustBe responseJson

      verify(service).createSubcontractor(any[CreateSubcontractorRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val service = mock[SubcontractorService]
      val controller = mockController(service)

      val bad = Json.obj("schemeId" -> 1)

      val req = FakeRequest(POST, createSubcontractorUrl)
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubcontractor()(req)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result).toString must include("obj.subcontractorType")
      verifyNoInteractions(service)
    }

    "returns 502 when service fails" in {
      val service = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.createSubcontractor(any[CreateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req = FakeRequest(POST, createSubcontractorUrl)
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubcontractor()(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "create-subcontractor-failed"
    }

  }
}
