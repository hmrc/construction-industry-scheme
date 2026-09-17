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
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationDraft
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import uk.gov.hmrc.constructionindustryscheme.models.FinalValidationSubcontractorPatch
import uk.gov.hmrc.constructionindustryscheme.services.{FinalValidationDraftNotReadyException, FinalValidationDraftService}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.constructionindustryscheme.controllers.FinalValidationDraftController
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class FinalValidationDraftControllerSpec extends SpecBase {

  private val finalValidationDraftService =
    mock[FinalValidationDraftService]

  private lazy val controller =
    new FinalValidationDraftController(
      fakeAuthAction(),
      finalValidationDraftService,
      cc
    )

  private val credentialId =
    "cred-123"

  private val instanceId =
    "1"

  private val draftId =
    "draft-123"

  private val subcontractorId =
    10903L

  private val createRequest =
    CreateFinalValidationDraftRequest(
      instanceId = instanceId,
      context = "monthly",
      subcontractors = Seq.empty
    )

  private val correctionRequest =
    FinalValidationCorrectionRequest(
      subcontractorId = subcontractorId,
      changeTarget = "utr",
      patch = FinalValidationSubcontractorPatch()
    )

  private val readinessRequest =
    UpdateFinalValidationReadinessRequest(
      subcontractorId = subcontractorId,
      issues = Seq.empty
    )

  private val draft =
    Json
      .parse(
        """
          |{
          |  "subcontractors": [
          |    {
          |      "subcontractorId": 10903,
          |      "subbieResourceRef": 7,
          |      "baseVersion": 12,
          |      "subcontractorType": "soletrader",
          |      "displayName": "A Alice",
          |      "base": {
          |        "firstName": "A",
          |        "surname": "Alice",
          |        "utr": "1111111111",
          |        "nino": "PX123456A"
          |      },
          |      "proposed": {
          |        "firstName": "A",
          |        "surname": "Alice",
          |        "utr": "2234567890",
          |        "nino": "PX123456A"
          |      },
          |      "changedTargets": [
          |        "utrYesNo",
          |        "utr"
          |      ],
          |      "issues": [],
          |      "readiness": "Complete",
          |      "commitStatus": "Pending"
          |    }
          |  ]
          |}
          |""".stripMargin
      )
      .as[FinalValidationDraft]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(finalValidationDraftService)
  }

  "create" - {

    "return CREATED when the draft is created" in {

      when(
        finalValidationDraftService.create(
          credentialId,
          createRequest
        )
      ).thenReturn(
        Future.successful(draftId)
      )

      val result =
        controller
          .create()(
            FakeRequest()
              .withBody(createRequest)
          )

      status(result) mustBe CREATED

      contentAsJson(result) mustBe Json.obj(
        "draftId" -> draftId
      )

      verify(finalValidationDraftService)
        .create(
          credentialId,
          createRequest
        )
    }

    "return INTERNAL_SERVER_ERROR when creating the draft fails" in {

      when(
        finalValidationDraftService.create(
          credentialId,
          createRequest
        )
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val result =
        controller
          .create()(
            FakeRequest()
              .withBody(createRequest)
          )

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Failed to create Final Validation draft"
      )
    }
  }

  "get" - {

    "return OK when the draft is found" in {

      when(
        finalValidationDraftService.get(
          draftId,
          credentialId,
          instanceId
        )
      ).thenReturn(
        Future.successful(draft)
      )

      val result =
        controller
          .get(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(draft)

      verify(finalValidationDraftService)
        .get(
          draftId,
          credentialId,
          instanceId
        )
    }

    "return NOT_FOUND when the draft is not found" in {

      when(
        finalValidationDraftService.get(
          draftId,
          credentialId,
          instanceId
        )
      ).thenReturn(
        Future.failed(
          new NoSuchElementException("draft not found")
        )
      )

      val result =
        controller
          .get(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe NOT_FOUND

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Final Validation draft not found"
      )
    }

    "return INTERNAL_SERVER_ERROR when retrieving the draft fails" in {

      when(
        finalValidationDraftService.get(
          draftId,
          credentialId,
          instanceId
        )
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val result =
        controller
          .get(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Failed to retrieve Final Validation draft"
      )
    }
  }

  "updateCorrection" - {

    "return OK when the correction is updated" in {

      when(
        finalValidationDraftService.updateCorrection(
          draftId,
          credentialId,
          instanceId,
          correctionRequest
        )
      ).thenReturn(
        Future.successful(draft)
      )

      val result =
        controller
          .updateCorrection(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(correctionRequest)
          )

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(draft)

      verify(finalValidationDraftService)
        .updateCorrection(
          draftId,
          credentialId,
          instanceId,
          correctionRequest
        )
    }

    "return NOT_FOUND when the draft is not found" in {

      when(
        finalValidationDraftService.updateCorrection(
          draftId,
          credentialId,
          instanceId,
          correctionRequest
        )
      ).thenReturn(
        Future.failed(
          new NoSuchElementException("draft not found")
        )
      )

      val result =
        controller
          .updateCorrection(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(correctionRequest)
          )

      status(result) mustBe NOT_FOUND

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Final Validation draft not found"
      )
    }

    "return INTERNAL_SERVER_ERROR when updating the correction fails" in {

      when(
        finalValidationDraftService.updateCorrection(
          draftId,
          credentialId,
          instanceId,
          correctionRequest
        )
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val result =
        controller
          .updateCorrection(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(correctionRequest)
          )

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Failed to update Final Validation correction"
      )
    }
  }

  "updateReadiness" - {

    "return OK when the readiness is updated" in {

      when(
        finalValidationDraftService.updateReadiness(
          draftId,
          credentialId,
          instanceId,
          readinessRequest
        )
      ).thenReturn(
        Future.successful(draft)
      )

      val result =
        controller
          .updateReadiness(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(readinessRequest)
          )

      status(result) mustBe OK

      contentAsJson(result) mustBe Json.toJson(draft)

      verify(finalValidationDraftService)
        .updateReadiness(
          draftId,
          credentialId,
          instanceId,
          readinessRequest
        )
    }

    "return NOT_FOUND when the draft is not found" in {

      when(
        finalValidationDraftService.updateReadiness(
          draftId,
          credentialId,
          instanceId,
          readinessRequest
        )
      ).thenReturn(
        Future.failed(
          new NoSuchElementException("draft not found")
        )
      )

      val result =
        controller
          .updateReadiness(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(readinessRequest)
          )

      status(result) mustBe NOT_FOUND

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Final Validation draft not found"
      )
    }

    "return INTERNAL_SERVER_ERROR when updating readiness fails" in {

      when(
        finalValidationDraftService.updateReadiness(
          draftId,
          credentialId,
          instanceId,
          readinessRequest
        )
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val result =
        controller
          .updateReadiness(
            instanceId = instanceId,
            draftId = draftId
          )(
            FakeRequest()
              .withBody(readinessRequest)
          )

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Failed to update Final Validation readiness"
      )
    }
  }

  "commit" - {

    "return NO_CONTENT when the draft is committed" in {

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.successful(())
      )

      val result =
        controller
          .commit(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe NO_CONTENT

      verify(finalValidationDraftService)
        .commit(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
    }

    "return NOT_FOUND when the draft is not found" in {

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.failed(
          new NoSuchElementException("draft not found")
        )
      )

      val result =
        controller
          .commit(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe NOT_FOUND

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Final Validation draft not found"
      )
    }

    "return CONFLICT when the draft is not ready to commit" in {

      val error =
        new FinalValidationDraftNotReadyException(
          "Final Validation draft is not ready to commit"
        )

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.failed(error)
      )

      val result =
        controller
          .commit(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe CONFLICT

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Final Validation draft is not ready to commit"
      )
    }

    "return INTERNAL_SERVER_ERROR when committing the draft fails" in {

      when(
        finalValidationDraftService.commit(
          any[String],
          any[String],
          any[String]
        )(any[HeaderCarrier])
      ).thenReturn(
        Future.failed(
          new RuntimeException("boom")
        )
      )

      val result =
        controller
          .commit(
            instanceId = instanceId,
            draftId = draftId
          )(fakeRequest)

      status(result) mustBe INTERNAL_SERVER_ERROR

      contentAsJson(result) mustBe Json.obj(
        "error" -> "Failed to commit Final Validation draft"
      )
    }
  }

}
