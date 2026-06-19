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
import uk.gov.hmrc.constructionindustryscheme.repositories.StoredRequestedVerification

import java.time.LocalDateTime

object VerificationSubmissionContextBuilder {

  private def parseLong(value: String, fieldName: String): Either[String, Long] =
    try {
      Right(value.trim.toLong)
    } catch {
      case _: NumberFormatException =>
        Left(s"Invalid long value for $fieldName: '$value'")
    }

  private def sequence[A](values: Seq[Either[String, A]]): Either[String, Seq[A]] = {
    val errors = values.collect { case Left(err) => err }
    if errors.nonEmpty then Left(errors.mkString("; "))
    else Right(values.collect { case Right(v) => v })
  }

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
    } yield {
      VerificationSubmissionContext(
        hmrcMarkGenerated = hmrcMarkGenerated,
        submissionRequestDate = submissionRequestDate,
        verificationBatchResourceRef = batchResourceRef,
        actionIndicators = actionIndicators,
        requestedVerifications = requestedVerifications
      )
    }

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
      request.subcontractors.flatMap { subcontractor =>
        val verificationOpt =
          subcontractor.subbieResourceRef
            .map(_.toString)
            .flatMap(verificationByResourceRef.get)

        verificationOpt.map { verification =>
          for {
            verificationResourceRef <- parseLong(verification.verificationResourceRef, "verificationResourceRef")
            actionIndicator <- actionByVerificationRef.get(verificationResourceRef).toRight(s"No action indicator found for verificationResourceRef: $verificationResourceRef")
          } yield {
            StoredRequestedVerification(
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
      }
    sequence(requested)
  }
}
