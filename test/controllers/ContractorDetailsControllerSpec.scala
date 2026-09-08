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
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.ContractorDetailsController
import uk.gov.hmrc.constructionindustryscheme.models.UpdateContractorSchemeParams
import uk.gov.hmrc.constructionindustryscheme.services.ContractorDetailsService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class ContractorDetailsControllerSpec extends SpecBase with EitherValues {

  override def beforeEach(): Unit =
    super.beforeEach()

  private def mockController(
    service: ContractorDetailsService,
    auth: AuthAction = fakeAuthAction()
  ): ContractorDetailsController =
    new ContractorDetailsController(auth, service, cc)

  private val updateUrl = "/contractor-details/update"

  private val validJson: JsValue = Json.toJson(
    UpdateContractorSchemeParams(
      schemeId = 123,
      instanceId = "cisId",
      accountsOfficeReference = "123 PA 87654321",
      taxOfficeNumber = "123",
      taxOfficeReference = "45678",
      utr = Some("1234567890"),
      name = Some("Scheme ABC"),
      emailAddress = Some("test@mail.com"),
      version = Some(7)
    )
  )

  "updateContractorDetails" - {

    "returns 204 when service succeeds" in {

      val service    = mock[ContractorDetailsService]
      val controller = mockController(service)

      when(
        service.submitContractorDetails(
          any[UpdateContractorSchemeParams]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(()))

      val req = FakeRequest(POST, updateUrl)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result =
        controller.updateContractorDetails()(req)

      status(result) mustBe NO_CONTENT

      verify(service)
        .submitContractorDetails(
          any[UpdateContractorSchemeParams]
        )(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {

      val service    = mock[ContractorDetailsService]
      val controller = mockController(service)

      val badJson = Json.obj(
        "schemeId" -> 123
      )

      val req = FakeRequest(POST, updateUrl)
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result =
        controller.updateContractorDetails()(req)

      status(result) mustBe BAD_REQUEST

      verifyNoInteractions(service)
    }

    "returns exception when service fails" in {

      val service    = mock[ContractorDetailsService]
      val controller = mockController(service)

      when(
        service.submitContractorDetails(
          any[UpdateContractorSchemeParams]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.failed(
          new RuntimeException("formp down")
        )
      )

      val req = FakeRequest(POST, updateUrl)
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result =
        controller.updateContractorDetails()(req)

      whenReady(result.failed) { ex =>
        ex mustBe a[RuntimeException]
        ex.getMessage mustBe "formp down"
      }
    }
  }
}
