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

package uk.gov.hmrc.constructionindustryscheme.services

import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.FinalValidationSubcontractorPatch
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.*
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationCommitStatus.*
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationReadiness.{Complete, Incomplete}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateFinalValidationDraftRequest, FinalValidationCorrectionRequest, FinalValidationUpdateSubcontractorRequest, UpdateFinalValidationReadinessRequest}
import uk.gov.hmrc.constructionindustryscheme.repositories.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class FinalValidationDraftService @Inject()(
  repository: FinalValidationDraftRepository,
  formpProxyConnector: FormpProxyConnector
)(using ec: ExecutionContext) {

  def create(
    userId: String,
    request: CreateFinalValidationDraftRequest
  ): Future[String] = {

    if (request.subcontractors.isEmpty) {
      Future.failed(
        new IllegalArgumentException(
          "Cannot create Final Validation draft without subcontractors"
        )
      )
    } else {
      request.subcontractors.find(_.issues.isEmpty) match {

        case Some(subcontractor) =>
          Future.failed(
            new IllegalArgumentException(
              s"Cannot create Final Validation draft for subcontractor " +
                s"${subcontractor.subcontractorId} without validation issues"
            )
          )

        case None =>
          val subcontractors =
            request.subcontractors.map { subcontractor =>
              FinalValidationDraftSubcontractor(
                subcontractorId = subcontractor.subcontractorId,
                subbieResourceRef = subcontractor.subbieResourceRef,
                baseVersion = subcontractor.baseVersion,
                subcontractorType = subcontractor.subcontractorType,
                displayName = subcontractor.displayName,
                base = subcontractor.details,
                proposed = subcontractor.details,
                changedTargets = Set.empty,
                issues = subcontractor.issues,
                readiness = Incomplete,
                commitStatus = Pending
              )
            }

          val draft =
            FinalValidationDraft(
              subcontractors = subcontractors
            )

          repository.create(
            userId = userId,
            instanceId = request.instanceId,
            context = request.context,
            data = Json.toJsObject(draft)
          )
      }
    }
  }

  def get(
    draftId: String,
    userId: String,
    instanceId: String
  ): Future[FinalValidationDraft] =
    for {
      stored <- getStored(draftId, userId, instanceId)
      draft <- decode(stored)
    } yield draft

  def updateCorrection(
    draftId: String,
    userId: String,
    instanceId: String,
    request: FinalValidationCorrectionRequest
  ): Future[FinalValidationDraft] =
    updateDraft(
      draftId,
      userId,
      instanceId
    ) { draft =>
      updateSubcontractor(
        draft,
        request.subcontractorId
      ) { subcontractor =>

        if (subcontractor.readiness == Complete) {
          throw new IllegalStateException(
            s"Cannot update completed Final Validation subcontractor ${request.subcontractorId}"
          )
        }

        subcontractor.copy(
          proposed = applyCorrection(
            subcontractor.proposed,
            request.changeTarget,
            request.patch
          ),
          changedTargets = subcontractor.changedTargets + request.changeTarget,
          commitStatus = Pending
        )
      }
    }

  def updateReadiness(
    draftId: String,
    userId: String,
    instanceId: String,
    request: UpdateFinalValidationReadinessRequest
  ): Future[FinalValidationDraft] =
    updateDraft(
      draftId,
      userId,
      instanceId
    ) { draft =>

      val readiness =
        if (request.issues.isEmpty) {
          Complete
        } else {
          Incomplete
        }

      updateSubcontractor(
        draft,
        request.subcontractorId
      ) { subcontractor =>
        subcontractor.copy(
          issues = request.issues,
          readiness = readiness
        )
      }
    }

  def commit(
    draftId: String,
    userId: String,
    instanceId: String
  )(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      draft <- get(draftId, userId, instanceId)
      _ <- if (draft.allComplete) {
        Future.unit
      } else {
        Future.failed(
          new FinalValidationDraftNotReadyException(
            s"Final Validation draft $draftId is not ready to commit"
          )
        )
      }
      _ <- commitSubcontractors(
        draftId,
        userId,
        instanceId,
        draft.subcontractors.map(_.subcontractorId)
      )
    } yield ()

  private def commitSubcontractors(
    draftId: String,
    userId: String,
    instanceId: String,
    subcontractorIds: Seq[Long]
  )(implicit hc: HeaderCarrier): Future[Unit] =
    subcontractorIds.foldLeft(Future.unit) { (result, subcontractorId) =>
      result.flatMap { _ =>
        commitSubcontractor(
          draftId,
          userId,
          instanceId,
          subcontractorId
        )
      }
    }

  private def commitSubcontractor(
    draftId: String,
    userId: String,
    instanceId: String,
    subcontractorId: Long
  )(implicit hc: HeaderCarrier): Future[Unit] =
    get(draftId, userId, instanceId).flatMap { draft =>

      draft.subcontractor(subcontractorId) match {

        case None =>
          Future.failed(
            new NoSuchElementException(
              s"Final Validation subcontractor $subcontractorId not found in draft $draftId"
            )
          )

        case Some(subcontractor) =>
          subcontractor.commitStatus match {

            case Committed | NotRequired =>
              Future.unit

            case _ if subcontractor.changedTargets.isEmpty =>
              updateCommitStatus(
                draftId,
                userId,
                instanceId,
                subcontractorId,
                NotRequired
              )

            case _ =>
              val request =
                buildFinalValidationUpdateRequest(
                  instanceId,
                  subcontractor
                )

              formpProxyConnector
                .updateSubcontractorForFinalValidation(request)
                .flatMap { _ =>
                  updateCommitStatus(
                    draftId,
                    userId,
                    instanceId,
                    subcontractorId,
                    Committed
                  )
                }
                .recoverWith { case error =>
                  updateCommitStatus(
                    draftId,
                    userId,
                    instanceId,
                    subcontractorId,
                    Failed
                  ).flatMap { _ =>
                    Future.failed(error)
                  }
                }
          }
      }
    }

  private def updateCommitStatus(
    draftId: String,
    userId: String,
    instanceId: String,
    subcontractorId: Long,
    commitStatus: FinalValidationCommitStatus
  ): Future[Unit] =
    updateDraft(
      draftId,
      userId,
      instanceId
    ) { draft =>
      updateSubcontractor(
        draft,
        subcontractorId
      ) { subcontractor =>
        subcontractor.copy(
          commitStatus = commitStatus
        )
      }
    }.map(_ => ())

  private def buildFinalValidationUpdateRequest(
    instanceId: String,
    subcontractor: FinalValidationDraftSubcontractor
  ): FinalValidationUpdateSubcontractorRequest =
    FinalValidationUpdateSubcontractorRequest(
      instanceId = instanceId,
      subcontractorId = subcontractor.subcontractorId,
      subbieResourceRef = subcontractor.subbieResourceRef,
      changeTargets = subcontractor.changedTargets,
      patch = toPatch(subcontractor.proposed)
    )

  private def toPatch(
    details: FinalValidationSubcontractorDetails
  ): FinalValidationSubcontractorPatch =
    FinalValidationSubcontractorPatch(
      utr = details.utr,
      partnerUtr = details.partnerUtr,
      crn = details.crn,
      firstName = details.firstName,
      secondName = details.secondName,
      surname = details.surname,
      partnershipTradingName = details.partnershipTradingName,
      tradingName = details.tradingName,
      nino = details.nino,
      worksReferenceNumber = details.worksReferenceNumber,
      addressLine1 = details.addressLine1,
      addressLine2 = details.addressLine2,
      addressLine3 = details.addressLine3,
      addressLine4 = details.addressLine4,
      country = details.country,
      postcode = details.postcode,
      emailAddress = details.emailAddress,
      phoneNumber = details.phoneNumber,
      mobilePhoneNumber = details.mobilePhoneNumber
    )

  private def applyCorrection(
    existing: FinalValidationSubcontractorDetails,
    changeTarget: String,
    patch: FinalValidationSubcontractorPatch
  ): FinalValidationSubcontractorDetails =
    changeTarget match {

      case "subcontractorName" =>
        existing.copy(
          firstName = patch.firstName,
          secondName = patch.secondName,
          surname = patch.surname
        )

      case "tradingName" =>
        existing.copy(
          tradingName = patch.tradingName
        )

      case "partnershipTradingName" =>
        existing.copy(
          partnershipTradingName = patch.partnershipTradingName
        )

      case "utrYesNo" | "utr" =>
        existing.copy(
          utr = patch.utr
        )

      case "partnerUtrYesNo" | "partnerUtr" =>
        existing.copy(
          partnerUtr = patch.partnerUtr
        )

      case "ninoYesNo" | "nino" =>
        existing.copy(
          nino = patch.nino
        )

      case "crnYesNo" | "crn" =>
        existing.copy(
          crn = patch.crn
        )

      case "worksReferenceNumberYesNo" | "worksReferenceNumber" =>
        existing.copy(
          worksReferenceNumber = patch.worksReferenceNumber
        )

      case "addressYesNo" | "address" =>
        existing.copy(
          addressLine1 = patch.addressLine1,
          addressLine2 = patch.addressLine2,
          addressLine3 = patch.addressLine3,
          addressLine4 = patch.addressLine4,
          country = patch.country,
          postcode = patch.postcode
        )

      case "contactDetailsYesNo" =>
        existing.copy(
          emailAddress = patch.emailAddress,
          phoneNumber = patch.phoneNumber,
          mobilePhoneNumber = patch.mobilePhoneNumber
        )

      case "emailAddress" =>
        existing.copy(
          emailAddress = patch.emailAddress
        )

      case "phoneNumber" =>
        existing.copy(
          phoneNumber = patch.phoneNumber
        )

      case "mobilePhoneNumber" =>
        existing.copy(
          mobilePhoneNumber = patch.mobilePhoneNumber
        )

      case other =>
        throw new IllegalArgumentException(
          s"Unsupported Final Validation change target: $other"
        )
    }

  private def updateSubcontractor(
     draft: FinalValidationDraft,
     subcontractorId: Long
   )(update: FinalValidationDraftSubcontractor => FinalValidationDraftSubcontractor): FinalValidationDraft = {

    val index =
      draft.subcontractors.indexWhere(
        _.subcontractorId == subcontractorId
      )

    if (index < 0) {
      throw new NoSuchElementException(
        s"Final Validation subcontractor $subcontractorId not found"
      )
    }

    val existing =
      draft.subcontractors(index)

    draft.copy(
      subcontractors =
        draft.subcontractors.updated(
          index,
          update(existing)
        )
    )
  }

  private def updateDraft(
    draftId: String,
    userId: String,
    instanceId: String
  )(update: FinalValidationDraft => FinalValidationDraft): Future[FinalValidationDraft] =
    for {
      stored <- getStored(draftId, userId, instanceId)
      current <- decode(stored)
      updated <- Future.fromTry(Try(update(current)))
      replaced <- repository.replace(
        stored,
        Json.toJsObject(updated)
      )
      _ <- if (replaced) {
        Future.unit
      } else {
        Future.failed(
          new IllegalStateException(
            s"Failed to update Final Validation draft $draftId due to version mismatch"
          )
        )
      }
    } yield updated

  private def getStored(
    draftId: String,
    userId: String,
    instanceId: String
  ): Future[FinalValidationDraftData] =
    repository.get(draftId, userId, instanceId).flatMap {
      case Some(data) =>
        Future.successful(data)

      case None =>
        Future.failed(
          new NoSuchElementException(
            s"Final Validation draft $draftId not found"
          )
        )
    }

  private def decode(
    stored: FinalValidationDraftData
  ): Future[FinalValidationDraft] =
    stored.data.validate[FinalValidationDraft] match {
      case JsSuccess(draft, _) =>
        Future.successful(draft)

      case JsError(errors) =>
        Future.failed(
          new IllegalStateException(
            s"Failed to decode Final Validation draft ${stored.id}: $errors"
          )
        )
    }
  
}
