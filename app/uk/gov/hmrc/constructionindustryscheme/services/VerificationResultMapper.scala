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

  private case class ChrisIdentity(
    utr: Option[String],
    partnershipUtr: Option[String],
    tradingName: Option[String],
    foreName: Option[String],
    middleName: Option[String],
    surname: Option[String],
    nino: Option[String]
  )

  def mapAll(
    chrisResults: Seq[CisResponseSubcontractor],
    context: StoredVerificationContext,
    verifiedDate: LocalDateTime
  ): Future[Seq[VerificationResult]] = {
    val verificationPrefixes = chrisResults
      .flatMap(_.verificationNumber.map(_.trim).filter(_.nonEmpty))
      .map(_.take(11))
      .distinct

    if (verificationPrefixes.size > 1) {
      logger.warn("Verification numbers in batch do not match")
    }

    if (context.requestedVerifications.isEmpty && chrisResults.nonEmpty) {
      val errors = chrisResults.map { chris =>
        s"No requested verification available for subcontractor: $chris"
      }

      Future.failed(new RuntimeException(s"Errors occurred during mapping: ${errors.mkString("; ")}"))
    } else {
      val matches = matchRequestedVerifications(chrisResults, context.requestedVerifications)

      val mappedFromChris = chrisResults.zipWithIndex.flatMap { case (chris, index) =>
        matches.get(index).map { requested =>
          mapOne(chris, requested, verifiedDate)
        }
      }

      chrisResults.indices.filterNot(matches.contains).foreach { index =>
        logger.warn(s"Skipping duplicate or unmatched CHRIS response at index=$index")
      }

      val matchedResourceRefs = matches.values.map(_.verificationResourceRef).toSet

      val missingResults = context.requestedVerifications
        .filterNot(requested => matchedResourceRefs.contains(requested.verificationResourceRef))
        .map { requested =>
          logger.warn(
            s"No matching CHRIS response found for verification resourceRef=${requested.verificationResourceRef}"
          )
          unmatchedVerificationResult(requested)
        }

      Future.successful(mappedFromChris ++ missingResults)
    }
  }

  private def unmatchedVerificationResult(
    requested: StoredRequestedVerification
  ): VerificationResult =
    VerificationResult(
      resourceRef = requested.verificationResourceRef,
      matchedToRequest = false,
      matched = None,
      verified = None,
      verificationNumber = None,
      taxTreatment = None,
      verifiedDate = None
    )

  private def mapOne(
    chris: CisResponseSubcontractor,
    requested: StoredRequestedVerification,
    verifiedDate: LocalDateTime
  ): VerificationResult = {
    val taxTreatment       = chris.taxTreatment.map(_.trim).filter(_.nonEmpty)
    val verificationNumber = chris.verificationNumber.map(_.trim).filter(_.nonEmpty)
    val matched            = verificationNumber.flatMap(_ => normalise(chris.matched).collect { case "MATCHED" => "Y" })
    val verified           = deriveVerified(chris.matched, Some(requested.actionIndicator), verificationNumber)

    VerificationResult(
      resourceRef = requested.verificationResourceRef,
      matchedToRequest = true,
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
  }

  private def matchRequestedVerifications(
    chrisResults: Seq[CisResponseSubcontractor],
    requestedVerifications: Seq[StoredRequestedVerification]
  ): Map[Int, StoredRequestedVerification] = {
    val unmatchedRequests = scala.collection.mutable.Set.from(requestedVerifications.indices)
    val matchedResponses  = scala.collection.mutable.Map.empty[Int, StoredRequestedVerification]

    val seenIdentities = scala.collection.mutable.Set.empty[ChrisIdentity]

    val duplicateResponseIndices = chrisResults.indices.filter { index =>
      val chris = chrisResults(index)

      val identity = ChrisIdentity(
        utr = normalise(chris.utr),
        partnershipUtr = normalise(chris.partnershipUtr),
        tradingName = normalise(chris.tradingName),
        foreName = normalise(chris.foreName),
        middleName = normalise(chris.middleName),
        surname = normalise(chris.surname),
        nino = normalise(chris.nino)
      )

      val hasUtr = identity.utr.isDefined || identity.partnershipUtr.isDefined

      hasUtr && !seenIdentities.add(identity)
    }.toSet

    def matchPass(matches: (StoredRequestedVerification, CisResponseSubcontractor) => Boolean): Unit =
      chrisResults.indices
        .filterNot(index => matchedResponses.contains(index) || duplicateResponseIndices.contains(index))
        .foreach { responseIndex =>
          unmatchedRequests.toSeq.sorted
            .find { requestIndex =>
              matches(requestedVerifications(requestIndex), chrisResults(responseIndex))
            }
            .foreach { requestIndex =>
              matchedResponses(responseIndex) = requestedVerifications(requestIndex)
              unmatchedRequests -= requestIndex
            }
        }

    // 1. UTR matching
    matchPass { (requested, chris) =>
      hasValue(requested.utr) && (requested.subcontractorType.map(_.trim.toLowerCase) match {
        case Some("partnership") =>
          same(requested.utr, chris.partnershipUtr)

        case Some("soletrader") | Some("company") | Some("trust") =>
          same(requested.utr, chris.utr)

        case _ =>
          false
      })
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
