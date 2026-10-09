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

import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.models.CisResponseSubcontractor
import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredRequestedVerification, StoredVerificationContext}
import uk.gov.hmrc.constructionindustryscheme.models.VerificationResult

import java.time.LocalDateTime
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class VerificationResultMapper @Inject() () extends Logging {

  private case class RequestedVerificationMatch(
    requested: StoredRequestedVerification,
    matched: Boolean
  )

  def mapAll(
    chrisResults: Seq[CisResponseSubcontractor],
    context: StoredVerificationContext,
    verifiedDate: LocalDateTime
  ): Future[Seq[VerificationResult]] = {
    val matches = matchRequestedVerifications(chrisResults, context.requestedVerifications)

    val mapped = chrisResults.zipWithIndex.map { case (chris, index) =>
      matches.get(index) match {
        case Some(requestMatch) => mapOne(chris, requestMatch, verifiedDate)
        case None               => Left(s"No requested verification available for subcontractor: $chris")
      }
    }

    val errors = mapped.collect { case Left(err) => err }

    if (errors.nonEmpty) {
      Future.failed(new RuntimeException(s"Errors occurred during mapping: ${errors.mkString("; ")}"))
    } else {
      Future.successful(mapped.collect { case Right(result) => result })
    }
  }

  private def mapOne(
    chris: CisResponseSubcontractor,
    requestMatch: RequestedVerificationMatch,
    verifiedDate: LocalDateTime
  ): Either[String, VerificationResult] = {
    val requested = requestMatch.requested

    if (!requestMatch.matched) {
      logger.warn(s"No matching requested verification found for subcontractor: $chris")

      Right(
        VerificationResult(
          resourceRef = requested.verificationResourceRef,
          matched = None,
          verified = None,
          verificationNumber = None,
          taxTreatment = None,
          verifiedDate = None
        )
      )
    } else {
      val taxTreatment       = chris.taxTreatment.map(_.trim).filter(_.nonEmpty)
      val verificationNumber = chris.verificationNumber.map(_.trim).filter(_.nonEmpty)
      val matched            = verificationNumber.flatMap(_ => normalise(chris.matched).collect { case "MATCHED" => "Y" })
      val verified           = deriveVerified(chris.matched, Some(requested.actionIndicator), verificationNumber)

      Right(
        VerificationResult(
          resourceRef = requested.verificationResourceRef,
          matched = matched,
          verified = verified,
          verificationNumber = verificationNumber,
          taxTreatment = taxTreatment,
          verifiedDate = verifiedDateFor(
            matched = matched,
            verificationNumber = verificationNumber,
            verifiedDate = verifiedDate,
            taxTreatment = taxTreatment
          )
        )
      )
    }
  }

  private def matchRequestedVerifications(
    chrisResults: Seq[CisResponseSubcontractor],
    requestedVerifications: Seq[StoredRequestedVerification]
  ): Map[Int, RequestedVerificationMatch] = {
    val unmatchedRequests = scala.collection.mutable.Set.from(requestedVerifications.indices)
    val matchedResponses  = scala.collection.mutable.Map.empty[Int, RequestedVerificationMatch]

    def matchPass(
      matches: (StoredRequestedVerification, CisResponseSubcontractor) => Boolean
    ): Unit =
      chrisResults.indices
        .filterNot(matchedResponses.contains)
        .foreach { responseIndex =>
          unmatchedRequests
            .find { requestIndex =>
              matches(requestedVerifications(requestIndex), chrisResults(responseIndex))
            }
            .foreach { requestIndex =>
              matchedResponses(responseIndex) =
                RequestedVerificationMatch(requestedVerifications(requestIndex), matched = true)
              unmatchedRequests -= requestIndex
            }
        }

    // 1. UTR matching
    matchPass { (requested, chris) =>
      requested.subcontractorType.map(_.trim.toLowerCase) match {
        case Some("partnership") =>
          same(requested.utr, chris.partnershipUtr)

        case Some("soletrader") | Some("company") | Some("trust") =>
          same(requested.utr, chris.utr)

        case _ =>
          false
      }
    }

    // 2. Trading name matching
    matchPass { (requested, chris) =>
      hasValue(requested.tradingName) &&
      same(requested.tradingName, chris.tradingName)
    }

    // 3. Sole trader name matching
    matchPass { (requested, chris) =>
      requested.subcontractorType.map(_.trim.toLowerCase).contains("soletrader") &&
      soleTraderNameMatches(requested, chris)
    }

    // Pair remaining responses and requests in their original order.
    val unmatchedResponseIndices = chrisResults.indices.filterNot(matchedResponses.contains)

    unmatchedResponseIndices.zip(unmatchedRequests.toSeq.sorted).foreach { case (responseIndex, requestIndex) =>
      matchedResponses(responseIndex) =
        RequestedVerificationMatch(requestedVerifications(requestIndex), matched = false)
      unmatchedRequests -= requestIndex
    }

    matchedResponses.toMap
  }

  private def soleTraderNameMatches(
    requested: StoredRequestedVerification,
    chris: CisResponseSubcontractor
  ): Boolean =
    (hasValue(requested.foreName) ||
      hasValue(requested.middleName) ||
      hasValue(requested.surname)) &&
      same(requested.foreName, chris.foreName) &&
      same(requested.middleName, chris.middleName) &&
      same(requested.surname, chris.surname)

  private def verifiedDateFor(
    matched: Option[String],
    verificationNumber: Option[String],
    verifiedDate: LocalDateTime,
    taxTreatment: Option[String]
  ): Option[LocalDateTime] =
    if (normalise(matched).contains("Y") && verificationNumber.isDefined && taxTreatment.isDefined) {
      Some(verifiedDate)
    } else {
      None
    }

  private def hasValue(value: Option[String]): Boolean =
    normalise(value).isDefined

  private def same(left: Option[String], right: Option[String]): Boolean =
    normalise(left) == normalise(right)

  private def normalise(value: Option[String]): Option[String] =
    value.map(_.trim.toUpperCase()).filter(_.nonEmpty)

  private def deriveVerified(
    matched: Option[String],
    actionIndicator: Option[String],
    verificationNumber: Option[String]
  ): Option[String] =
    if (verificationNumber.isEmpty) {
      None
    } else {
      (matched.map(_.trim.toUpperCase), actionIndicator.map(_.trim.toUpperCase)) match {
        case (Some("MATCHED"), _)                => Some("Y")
        case (Some("UNMATCHED"), Some("VERIFY")) => Some("Y")
        case _                                   => None
      }
    }
}
