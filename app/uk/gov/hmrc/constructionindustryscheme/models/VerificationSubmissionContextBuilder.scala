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

package uk.gov.hmrc.constructionindustryscheme.models

import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}
import uk.gov.hmrc.constructionindustryscheme.models.response.GetSubmissionWithVerificationBatchResponse
import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredRequestedVerification, StoredVerificationContext}

import java.time.LocalDateTime

object VerificationSubmissionContextBuilder {

  def build(
    request: ChrisVerificationRequest,
    hmrcMarkGenerated: String,
    submissionRequestDate: LocalDateTime
  ): Either[String, VerificationSubmissionContext] =
    for {
      batchResourceRef <- parseLong(
                            request.verificationBatchResourceRef,
                            "verificationBatchResourceRef"
                          )

      actionIndicators <- VerificationActionIndicatorBuilder.buildEither(request)

      requestedVerifications <- buildRequestedVerifications(
                                  request,
                                  actionIndicators
                                )
    } yield VerificationSubmissionContext(
      hmrcMarkGenerated = hmrcMarkGenerated,
      submissionRequestDate = submissionRequestDate,
      verificationBatchResourceRef = batchResourceRef,
      actionIndicators = actionIndicators,
      requestedVerifications = requestedVerifications
    )

  def buildFromFormpSnapshot(
    response: GetSubmissionWithVerificationBatchResponse,
    verificationBatchResourceRef: Long
  ): Either[String, StoredVerificationContext] =
    for {
      submission             <- response.submission.toRight(
                                  s"No submission returned for verificationBatchResourceRef=$verificationBatchResourceRef"
                                )
      hmrcMarkGenerated      <- submission.hmrcMarkGenerated.toRight(s"Missing hmrcMarkGenerated")
      submissionRequestDate  <- submission.submissionRequestDate.toRight(s"Missing submissionRequestDate")
      requestedVerifications <- buildRequestedVerificationsFromFormp(
                                  verifications = response.verifications,
                                  subcontractors = response.subcontractors
                                )
    } yield StoredVerificationContext(
      verificationBatchResourceRef = verificationBatchResourceRef,
      hmrcMarkGenerated = hmrcMarkGenerated,
      submissionRequestDate = submissionRequestDate,
      actionIndicators = requestedVerifications.map { requested =>
        VerificationActionIndicator(
          verificationResourceRef = requested.verificationResourceRef,
          actionIndicator = requested.actionIndicator
        )
      },
      requestedVerifications = requestedVerifications
    )

  private def buildRequestedVerifications(
    request: ChrisVerificationRequest,
    actionIndicators: Seq[VerificationActionIndicator]
  ): Either[String, Seq[StoredRequestedVerification]] = {

    val verificationByResourceRef: Map[String, VerificationDetails] =
      request.verifications
        .map(v => v.verificationResourceRef.trim -> v)
        .toMap

    val actionByVerificationRef: Map[Long, String] =
      actionIndicators
        .map(a => a.verificationResourceRef -> a.actionIndicator)
        .toMap

    val requested: Seq[Either[String, StoredRequestedVerification]] =
      request.subcontractors.map { subcontractor =>
        val verificationOpt =
          subcontractor.subbieResourceRef
            .map(_.toString)
            .flatMap(verificationByResourceRef.get)

        verificationOpt match {
          case None               =>
            Left(
              s"No verification found for subcontractorId: ${subcontractor.subcontractorId} (subbieResourceRef: ${subcontractor.subbieResourceRef})"
            )
          case Some(verification) =>
            for {
              verificationResourceRef <- parseLong(verification.verificationResourceRef, "verificationResourceRef")
              actionIndicator         <-
                actionByVerificationRef
                  .get(verificationResourceRef)
                  .toRight(s"No action indicator found for verificationResourceRef: $verificationResourceRef")
            } yield StoredRequestedVerification(
              verificationResourceRef = verificationResourceRef,
              subcontractorId = subcontractor.subcontractorId,
              subbieResourceRef = subcontractor.subbieResourceRef,
              subcontractorName = verification.subcontractorName,
              actionIndicator = actionIndicator,
              proceedVerification = verification.proceedVerification,
              foreName = subcontractor.firstName,
              middleName = subcontractor.secondName,
              surname = subcontractor.surname,
              tradingName = subcontractor.tradingName,
              utr = subcontractor.utr,
              nino = subcontractor.nino,
              crn = subcontractor.crn,
              partnershipUtr = subcontractor.partnerUtr,
              subcontractorType = subcontractor.subcontractorType
            )
        }
      }
    sequence(requested)
  }

  private def buildRequestedVerificationsFromFormp(
    verifications: Seq[Verification],
    subcontractors: Seq[Subcontractor]
  ): Either[String, Seq[StoredRequestedVerification]] = {
    val subcontractorsById =
      subcontractors.map(s => s.subcontractorId -> s).toMap

    sequence(
      verifications.map { verification =>
        for {
          subcontractorId         <- verification.subcontractorId.toRight(
                                       s"Missing subcontractorId for verificationId: ${verification.verificationId}"
                                     )
          subcontractor           <- subcontractorsById
                                       .get(subcontractorId)
                                       .toRight(s"No subcontractor found for subcontractorId: $subcontractorId")
          verificationResourceRef <-
            subcontractor.subbieResourceRef.toRight(s"Missing subbieResourceRef for subcontractorId: $subcontractorId")
          actionIndicator         <- verification.actionIndicator
                                       .map(_.trim)
                                       .filter(_.nonEmpty)
                                       .toRight(s"Missing actionIndicator for subcontractorId: $subcontractorId")
          proceedVerification     <-
            parseYesNo(verification.proceed, s"proceedVerification for subcontractorId: $subcontractorId")
        } yield StoredRequestedVerification(
          verificationResourceRef = verificationResourceRef,
          subcontractorId = subcontractorId,
          subbieResourceRef = subcontractor.subbieResourceRef,
          subcontractorName = subcontractor.displayName,
          actionIndicator = actionIndicator,
          proceedVerification = proceedVerification,
          foreName = subcontractor.firstName,
          middleName = subcontractor.secondName,
          surname = subcontractor.surname,
          tradingName = subcontractor.tradingName,
          utr = subcontractor.utr,
          nino = subcontractor.nino,
          crn = subcontractor.crn,
          partnershipUtr = subcontractor.partnerUtr,
          subcontractorType = subcontractor.subcontractorType
        )
      }
    )
  }

  private def parseLong(value: String, fieldName: String): Either[String, Long] =
    try
      Right(value.trim.toLong)
    catch {
      case _: NumberFormatException =>
        Left(s"Invalid long value for $fieldName: '$value'")
    }

  private def sequence[A](values: Seq[Either[String, A]]): Either[String, Seq[A]] = {
    val errors = values.collect { case Left(err) => err }
    if errors.nonEmpty then Left(errors.mkString("; "))
    else Right(values.collect { case Right(v) => v })
  }

  private def parseYesNo(
    value: Option[String],
    fieldName: String
  ): Either[String, Boolean] =
    value.map(_.trim.toUpperCase).filter(_.nonEmpty) match {
      case Some("Y")   => Right(true)
      case Some("N")   => Right(false)
      case Some(other) => Left(s"Invalid value for $fieldName: '$other' (expected 'Y' or 'N')")
      case None        => Left(s"Missing value for $fieldName")
    }
}
