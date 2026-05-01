package controllers

import base.SpecBase
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.AmendMonthlyReturnController
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAmendedMonthlyReturnRequest
import uk.gov.hmrc.constructionindustryscheme.services.AmendMonthlyReturnService
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class AmendMonthlyReturnControllerSpec extends SpecBase {

  private val requestBody = CreateAmendedMonthlyReturnRequest(
    instanceId = "1",
    taxYear = 2025,
    taxMonth = 1,
    version = 0
  )

  private def buildController(
    service: AmendMonthlyReturnService,
    authAction: AuthAction = fakeAuthAction()
  ): AmendMonthlyReturnController =
    new AmendMonthlyReturnController(
      authorise = authAction,
      service = service,
      cc = cc
    )

  "AmendMonthlyReturnController.createAmendedMonthlyReturn" - {

    "must return Created when the service succeeds" in {
      val mockService = mock[AmendMonthlyReturnService]

      when(mockService.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val result =
        buildController(mockService)
          .createAmendedMonthlyReturn(fakeRequest.withBody(requestBody))

      status(result) mustBe CREATED

      verify(mockService).createAmendedMonthlyReturn(
        any[CreateAmendedMonthlyReturnRequest]
      )(any[HeaderCarrier])
    }

    "must return the upstream status when the service fails with UpstreamErrorResponse" in {
      val mockService = mock[AmendMonthlyReturnService]

      when(mockService.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("formp error", BAD_REQUEST, BAD_REQUEST)))

      val result =
        buildController(mockService)
          .createAmendedMonthlyReturn(fakeRequest.withBody(requestBody))

      status(result) mustBe BAD_REQUEST
      contentAsJson(result) mustBe Json.obj("message" -> "formp error")
    }

    "must return InternalServerError when the service fails unexpectedly" in {
      val mockService = mock[AmendMonthlyReturnService]

      when(mockService.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("boom")))

      val result =
        buildController(mockService)
          .createAmendedMonthlyReturn(fakeRequest.withBody(requestBody))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe Json.obj("message" -> "Unexpected error")
    }

    "must return Unauthorized when auth fails" in {
      val mockService = mock[AmendMonthlyReturnService]

      val result =
        buildController(mockService, rejectingAuthAction)
          .createAmendedMonthlyReturn(fakeRequest.withBody(requestBody))

      status(result) mustBe UNAUTHORIZED

      verifyNoInteractions(mockService)
    }
  }
}
