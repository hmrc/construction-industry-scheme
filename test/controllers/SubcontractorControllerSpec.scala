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
import org.scalatest.EitherValues
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.{CONTENT_TYPE, GET, JSON, POST, contentAsJson, status}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.SubcontractorController
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAndUpdateSubcontractorRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.GetSubcontractorForDeleteResponse
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.constructionindustryscheme.models.Subcontractor
import uk.gov.hmrc.constructionindustryscheme.models.response.GetSubcontractorListResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

final class SubcontractorControllerSpec extends SpecBase with EitherValues {
  override def beforeEach(): Unit =
    super.beforeEach()

  private def mockController(
    subcontractorService: SubcontractorService,
    auth: AuthAction = fakeAuthAction()
  ): SubcontractorController =
    new SubcontractorController(auth, subcontractorService, cc)

  val cisId             = "1"
  val subbieResourceRef = 10L

  "createAndUpdateSubcontractor" - {

    val updateSubcontractorUrl = "/subcontractor/update"

    val validSoleTraderJson: JsValue = Json.toJson(
      CreateAndUpdateSubcontractorRequest.SoleTraderRequest(
        cisId = cisId,
        utr = Some("1234567890"),
        nino = Some("AA123456A"),
        firstName = Some("John"),
        secondName = Some("Q"),
        surname = Some("Smith"),
        country = Some("United Kingdom"),
        tradingName = Some("trading Name")
      )
    )

    val validCompanyJson: JsValue = Json.toJson(
      CreateAndUpdateSubcontractorRequest.CompanyRequest(
        cisId = cisId,
        utr = Some("1234567890"),
        crn = Some("CRN123"),
        tradingName = Some("ACME Ltd"),
        country = Some("United Kingdom")
      )
    )

    val validPartnershipJson: JsValue = Json.toJson(
      CreateAndUpdateSubcontractorRequest.PartnershipRequest(
        cisId = cisId,
        utr = Some("1111111111"),
        partnerUtr = Some("2222222222"),
        partnershipTradingName = Some("My Partnership"),
        partnerTradingName = Some("Nominated Partner"),
        country = Some("United Kingdom")
      )
    )

    "returns 204 when service succeeds (sole trader)" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, updateSubcontractorUrl)
        .withBody(validSoleTraderJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndUpdateSubcontractor()(req)

      status(result) mustBe NO_CONTENT
      verify(service).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier])
    }

    "returns 204 when service succeeds (company)" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, updateSubcontractorUrl)
        .withBody(validCompanyJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndUpdateSubcontractor()(req)

      status(result) mustBe NO_CONTENT
      verify(service).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier])
    }

    "returns 204 when service succeeds (partnership)" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val req = FakeRequest(POST, updateSubcontractorUrl)
        .withBody(validPartnershipJson)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndUpdateSubcontractor()(req)

      status(result) mustBe NO_CONTENT
      verify(service).createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier])
    }

    "returns 400 when JSON is invalid" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      val bad = Json.obj("schemeId" -> 1)

      val req = FakeRequest(POST, updateSubcontractorUrl)
        .withBody(bad)
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndUpdateSubcontractor()(req)

      status(result) mustBe BAD_REQUEST
      verifyNoInteractions(service)
    }

    "returns 502 when service fails" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.createAndUpdateSubcontractor(any[CreateAndUpdateSubcontractorRequest])(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req = FakeRequest(POST, updateSubcontractorUrl)
        .withBody(validSoleTraderJson) // any valid payload is fine here
        .withHeaders(CONTENT_TYPE -> JSON)

      val result = controller.createAndUpdateSubcontractor()(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "create-and-update-subcontractor-failed"
    }
  }

  "getSubcontractorUTRs" - {

    val cisId                   = "cis-123"
    val getSubcontractorUTRsUrl = s"/subcontractors/utr/$cisId"

    "returns 200 with subcontractor UTR list response when service returns data" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      val subcontractorUTRs: Seq[String] = Seq("1111111111", "2222222222")
      val responseJson                   = Json.obj("subcontractorUTRs" -> subcontractorUTRs)

      when(service.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(subcontractorUTRs))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getSubcontractorUTRsUrl)
      val result                                   = controller.getSubcontractorUTRs(cisId)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe responseJson

      verify(service).getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier])
    }

    "returns 502 when service fails" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.getSubcontractorUTRs(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, getSubcontractorUTRsUrl)
      val result                                   = controller.getSubcontractorUTRs(cisId)(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "get-subcontractorUTRs-failed"
    }
  }

  "getSubcontractorDeleteStatus" - {

    val url =
      s"/subcontractor/$cisId/$subbieResourceRef/delete-status"

    val mockResponse =
      GetSubcontractorForDeleteResponse(
        subcontractorName = "Gamma Builders",
        subcontractorCanBeDeleted = true
      )

    "return OK and response json when service succeeds" in {

      val service = mock[SubcontractorService]

      when(
        service.getSubcontractorDeleteStatus(
          any[String],
          any[Long]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(mockResponse)
      )

      val controller =
        mockController(service)

      val request =
        FakeRequest(GET, url)

      val result =
        controller.getSubcontractorDeleteStatus(
          cisId,
          subbieResourceRef
        )(request)

      status(result) mustBe OK

      contentAsJson(result) mustBe
        Json.toJson(mockResponse)

      verify(service)
        .getSubcontractorDeleteStatus(
          eqTo(cisId),
          eqTo(subbieResourceRef)
        )(any())
    }

    "return BadGateway when service fails" in {

      val service = mock[SubcontractorService]

      when(
        service.getSubcontractorDeleteStatus(
          eqTo(cisId),
          eqTo(subbieResourceRef)
        )(any())
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val controller =
        mockController(service)

      val request =
        FakeRequest(GET, url)

      val result =
        controller.getSubcontractorDeleteStatus(
          cisId,
          subbieResourceRef
        )(request)

      status(result) mustBe BAD_GATEWAY

      contentAsJson(result) mustBe Json.obj(
        "message" -> "get-subcontractor-delete-status-failed"
      )

      verify(service)
        .getSubcontractorDeleteStatus(
          eqTo(cisId),
          eqTo(subbieResourceRef)
        )(any())
    }
  }
  "getSubcontractorList" - {

    val cisId                   = "cis-123"
    val getSubcontractorListUrl = s"/subcontractors/$cisId"

    val subcontractor =
      Json
        .obj(
          "subcontractorId"    -> 999L,
          "subbieResourceRef"  -> 456L,
          "utr"                -> "1234567890",
          "firstName"          -> "John",
          "secondName"         -> "Q",
          "surname"            -> "Smith",
          "tradingName"        -> "John Smith Trading",
          "subcontractorType"  -> "soletrader",
          "country"            -> "United Kingdom",
          "postcode"           -> "AA1 1AA",
          "taxTreatment"       -> "NET",
          "verificationNumber" -> "V123456",
          "verified"           -> "Y",
          "matched"            -> "Y",
          "version"            -> 1
        )
        .as[Subcontractor]

    val response = GetSubcontractorListResponse(
      subcontractors = List(subcontractor)
    )

    "returns 200 with the subcontractor list when the service succeeds" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.getSubcontractorList(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, getSubcontractorListUrl)

      val result = controller.getSubcontractorList(cisId)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.toJson(response)

      (contentAsJson(result) \ "subcontractors")(0).\("subcontractorId").as[Long] mustBe 999L
      (contentAsJson(result) \ "subcontractors")(0).\("utr").as[String] mustBe "1234567890"
      (contentAsJson(result) \ "subcontractors")(0).\("displayName").as[String] mustBe "John Smith"

      verify(service).getSubcontractorList(eqTo(cisId))(any[HeaderCarrier])
    }

    "returns 200 with an empty subcontractor list when no subcontractors exist" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      val response = GetSubcontractorListResponse(List.empty)

      when(service.getSubcontractorList(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(response))

      val req: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, getSubcontractorListUrl)

      val result = controller.getSubcontractorList(cisId)(req)

      status(result) mustBe OK
      contentAsJson(result) mustBe Json.obj("subcontractors" -> Json.arr())

      verify(service).getSubcontractorList(eqTo(cisId))(any[HeaderCarrier])
    }

    "returns 502 when the service fails" in {
      val service    = mock[SubcontractorService]
      val controller = mockController(service)

      when(service.getSubcontractorList(eqTo(cisId))(any[HeaderCarrier]))
        .thenReturn(Future.failed(new RuntimeException("formp down")))

      val req: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(GET, getSubcontractorListUrl)

      val result = controller.getSubcontractorList(cisId)(req)

      status(result) mustBe BAD_GATEWAY
      (contentAsJson(result) \ "message").as[String] mustBe "get-subcontractor-list-failed"

      verify(service).getSubcontractorList(eqTo(cisId))(any[HeaderCarrier])
    }
  }

}
