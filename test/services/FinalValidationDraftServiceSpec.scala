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

package services

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.FinalValidationSubcontractorPatch
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.*
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, verifyNoInteractions, when}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.constructionindustryscheme.repositories.{FinalValidationDraftData, FinalValidationDraftRepository}
import uk.gov.hmrc.constructionindustryscheme.services.FinalValidationDraftService
import uk.gov.hmrc.constructionindustryscheme.services.FinalValidationDraftNotReadyException
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import scala.concurrent.Future

class FinalValidationDraftServiceSpec extends SpecBase {

  private val repository          = mock[FinalValidationDraftRepository]
  private val formpProxyConnector = mock[FormpProxyConnector]
  private val service             = new FinalValidationDraftService(repository, formpProxyConnector)

  private val userId     = "cred-123"
  private val instanceId = "1"
  private val draftId    = "draft-123"

  private val details =
    Json
      .obj(
        "firstName" -> "A",
        "surname"   -> "Alice",
        "utr"       -> "1111111111"
      )
      .as[FinalValidationSubcontractorDetails]

  private val issue =
    Json
      .obj(
        "fieldKey" -> "utr",
        "value"    -> "1111111111"
      )
      .as[FinalValidationDraftIssue]

  private def draft(
    readiness: String = "Incomplete",
    changedTargets: Set[String] = Set.empty,
    commitStatus: String = "Pending",
    proposedUtr: String = "1111111111"
  ): FinalValidationDraft =
    Json
      .obj(
        "subcontractors" -> Json.arr(
          Json.obj(
            "subcontractorId"   -> 10903L,
            "subbieResourceRef" -> 7L,
            "baseVersion"       -> 12,
            "subcontractorType" -> "soletrader",
            "displayName"       -> "A Alice",
            "base"              -> details,
            "proposed"          -> Json.obj(
              "firstName" -> "A",
              "surname"   -> "Alice",
              "utr"       -> proposedUtr
            ),
            "changedTargets"    -> changedTargets,
            "issues"            -> Seq(issue),
            "readiness"         -> readiness,
            "commitStatus"      -> commitStatus
          )
        )
      )
      .as[FinalValidationDraft]

  private def stored(value: FinalValidationDraft) =
    FinalValidationDraftData(
      draftId,
      userId,
      instanceId,
      "monthly",
      Json.toJsObject(value),
      0L,
      Instant.EPOCH
    )

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(repository, formpProxyConnector)
  }

  "create" - {

    "fail when there are no subcontractors" in {
      val error =
        service
          .create(userId, CreateFinalValidationDraftRequest(instanceId, "monthly", Seq.empty))
          .failed
          .futureValue

      error.getMessage mustBe "Cannot create Final Validation draft without subcontractors"
      verifyNoInteractions(repository)
    }

    "fail when a subcontractor has no issues" in {
      val request =
        CreateFinalValidationDraftRequest(
          instanceId,
          "monthly",
          Seq(
            CreateFinalValidationDraftSubcontractor(
              10903L,
              7L,
              Some(12),
              Some("soletrader"),
              "A Alice",
              details,
              Seq.empty
            )
          )
        )

      service.create(userId, request).failed.futureValue.getMessage mustBe
        "Cannot create Final Validation draft for subcontractor 10903 without validation issues"

      verifyNoInteractions(repository)
    }

    "create a draft" in {
      val request =
        CreateFinalValidationDraftRequest(
          instanceId,
          "monthly",
          Seq(
            CreateFinalValidationDraftSubcontractor(
              10903L,
              7L,
              Some(12),
              Some("soletrader"),
              "A Alice",
              details,
              Seq(issue)
            )
          )
        )

      when(repository.create(any[String], any[String], any[String], any[JsObject]))
        .thenReturn(Future.successful(draftId))

      service.create(userId, request).futureValue mustBe draftId

      verify(repository).create(any[String], any[String], any[String], any[JsObject])
    }
  }

  "get" - {

    "return the draft" in {
      val expected = draft()

      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(expected))))

      service.get(draftId, userId, instanceId).futureValue mustBe expected
    }

    "fail when the draft does not exist" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(None))

      service.get(draftId, userId, instanceId).failed.futureValue.getMessage mustBe
        s"Final Validation draft $draftId not found"
    }
  }

  "updateCorrection" - {

    "update proposed value and change target" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft()))))
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))

      val result =
        service
          .updateCorrection(
            draftId,
            userId,
            instanceId,
            FinalValidationCorrectionRequest(
              10903L,
              "utr",
              FinalValidationSubcontractorPatch(utr = Some("2234567890"))
            )
          )
          .futureValue
          .subcontractors
          .head

      result.proposed.utr mustBe Some("2234567890")
      result.changedTargets must contain("utr")
    }

    "fail when the subcontractor is complete" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft(readiness = "Complete")))))

      service
        .updateCorrection(
          draftId,
          userId,
          instanceId,
          FinalValidationCorrectionRequest(
            10903L,
            "utr",
            FinalValidationSubcontractorPatch(utr = Some("2234567890"))
          )
        )
        .failed
        .futureValue
        .getMessage mustBe "Cannot update completed Final Validation subcontractor 10903"
    }

    "fail on version mismatch" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft()))))
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(false))

      service
        .updateCorrection(
          draftId,
          userId,
          instanceId,
          FinalValidationCorrectionRequest(
            10903L,
            "utr",
            FinalValidationSubcontractorPatch(utr = Some("2234567890"))
          )
        )
        .failed
        .futureValue
        .getMessage mustBe s"Failed to update Final Validation draft $draftId due to version mismatch"
    }
  }

  "updateReadiness" - {

    "mark complete when there are no issues" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft()))))
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))

      val result =
        service
          .updateReadiness(
            draftId,
            userId,
            instanceId,
            UpdateFinalValidationReadinessRequest(10903L, Seq.empty)
          )
          .futureValue
          .subcontractors
          .head

      result.issues mustBe empty
      result.readiness mustBe FinalValidationReadiness.Complete
    }

    "remain incomplete when issues exist" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft()))))
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))

      val result =
        service
          .updateReadiness(
            draftId,
            userId,
            instanceId,
            UpdateFinalValidationReadinessRequest(10903L, Seq(issue))
          )
          .futureValue
          .subcontractors
          .head

      result.issues mustBe Seq(issue)
      result.readiness mustBe FinalValidationReadiness.Incomplete
    }
  }

  "commit" - {

    "fail when the draft is not ready" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft()))))

      val error = service.commit(draftId, userId, instanceId).failed.futureValue

      error.isInstanceOf[FinalValidationDraftNotReadyException] mustBe true
      error.getMessage mustBe s"Final Validation draft $draftId is not ready to commit"
      verifyNoInteractions(formpProxyConnector)
    }

    "mark a subcontractor NotRequired when there are no changes" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(Future.successful(Some(stored(draft(readiness = "Complete")))))
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))

      service.commit(draftId, userId, instanceId).futureValue

      val captor = ArgumentCaptor.forClass(classOf[JsObject])
      verify(repository).replace(any[FinalValidationDraftData], captor.capture())

      (captor.getValue \ "subcontractors" \ 0 \ "commitStatus").as[String] mustBe "NotRequired"
      verifyNoInteractions(formpProxyConnector)
    }

    "skip a subcontractor that is already committed" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(
          Future.successful(
            Some(
              stored(
                draft(
                  readiness = "Complete",
                  changedTargets = Set("utr"),
                  commitStatus = "Committed"
                )
              )
            )
          )
        )

      service.commit(draftId, userId, instanceId).futureValue

      verifyNoInteractions(formpProxyConnector)
    }

    "call FormP and mark the subcontractor Committed" in {
      when(repository.get(draftId, userId, instanceId))
        .thenReturn(
          Future.successful(
            Some(
              stored(
                draft(
                  readiness = "Complete",
                  changedTargets = Set("utr"),
                  proposedUtr = "2234567890"
                )
              )
            )
          )
        )
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))
      when(
        formpProxyConnector.updateSubcontractorForFinalValidation(
          any[FinalValidationUpdateSubcontractorRequest]
        )(any[HeaderCarrier])
      ).thenReturn(Future.unit)

      service.commit(draftId, userId, instanceId).futureValue

      val requestCaptor = ArgumentCaptor.forClass(classOf[FinalValidationUpdateSubcontractorRequest])
      verify(formpProxyConnector).updateSubcontractorForFinalValidation(
        requestCaptor.capture()
      )(any[HeaderCarrier])

      requestCaptor.getValue.instanceId mustBe instanceId
      requestCaptor.getValue.subcontractorId mustBe 10903L
      requestCaptor.getValue.subbieResourceRef mustBe 7L
      requestCaptor.getValue.changeTargets mustBe Set("utr")
      requestCaptor.getValue.patch.utr mustBe Some("2234567890")

      val dataCaptor = ArgumentCaptor.forClass(classOf[JsObject])
      verify(repository).replace(any[FinalValidationDraftData], dataCaptor.capture())

      (dataCaptor.getValue \ "subcontractors" \ 0 \ "commitStatus").as[String] mustBe "Committed"
    }

    "mark the subcontractor Failed and propagate a FormP failure" in {
      val expected = new RuntimeException("FormP failed")

      when(repository.get(draftId, userId, instanceId))
        .thenReturn(
          Future.successful(
            Some(
              stored(
                draft(
                  readiness = "Complete",
                  changedTargets = Set("utr")
                )
              )
            )
          )
        )
      when(repository.replace(any[FinalValidationDraftData], any[JsObject]))
        .thenReturn(Future.successful(true))
      when(
        formpProxyConnector.updateSubcontractorForFinalValidation(
          any[FinalValidationUpdateSubcontractorRequest]
        )(any[HeaderCarrier])
      ).thenReturn(Future.failed(expected))

      service.commit(draftId, userId, instanceId).failed.futureValue mustBe expected

      val captor = ArgumentCaptor.forClass(classOf[JsObject])
      verify(repository).replace(any[FinalValidationDraftData], captor.capture())

      (captor.getValue \ "subcontractors" \ 0 \ "commitStatus").as[String] mustBe "Failed"
    }
  }
}
