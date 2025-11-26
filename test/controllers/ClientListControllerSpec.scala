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

import actions.FakeAuthAction
import base.SpecBase
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import play.api.libs.json.Json
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.controllers.ClientListController
import uk.gov.hmrc.constructionindustryscheme.models.ClientListStatus.{Failed, InProgress, InitiateDownload, Succeeded}
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, CisTaxpayerSearchResult}
import uk.gov.hmrc.constructionindustryscheme.services.clientlist.*
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.rdsdatacacheproxy.cis.models.ClientSearchResult

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

    "return 403 Forbidden with \"Missing credentialId\" when no credentialId is available" in {
      val mockService = mock[ClientListService]

      val controller =
        new ClientListController(noEnrolmentReferenceAuthAction, mockService, cc)

      val result = controller.start()(fakeRequest)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj("message" -> "Missing credentialId")

      verify(mockService, never()).process(any[String])(any[HeaderCarrier])
    }
  }

  "ClientListController.getAllClients" - {

    val irAgentId = "SA123456"
    val credId = "cred-123"

    "return 200 OK with ClientSearchResult when both irAgentId and credentialId are available" in {
      val mockService = mock[ClientListService]

      val clientSearchResult = ClientSearchResult(
        clients = List(
          CisTaxpayerSearchResult(
            uniqueId = "client-1",
            taxOfficeNumber = "111",
            taxOfficeRef = "test111",
            aoDistrict = Some("district1"),
            aoPayType = Some("type1"),
            aoCheckCode = Some("check1"),
            aoReference = Some("ref1"),
            validBusinessAddr = Some("Y"),
            correlation = Some("corr1"),
            ggAgentId = Some("agent1"),
            employerName1 = Some("Test Company Ltd"),
            employerName2 = Some("Test Company"),
            agentOwnRef = Some("own-ref-1"),
            schemeName = Some("Test Scheme")
          )
        ),
        totalCount = 1,
        clientNameStartingCharacters = List("T")
      )

      when(mockService.getClientList(any[String], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(clientSearchResult))

      val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "totalCount").as[Int] mustBe 1
      (json \ "clients").as[List[CisTaxpayerSearchResult]] must have size 1
      (json \ "clientNameStartingCharacters").as[List[String]] mustBe List("T")

      verify(mockService, times(1)).getClientList(any[String], any[String])(using any[HeaderCarrier])
    }

    "return 200 OK with empty client list when no clients found" in {
      val mockService = mock[ClientListService]

      val emptyResult = ClientSearchResult(
        clients = List.empty,
        totalCount = 0,
        clientNameStartingCharacters = List.empty
      )

      when(mockService.getClientList(any[String], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.successful(emptyResult))

      val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      status(result) mustBe OK
      val json = contentAsJson(result)
      (json \ "totalCount").as[Int] mustBe 0
      (json \ "clients").as[List[CisTaxpayerSearchResult]] mustBe empty
      (json \ "clientNameStartingCharacters").as[List[String]] mustBe empty
    }

    "return 403 Forbidden when IR-PAYE-AGENT enrolment is missing" in {
      val mockService = mock[ClientListService]

      val authAction = FakeAuthAction.withEnrolments(Set.empty, bodyParsers, Some(credId))
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj("error" -> "credentialId and/or irAgentId are missing from session")

      verify(mockService, never()).getClientList(any[String], any[String])(using any[HeaderCarrier])
    }

    "return 403 Forbidden when credentialId is missing" in {
      val mockService = mock[ClientListService]

      val authAction = FakeAuthAction.withEnrolments(
        Set(uk.gov.hmrc.auth.core.Enrolment(
          key = "IR-PAYE-AGENT",
          identifiers = Seq(uk.gov.hmrc.auth.core.EnrolmentIdentifier("IRAgentReference", irAgentId)),
          state = "Activated"
        )),
        bodyParsers,
        credId = None
      )
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj("error" -> "credentialId and/or irAgentId are missing from session")

      verify(mockService, never()).getClientList(any[String], any[String])(using any[HeaderCarrier])
    }

    "return 403 Forbidden when both irAgentId and credentialId are missing" in {
      val mockService = mock[ClientListService]

      val controller = new ClientListController(noEnrolmentReferenceAuthAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj("error" -> "credentialId and/or irAgentId are missing from session")

      verify(mockService, never()).getClientList(any[String], any[String])(using any[HeaderCarrier])
    }

    "propagate upstream errors from service" in {
      val mockService = mock[ClientListService]

      when(mockService.getClientList(any[String], any[String])(using any[HeaderCarrier]))
        .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", 503, 503)))

      val authAction = FakeAuthAction.withIrPayeAgent(irAgentId, bodyParsers, credId)
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.getAllClients()(fakeRequest)

      val thrown = intercept[UpstreamErrorResponse] {
        status(result)
      }
      thrown.statusCode mustBe 503
      thrown.message must include("Service unavailable")
    }
  }

  "ClientListController.clientTaxpayer" - {

    val uniqueId = "client-1"
    val testIrAgentId = "SA123456"
    val testCredId = "cred-123"

    "return 200 OK with CisTaxpayer JSON when agent + cred + client exist" in {
      val mockService = mock[ClientListService]

      val taxpayer = CisTaxpayer(
        uniqueId         = uniqueId,
        taxOfficeNumber  = "111",
        taxOfficeRef     = "test111",
        aoDistrict       = None,
        aoPayType        = None,
        aoCheckCode      = None,
        aoReference      = None,
        validBusinessAddr = None,
        correlation      = None,
        ggAgentId        = None,
        employerName1    = Some("Test Company Ltd"),
        employerName2    = None,
        agentOwnRef      = Some("own-ref-1"),
        schemeName       = Some("Test Scheme"),
        utr              = Some("1234567890"),
        enrolledSig      = None
      )

      when(
        mockService.getClientTaxpayer(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.successful(taxpayer))

      val authAction = FakeAuthAction.withIrPayeAgent(testIrAgentId, bodyParsers, testCredId)
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.clientTaxpayer(uniqueId)(fakeRequest)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(taxpayer)

      verify(mockService, times(1))
        .getClientTaxpayer(any[String], any[String], any[String])(any[HeaderCarrier])
    }

    "return 404 NotFound when service throws NoSuchElementException (client not found for this agent)" in {
      val mockService = mock[ClientListService]

      when(
        mockService.getClientTaxpayer(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(new NoSuchElementException("no client")))

      val authAction = FakeAuthAction.withIrPayeAgent(testIrAgentId, bodyParsers, testCredId)
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.clientTaxpayer(uniqueId)(fakeRequest)

      status(result) mustBe NOT_FOUND
      contentAsJson(result) mustBe Json.obj("message" -> "Client not found for this agent")

      verify(mockService, times(1))
        .getClientTaxpayer(any[String], any[String], any[String])(any[HeaderCarrier])
    }

    "return 403 Forbidden when IR-PAYE-AGENT enrolment or credentialId are missing" in {
      val mockService = mock[ClientListService]

      val authAction = FakeAuthAction.withEnrolments(
        Set.empty,
        bodyParsers,
        None
      )
      val controller = new ClientListController(authAction, mockService, cc)

      val result = controller.clientTaxpayer(uniqueId)(fakeRequest)

      status(result) mustBe FORBIDDEN
      contentAsJson(result) mustBe Json.obj("error" -> "credentialId and/or irAgentId are missing from session")

      verify(mockService, never())
        .getClientTaxpayer(any[String], any[String], any[String])(any[HeaderCarrier])
    }
  }
}
