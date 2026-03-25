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
import org.mockito.{ArgumentMatchers, Mockito}
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, INTERNAL_SERVER_ERROR, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, JSON, POST, await, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.controllers.SubmissionController
import uk.gov.hmrc.constructionindustryscheme.models.audit.XmlConversionResult
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.services.{AuditService, SubmissionService}
import uk.gov.hmrc.constructionindustryscheme.utils.XmlValidator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl

import scala.xml.NodeSeq
import java.time.Clock
import scala.concurrent.Future
import scala.util.{Failure, Success}

final class SubmissionControllerSpec extends SpecBase with EitherValues {

  private val submissionId = "sub-123"

  private val validJson: JsValue = Json.obj(
    "utr"                   -> "1234567890",
    "aoReference"           -> "123/AB456",
    "monthYear"             -> "2025-05",
    "email"                 -> "test@test.com",
    "isAgent"               -> true,
    "clientTaxOfficeNumber" -> "123",
    "clientTaxOfficeRef"    -> "ABC456",
    "returnType"            -> "monthlyNilReturn",
    "informationCorrect"    -> "yes",
    "inactivity"            -> "no"
  )

  val mockAuditService: AuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    Mockito.reset(
      mockAuditService
    )
    super.beforeEach()
  }

  val appConfig: AppConfig = mock[AppConfig]

  private def mkController(
    submissionService: SubmissionService,
    auth: AuthAction = fakeAuthAction(),
    xmlValidator: XmlValidator,
    appConfig: AppConfig = appConfig,
    clock: Clock = Clock.systemUTC()
  ): SubmissionController =
    new SubmissionController(
      auth,
      submissionService,
      mockAuditService,
      xmlValidator,
      cc,
      appConfig,
      clock
    )

  "submitToChris" - {
    "returns 200 with SUBMITTED when service returns SubmittedStatus" in {
      val submissionService = mock[SubmissionService]

      val xmlValidator = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(mockAuditService.monthlyNilReturnResponseEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))

      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))
      when(
        submissionService.initialiseGovTalkStatus(any[EmployerReference], any[String], any[String], any[String])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful("instance-123"))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val payload = invocation.getArgument(0, classOf[BuiltSubmissionPayload])
          val result  = mkSubmissionResult(SUBMITTED)
          Future.successful(
            result.copy(meta = result.meta.copy(correlationId = payload.correlationId))
          )
        }

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId

      verify(submissionService, times(1)).submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }

    "returns 200 with SUBMITTED_NO_RECEIPT when service returns SubmittedNoReceiptStatus" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))
      when(
        submissionService.initialiseGovTalkStatus(any[EmployerReference], any[String], any[String], any[String])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful("instance-123"))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val payload = invocation.getArgument(0, classOf[BuiltSubmissionPayload])
          val result  = mkSubmissionResult(SUBMITTED_NO_RECEIPT)
          Future.successful(
            result.copy(meta = result.meta.copy(correlationId = payload.correlationId))
          )
        }

      val request =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "SUBMITTED_NO_RECEIPT"

      verify(submissionService).submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }

    "returns 202 with ACCEPTED when service returns AcceptedStatus" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))
      when(
        submissionService.initialiseGovTalkStatus(any[EmployerReference], any[String], any[String], any[String])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful("instance-123"))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val payload = invocation.getArgument(0, classOf[BuiltSubmissionPayload])
          val result  = mkSubmissionResult(ACCEPTED)
          Future.successful(
            result.copy(meta = result.meta.copy(correlationId = payload.correlationId))
          )
        }

      val request =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe 202
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "ACCEPTED"
      (js \ "responseEndPoint" \ "pollIntervalSeconds").as[Int] mustBe 15

      verify(submissionService).submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }

    "returns 200 with DEPARTMENTAL_ERROR and error object when service returns DepartmentalErrorStatus" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val err = GovTalkError("1234", "fatal", "boom")

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))
      when(
        submissionService.initialiseGovTalkStatus(any[EmployerReference], any[String], any[String], any[String])(
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful("instance-123"))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val payload = invocation.getArgument(0, classOf[BuiltSubmissionPayload])
          val result  = mkSubmissionResult(DEPARTMENTAL_ERROR, Some(err))
          Future.successful(
            result.copy(meta = result.meta.copy(correlationId = payload.correlationId))
          )
        }

      val request =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "DEPARTMENTAL_ERROR"
      val e  = (js \ "error").as[JsObject]
      (e \ "number").as[String] mustBe "1234"
      (e \ "type").as[String] mustBe "fatal"
      (e \ "text").as[String].toLowerCase must include("boom")

      verify(submissionService).submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }

    "returns 400 when request JSON is invalid" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq])).thenReturn(Success(()))

      val badJson = Json.obj("utr" -> 123)

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(badJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe BAD_REQUEST
      (contentAsJson(result) \ "message").isDefined mustBe true

      verifyNoInteractions(mockAuditService)
      verifyNoInteractions(submissionService)
    }

    "returns 200 OK with SUBMITTED when ChRIS submit fails but failure is recoverable" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      when(appConfig.chrisGatewayUrl).thenReturn("http://chris.example/gateway")

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))
      when(
        submissionService.initialiseGovTalkStatus(
          any[EmployerReference],
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful("instance-123"))
      when(submissionService.updateGovTalkStatus(any[UpdateGovTalkStatusRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe OK

      val js = contentAsJson(result)

      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "STARTED"

      verify(submissionService, times(1))
        .submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }

    "returns RuntimeException if xmlValidator.validate fails" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(mockAuditService.monthlyNilReturnResponseEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenReturn(Future.successful(mkSubmissionResult(SUBMITTED_NO_RECEIPT)))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Failure(new RuntimeException("XML validation failed due to exception")))

      when(xmlValidator.validate(any())).thenReturn(Failure(new Exception("invalid!")))

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
        .withBody(validJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      val thrown = intercept[RuntimeException] {
        await(result)
      }
      thrown.getMessage must include("XML validation failed: invalid!")
    }

    "returns 500 InternalServerError when GovTalk initialisation/update fails in recoverable ChRIS failure flow" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      when(appConfig.chrisGatewayUrl).thenReturn("http://chris.example/gateway")

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))

      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      when(
        submissionService.initialiseGovTalkStatus(
          any[EmployerReference],
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new RuntimeException("govtalk failure")))

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe INTERNAL_SERVER_ERROR
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "error" \ "text").as[String] mustBe "GovTalk status already exists"
    }

    "returns 502 BadGateway when sent or ack correlation id is empty" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))

      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { _ =>
          Future.successful(
            mkSubmissionResult(SUBMITTED).copy(meta = mkMeta(corrId = ""))
          )
        }

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(req)

      status(result) mustBe BAD_GATEWAY
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "error" \ "text").as[String] mustBe "empty correlationId"
    }

    "returns 502 BadGateway with FATAL_ERROR when correlation id validation fails" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))

      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val result = mkSubmissionResult(SUBMITTED).copy(
            meta = mkMeta(corrId = "DIFFERENT-CORRELATION-ID")
          )
          Future.successful(result)
        }

      val request =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe BAD_GATEWAY
      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "error" \ "text").as[String] must include("correlationId mismatch")

      verify(submissionService, never())
        .initialiseGovTalkStatus(any[EmployerReference], any[String], any[String], any[String])(any[HeaderCarrier])
    }

    "returns 502 BadGateway when ack correlation id is empty" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]

      val controller = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(mockAuditService.monthlyNilReturnRequestEvent(any())(any()))
        .thenReturn(Future.successful(AuditResult.Success))
      when(xmlValidator.validate(any[NodeSeq]))
        .thenReturn(Success(()))

      when(submissionService.submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier]))
        .thenAnswer { invocation =>
          val result = mkSubmissionResult(SUBMITTED).copy(
            meta = mkMeta(corrId = "")
          )

          Future.successful(result)
        }

      val request =
        FakeRequest(POST, s"/cis/submissions/$submissionId/submit-to-chris")
          .withBody(validJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.submitToChris(submissionId)(request)

      status(result) mustBe BAD_GATEWAY

      val js = contentAsJson(result)
      (js \ "submissionId").as[String] mustBe submissionId
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "error" \ "text").as[String] mustBe "empty correlationId"

      verify(submissionService).submitToChris(any[BuiltSubmissionPayload])(any[HeaderCarrier])
    }
  }

  "createSubmission" - {

    val validCreateJson = Json.obj(
      "instanceId" -> "123",
      "taxYear"    -> 2024,
      "taxMonth"   -> 4
    )

    "returns 201 with submissionId when service returns id" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(submissionService.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful("sub-999"))

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmission(req)

      status(result) mustBe CREATED
      (contentAsJson(result) \ "submissionId").as[String] mustBe "sub-999"

      verify(submissionService).createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val bad = Json.obj("taxYear" -> 2024)

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmission(req)

      status(result) mustBe BAD_REQUEST
      contentAsJson(result).toString must include("obj.instanceId")
      verifyNoInteractions(submissionService)
    }

    "returns 502 when service fails" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(submissionService.createSubmission(any[CreateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmission(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "create-submission-failed"
    }

    "returns 401 when unauthorised" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        auth = rejectingAuthAction,
        xmlValidator = xmlValidator
      )

      val req = FakeRequest(POST, "/cis/submissions/create-and-track")
        .withBody(validCreateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createSubmission(req)

      status(result) mustBe UNAUTHORIZED
      verifyNoInteractions(submissionService)
    }
  }

  "updateSubmission" - {

    val minimalUpdateJson = Json.obj(
      "instanceId"        -> "123",
      "taxYear"           -> 2024,
      "taxMonth"          -> 4,
      "submittableStatus" -> "REJECTED"
    )

    "returns 204 NoContent when service updates ok" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(submissionService.updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier]))
        .thenReturn(Future.unit)

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(minimalUpdateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe NO_CONTENT
      verify(submissionService).updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val bad = Json.obj("instanceId" -> "123")

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(submissionService)
    }

    "returns 502 BadGateway when service fails" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      when(submissionService.updateSubmission(any[UpdateSubmissionRequest])(any[HeaderCarrier]))
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
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        auth = rejectingAuthAction,
        xmlValidator = xmlValidator
      )

      val req = FakeRequest(POST, s"/cis/submissions/$submissionId/update")
        .withBody(minimalUpdateJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.updateSubmission(submissionId)(req)

      status(result) mustBe UNAUTHORIZED
      verifyNoInteractions(submissionService)
    }
  }

  "pollSubmission" - {

    "override polling url is true" - {

      "returns 200 with ACCEPTED status and override pollUrl when service returns ACCEPTED with pollUrl" in {
        val submissionService = mock[SubmissionService]
        val config            = mock[AppConfig]
        val xmlValidator      = mock[XmlValidator]

        when(config.chrisHost).thenReturn(Seq("chris.test"))
        when(config.useOverridePollResponseEndPoint).thenReturn(true)
        when(config.overridePollResponseEndPoint).thenReturn("override.chris.test")
        val controller = mkController(
          submissionService = submissionService,
          appConfig = config,
          xmlValidator = xmlValidator
        )

        val pollUrl         = "http://chris.test/poll"
        val overridePollUrl = "https://override.chris.test/poll"
        val correlationId   = "CORR999"

        when(
          submissionService.pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(overridePollUrl))(
            using any[HeaderCarrier]
          )
        )
          .thenReturn(
            Future.successful(
              ChrisPollResponse(ACCEPTED, correlationId, Some(overridePollUrl), Some(10), None, None, None)
            )
          )

        val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

        val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

        status(result) mustBe OK
        val js = contentAsJson(result)
        (js \ "status").as[String] mustBe "ACCEPTED"
        (js \ "pollUrl").as[String] mustBe overridePollUrl
        (js \ "intervalSeconds").as[Int] mustBe 10

        verify(submissionService)
          .pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(overridePollUrl))(using
            any[HeaderCarrier]
          )
      }

    }

    "returns 200 with SUBMITTED status when service returns SUBMITTED" in {
      val submissionService = mock[SubmissionService]
      val config            = mock[AppConfig]
      val xmlValidator      = mock[XmlValidator]

      when(config.chrisHost).thenReturn(Seq("chris.test"))
      val controller = mkController(
        submissionService = submissionService,
        appConfig = config,
        xmlValidator = xmlValidator
      )

      val pollUrl       = "http://chris.test/poll"
      val correlationId = "CORR123"

      when(
        submissionService.pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(Future.successful(ChrisPollResponse(SUBMITTED, correlationId, None, None, None, None, None)))

      val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

      val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "SUBMITTED"
      (js \ "pollUrl").asOpt[String] mustBe None
      (js \ "intervalSeconds").asOpt[Int] mustBe None

      verify(submissionService).pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
        any[HeaderCarrier]
      )
    }

    "returns 200 with FATAL_ERROR status when service returns FATAL_ERROR" in {
      val submissionService = mock[SubmissionService]
      val config            = mock[AppConfig]
      val xmlValidator      = mock[XmlValidator]

      when(config.chrisHost).thenReturn(Seq("chris.test"))
      val controller = mkController(
        submissionService = submissionService,
        appConfig = config,
        xmlValidator = xmlValidator
      )

      val pollUrl       = "http://chris.test/poll"
      val correlationId = "CORR456"

      when(
        submissionService.pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.successful(
            ChrisPollResponse(
              uk.gov.hmrc.constructionindustryscheme.models.FATAL_ERROR,
              correlationId,
              None,
              None,
              None,
              None,
              None
            )
          )
        )

      val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

      val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "FATAL_ERROR"
      (js \ "pollUrl").asOpt[String] mustBe None
      (js \ "intervalSeconds").asOpt[Int] mustBe None

      verify(submissionService).pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
        any[HeaderCarrier]
      )
    }

    "returns 200 with DEPARTMENTAL_ERROR status when service returns DEPARTMENTAL_ERROR" in {
      val submissionService = mock[SubmissionService]
      val config            = mock[AppConfig]
      val xmlValidator      = mock[XmlValidator]

      when(config.chrisHost).thenReturn(Seq("chris.test"))
      val controller = mkController(
        submissionService = submissionService,
        appConfig = config,
        xmlValidator = xmlValidator
      )

      val pollUrl       = "http://chris.test/poll"
      val correlationId = "CORR789"

      when(
        submissionService.pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.successful(ChrisPollResponse(DEPARTMENTAL_ERROR, correlationId, None, None, None, None, None))
        )

      val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

      val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "DEPARTMENTAL_ERROR"
      (js \ "pollUrl").asOpt[String] mustBe None
      (js \ "intervalSeconds").asOpt[Int] mustBe None

      verify(submissionService).pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
        any[HeaderCarrier]
      )
    }

    "returns 200 with ACCEPTED status and pollUrl when service returns ACCEPTED with pollUrl" in {
      val submissionService = mock[SubmissionService]
      val config            = mock[AppConfig]
      val xmlValidator      = mock[XmlValidator]

      when(config.chrisHost).thenReturn(Seq("chris.test"))
      val controller = mkController(
        submissionService = submissionService,
        appConfig = config,
        xmlValidator = xmlValidator
      )

      val pollUrl       = "http://chris.test/poll"
      val correlationId = "CORR999"

      when(
        submissionService.pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
          any[HeaderCarrier]
        )
      )
        .thenReturn(
          Future.successful(ChrisPollResponse(ACCEPTED, correlationId, Some(pollUrl), Some(10), None, None, None))
        )

      val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

      val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

      status(result) mustBe OK
      val js = contentAsJson(result)
      (js \ "status").as[String] mustBe "ACCEPTED"
      (js \ "pollUrl").as[String] mustBe pollUrl
      (js \ "intervalSeconds").as[Int] mustBe 10

      verify(submissionService).pollSubmission(ArgumentMatchers.eq(correlationId), ArgumentMatchers.eq(pollUrl))(using
        any[HeaderCarrier]
      )
    }

    "returns 401 when unauthorised" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        auth = rejectingAuthAction,
        xmlValidator = xmlValidator
      )

      val pollUrl       = "http://chris.test/poll"
      val correlationId = "CORR-UNAUTH"

      val req = FakeRequest(GET, s"/cis/submissions/poll?pollUrl=$pollUrl&correlationId=$correlationId")

      val result = controller.pollSubmission(RedirectUrl(pollUrl), correlationId)(req)

      status(result) mustBe UNAUTHORIZED
      verifyNoInteractions(submissionService)
    }
  }

  "createMonthlyNilReturnRequestJson" - {

    trait TestableMonthlyNilReturnService {
      def convertXmlToJson(xml: String): XmlConversionResult
      def createMonthlyNilReturnRequestJson(payload: BuiltSubmissionPayload): JsValue
    }

    "return correct JSON for successful conversion" in {
      val service: TestableMonthlyNilReturnService = new TestableMonthlyNilReturnService {
        def convertXmlToJson(xml: String): XmlConversionResult =
          XmlConversionResult(true, Some(Json.obj("ok" -> true)), None)

        def createMonthlyNilReturnRequestJson(payload: BuiltSubmissionPayload): JsValue =
          convertXmlToJson(payload.envelope.toString) match {
            case XmlConversionResult(true, Some(json), _)   => json
            case XmlConversionResult(false, _, Some(error)) => Json.obj("error" -> error)
            case _                                          => Json.obj("error" -> "unexpected conversion failure")
          }
      }
      val payload                                  = BuiltSubmissionPayload(<xml></xml>, "corr-1", "irmark-1", <xml></xml>)
      val result                                   = service.createMonthlyNilReturnRequestJson(payload)
      result mustBe Json.obj("ok" -> true)
    }
    "return error JSON when XML conversion fails" in {
      val service: TestableMonthlyNilReturnService = new TestableMonthlyNilReturnService {
        def convertXmlToJson(xml: String): XmlConversionResult =
          XmlConversionResult(false, None, Some("Invalid XML"))

        def createMonthlyNilReturnRequestJson(payload: BuiltSubmissionPayload): JsValue =
          convertXmlToJson(payload.envelope.toString) match {
            case XmlConversionResult(true, Some(json), _)   => json
            case XmlConversionResult(false, _, Some(error)) => Json.obj("error" -> error)
            case _                                          => Json.obj("error" -> "unexpected conversion failure")
          }
      }
      val payload                                  = BuiltSubmissionPayload(<xml>bad</xml>, "corr-2", "irmark-2", <xml></xml>)
      val result                                   = service.createMonthlyNilReturnRequestJson(payload)
      result mustBe Json.obj("error" -> "Invalid XML")
    }
    "return unexpected conversion failure when neither json nor error provided" in {
      val service: TestableMonthlyNilReturnService = new TestableMonthlyNilReturnService {
        def convertXmlToJson(xml: String): XmlConversionResult =
          XmlConversionResult(false, None, None)

        def createMonthlyNilReturnRequestJson(payload: BuiltSubmissionPayload): JsValue =
          convertXmlToJson(payload.envelope.toString) match {
            case XmlConversionResult(true, Some(json), _)   => json
            case XmlConversionResult(false, _, Some(error)) => Json.obj("error" -> error)
            case _                                          => Json.obj("error" -> "unexpected conversion failure")
          }
      }
      val payload                                  = BuiltSubmissionPayload(<xml>weird</xml>, "corr-3", "irmark-3", <xml></xml>)
      val result                                   = service.createMonthlyNilReturnRequestJson(payload)
      result mustBe Json.obj("error" -> "unexpected conversion failure")
    }
  }

  "createMonthlyNilReturnResponseJson" - {

    trait TestableMonthlyNilResponseService {
      def convertXmlToJson(xml: String): XmlConversionResult
      def createMonthlyNilReturnResponseJson(res: SubmissionResult): JsValue
    }

    val govTalkMeta = GovTalkMeta(
      qualifier = "response",
      function = "submit",
      className = "CIS300MR",
      correlationId = "correlationId",
      gatewayTimestamp = Some("gatewayTimestamp"),
      responseEndPoint = ResponseEndPoint("/poll", 100),
      error = None
    )
    val res         = SubmissionResult(SUBMITTED, "rawXml", govTalkMeta)

    "return correct JSON for successful conversion" in {
      val service: TestableMonthlyNilResponseService = new TestableMonthlyNilResponseService {
        def convertXmlToJson(xml: String): XmlConversionResult =
          XmlConversionResult(true, Some(Json.obj("ok" -> true)), None)

        def createMonthlyNilReturnResponseJson(res: SubmissionResult): JsValue =
          convertXmlToJson(res.rawXml) match {
            case XmlConversionResult(true, Some(json), _) => json
            case _                                        => Json.toJson(res.rawXml)
          }
      }
      val result                                     = service.createMonthlyNilReturnResponseJson(res)
      result mustBe Json.obj("ok" -> true)
    }
    "return error JSON when XML conversion fails" in {
      val service: TestableMonthlyNilResponseService = new TestableMonthlyNilResponseService {
        def convertXmlToJson(xml: String): XmlConversionResult =
          XmlConversionResult(false, None, Some("Invalid XML"))

        def createMonthlyNilReturnResponseJson(res: SubmissionResult): JsValue =
          convertXmlToJson(res.rawXml) match {
            case XmlConversionResult(true, Some(json), _) => json
            case _                                        => Json.toJson(res.rawXml)
          }
      }

      val result = service.createMonthlyNilReturnResponseJson(res)
      result mustBe Json.toJson(res.rawXml)
    }
  }

  "sendSuccessfulEmail" - {

    "returns 202 when service succeeds" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val body = Json.obj(
        "email" -> "test@test.com",
        "month" -> "September",
        "year"  -> "2025"
      )

      when(submissionService.sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/send-success-email")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendSuccessfulEmail(submissionId)(req)

      status(result) mustBe 202

      verify(submissionService, times(1)).sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(
        any[HeaderCarrier]
      )
    }

    "returns 400 when request JSON is invalid" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val badJson = Json.obj("email" -> "test@test.com")

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/send-success-email")
          .withBody(badJson)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendSuccessfulEmail(submissionId)(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(submissionService)
    }

    "returns 502 when service fails" in {
      val submissionService = mock[SubmissionService]
      val xmlValidator      = mock[XmlValidator]
      val controller        = mkController(
        submissionService = submissionService,
        xmlValidator = xmlValidator
      )

      val body = Json.obj(
        "email" -> "test@test.com",
        "month" -> "September",
        "year"  -> "2025"
      )

      when(submissionService.sendSuccessfulEmail(any[String], any[SendSuccessEmailRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val req =
        FakeRequest(POST, s"/cis/submissions/$submissionId/send-success-email")
          .withBody(body)
          .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.sendSuccessfulEmail(submissionId)(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "send-success-email-failed"
    }
  }

  private def mkMeta(
    corrId: String = "CID123",
    pollSecs: Int = 15,
    ts: Option[String] = None,
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
