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
import org.mockito.Mockito.*
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.controllers.JourneyHandoffController
import uk.gov.hmrc.constructionindustryscheme.repositories.{JourneyHandoffData, JourneyHandoffRepository}

import java.time.Instant
import scala.concurrent.Future

class JourneyHandoffControllerSpec extends SpecBase {

  private val repo = mock[JourneyHandoffRepository]

  private val credentialId = "cred-123"
  private val journeyType  = "amend-monthly-return"
  private val handoffId    = "handoff-123"

  private def controller: JourneyHandoffController =
    new JourneyHandoffController(
      authorise = fakeAuthAction(),
      repo = repo,
      cc = cc
    )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(repo)
  }

  "JourneyHandoffController" - {

    "create" - {

      "must create handoff data and return Created with the handoff id" in {
        val body = Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        )

        when(repo.create(credentialId, journeyType, body))
          .thenReturn(Future.successful(handoffId))

        val request =
          FakeRequest(POST, s"/journey-handoffs/$journeyType")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withBody(body)

        val result = controller.create(journeyType)(request)

        status(result) mustBe CREATED
        contentAsJson(result) mustBe Json.obj("id" -> handoffId)

        verify(repo).create(credentialId, journeyType, body)
      }

      "must return BadRequest when JSON body is not an object" in {
        val request =
          FakeRequest(POST, s"/journey-handoffs/$journeyType")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withBody(Json.arr("invalid"))

        val result = controller.create(journeyType)(request)

        status(result) mustBe BAD_REQUEST
        contentAsJson(result) mustBe Json.obj("error" -> "Invalid JSON")

        verifyNoInteractions(repo)
      }

      "must return InternalServerError when repo create fails" in {
        val body = Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        )

        when(repo.create(credentialId, journeyType, body))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val request =
          FakeRequest(POST, s"/journey-handoffs/$journeyType")
            .withHeaders(CONTENT_TYPE -> JSON)
            .withBody(body)

        val result = controller.create(journeyType)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj("error" -> "Failed to create handoff data")

        verify(repo).create(credentialId, journeyType, body)
      }
    }

    "get" - {

      "must return Ok with handoff data when found" in {
        val data = Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        )

        val handoff = JourneyHandoffData(
          id = handoffId,
          userId = credentialId,
          journeyType = journeyType,
          data = data,
          lastUpdated = Instant.now()
        )

        when(repo.get(handoffId, credentialId, journeyType))
          .thenReturn(Future.successful(Some(handoff)))

        val request =
          FakeRequest(GET, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.get(journeyType, handoffId)(request)

        status(result) mustBe OK
        contentAsJson(result) mustBe data

        verify(repo).get(handoffId, credentialId, journeyType)
      }

      "must return NotFound when handoff data is not found" in {
        when(repo.get(handoffId, credentialId, journeyType))
          .thenReturn(Future.successful(None))

        val request =
          FakeRequest(GET, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.get(journeyType, handoffId)(request)

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.obj("error" -> "Handoff data not found")

        verify(repo).get(handoffId, credentialId, journeyType)
      }

      "must return InternalServerError when repo get fails" in {
        when(repo.get(handoffId, credentialId, journeyType))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val request =
          FakeRequest(GET, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.get(journeyType, handoffId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj("error" -> "Failed to retrieve handoff data")

        verify(repo).get(handoffId, credentialId, journeyType)
      }
    }

    "delete" - {

      "must return Ok when handoff data is deleted" in {
        when(repo.delete(handoffId, credentialId, journeyType))
          .thenReturn(Future.successful(true))

        val request =
          FakeRequest(DELETE, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.delete(journeyType, handoffId)(request)

        status(result) mustBe OK

        verify(repo).delete(handoffId, credentialId, journeyType)
      }

      "must return NotFound when handoff data is not found" in {
        when(repo.delete(handoffId, credentialId, journeyType))
          .thenReturn(Future.successful(false))

        val request =
          FakeRequest(DELETE, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.delete(journeyType, handoffId)(request)

        status(result) mustBe NOT_FOUND
        contentAsJson(result) mustBe Json.obj("error" -> "Handoff data not found")

        verify(repo).delete(handoffId, credentialId, journeyType)
      }

      "must return InternalServerError when repo delete fails" in {
        when(repo.delete(handoffId, credentialId, journeyType))
          .thenReturn(Future.failed(new RuntimeException("boom")))

        val request =
          FakeRequest(DELETE, s"/journey-handoffs/$journeyType/$handoffId")

        val result = controller.delete(journeyType, handoffId)(request)

        status(result) mustBe INTERNAL_SERVER_ERROR
        contentAsJson(result) mustBe Json.obj("error" -> "Failed to delete handoff data")

        verify(repo).delete(handoffId, credentialId, journeyType)
      }
    }
  }
}
