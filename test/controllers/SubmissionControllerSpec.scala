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
import org.mockito.ArgumentMatchers
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, JSON, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.controllers.SubmissionController
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateAndTrackSubmissionRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, BuiltSubmissionPayload, DEPARTMENTAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SUBMITTED, SUBMITTED_NO_RECEIPT, SubmissionResult, SubmissionStatus, SuccessEmailParams}
import uk.gov.hmrc.constructionindustryscheme.services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future


final class SubmissionControllerSpec extends SpecBase with EitherValues {

  private val submissionId = "sub-123"

  private val validJson: JsValue = Json.obj(
    "utr" -> "1234567890",
    "aoReference" -> "123/AB456",
    "informationCorrect" -> "yes",
    "inactivity" -> "yes",
    "monthYear" -> "2025-09",
    "email" -> "test@test.com"
  )

  private def mkAppConfig(
                           missingMandatory: Boolean = false,
                           irmarkBad: Boolean = false
                         ): AppConfig = {
    val appConfig = mock[AppConfig]
    when(appConfig.chrisEnableMissingMandatory).thenReturn(missingMandatory)
    when(appConfig.chrisEnableIrmarkBad).thenReturn(irmarkBad)
    appConfig
  }

  private def mkController(
                            service: SubmissionService,
                            auth: AuthAction = fakeAuthAction(),
                            appConfig: AppConfig = mkAppConfig()
                          ): SubmissionController =
    new SubmissionController(auth, service, cc, appConfig)

  "submitToChris" - {

    "returns 200 with SUBMITTED when service returns SubmittedStatus" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      when(service.submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkSubmissionResult(SUBMITTED)))

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result= controller.submitToChris(submissionId)(request)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId

      verify(service, times(1)).submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier])
    }
    
    "returns 200 with SUBMITTED_NO_RECEIPT when service returns SubmittedNoReceiptStatus" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      when(service.submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkSubmissionResult(SUBMITTED_NO_RECEIPT)))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "SUBMITTED_NO_RECEIPT"
      (js \ "submissionId").as[String] mustBe submissionId
    }

    "returns 202 with ACCEPTED when service returns AcceptedStatus (includes nextPollInSeconds)" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      when(service.submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkSubmissionResult(ACCEPTED)))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe 202
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "ACCEPTED"
      (js \ "responseEndPoint" \ "pollIntervalSeconds").as[Int] mustBe 15
      (js \ "responseEndPoint" \ "url").as[String] must include("/poll")
    }

    "returns 200 with DEPARTMENTAL_ERROR and error object when service returns DepartmentalErrorStatus" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      val err = GovTalkError("1234", "fatal", "boom")
      when(service.submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkSubmissionResult(DEPARTMENTAL_ERROR, Some(err))))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "DEPARTMENTAL_ERROR"
      val e = (js \ "error").as[JsObject]
      (e \ "number").as[String] mustBe "1234"
      (e \ "type").as[String] mustBe "fatal"
      (e \ "text").as[String].toLowerCase must include ("boom")
    }

    "returns 400 when request JSON is invalid" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      val badJson = Json.obj("utr" -> 123)

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").isDefined mustBe true

      verifyNoInteractions(service)
    }

    "returns 502 BadGateway when service fails" in {
      val service    = mock[SubmissionService]
      val controller = mkController(service)

      when(service.submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe BAD_GATEWAY
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "error").as[String] mustBe "upstream-failure"

      verify(service, times(1)).submitToChris(any[BuiltSubmissionPayload], any[Option[SuccessEmailParams]])(any[HeaderCarrier])
    }
  }

  "createAndTrackSubmission" - {

    val validCreateJson = Json.obj(
      "instanceId" -> "123",
      "taxYear" -> 2024,
      "taxMonth" -> 4
    )

    "returns 201 with submissionId when service returns id" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      when(service.createAndTrackSubmission(any[CreateAndTrackSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful("sub-999"))

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndTrackSubmission(req)

      status(result) mustBe CREATED
      (contentAsJson(result) \ "submissionId").as[String] mustBe "sub-999"

      verify(service).createAndTrackSubmission(any[CreateAndTrackSubmissionRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      val bad = Json.obj("taxYear" -> 2024)

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndTrackSubmission(req)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result).toString must include("obj.instanceId")
      verifyNoInteractions(service)
    }

    "returns 502 when service fails" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      when(service.createAndTrackSubmission(any[CreateAndTrackSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndTrackSubmission(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "create-submission-failed"
    }

    "returns 401 when unauthorised" in {
      val service = mock[SubmissionService]
      val controller = mkController(service, auth = rejectingAuthAction)

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndTrackSubmission(req)

      status(result) mustBe UNAUTHORIZED
      verifyNoInteractions(service)
    }
  }

  "updateSubmission" - {

    val minimalUpdateJson = Json.obj(
      "instanceId" -> "123",
      "taxYear" -> 2024,
      "taxMonth" -> 4,
      "submittableStatus" -> "REJECTED"
    )

    "returns 204 NoContent when service updates ok" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      when(service.updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(minimalUpdateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe NO_CONTENT
      verify(service).updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      val bad = Json.obj("instanceId" -> "123")

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(service)
    }

    "returns 502 BadGateway when service fails" in {
      val service = mock[SubmissionService]
      val controller = mkController(service)

      when(service.updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp update failed")))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(minimalUpdateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe BAD_GATEWAY
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "message").as[String] mustBe "update-submission-failed"
    }

    "returns 401 when unauthorised" in {
      val service = mock[SubmissionService]
      val controller = mkController(service, auth = rejectingAuthAction)

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(minimalUpdateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe UNAUTHORIZED
      verifyNoInteractions(service)
    }
  }


  private def mkMeta(
                      corrId: String = "CID123",
                      pollSecs: Int = 15,
                      ts: String = "2025-01-01T00:00:00Z",
                      err: Option[GovTalkError] = None
                    ): GovTalkMeta =
    GovTalkMeta(
      qualifier = "response",
      function = "submit",
      className = "CIS300MR",
      correlationId = corrId,
      gatewayTimestamp = ts,
      responseEndPoint = ResponseEndPoint("/poll", pollSecs),
      error = err
    )

  private def mkSubmissionResult(
                                  status: SubmissionStatus,
                                  err: Option[GovTalkError] = None
                                ): SubmissionResult =
    SubmissionResult(
      status = status,
      rawXml = "<ack/>",
      meta = mkMeta(err = err)
    )

}
