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

import uk.gov.hmrc.constructionindustryscheme.models.CisResponseSubcontractor
import uk.gov.hmrc.constructionindustryscheme.repositories.{StoredRequestedVerification, StoredVerificationContext}
import uk.gov.hmrc.constructionindustryscheme.models.VerificationResults

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future


@Singleton
class VerificationResultMapper @Inject() () {

  def mapAll(
    chrisResults: Seq[CisResponseSubcontractor],
    context: StoredVerificationContext
  ): Future[Seq[VerificationResults]] = {
    val mapped = chrisResults.map( chris => mapOne(chris, context))
    val errors = mapped.collect { case Left(err) => err }
    
    if (errors.nonEmpty) {
      Future.failed(new RuntimeException(s"Errors occurred during mapping: ${errors.mkString("; ")}"))
    } else {
      Future.successful(mapped.collect { case Right(result) => result })
    }
  }
  
  private def mapOne(
    chris: CisResponseSubcontractor,
    context: StoredVerificationContext
  ): Either[String, VerificationResults] =
    for {
      requested <- findRequestedVerification(chris, context)
      subbieRef <- requested.subbieResourceRef.toRight(
                     s"Missing subbieResourceRef for matched verificationResourceRef: ${requested.verificationResourceRef}"
                   )
    } yield {
      VerificationResults (
        subbieResourceRef = subbieRef,
        matched = chris.matched,
        verified = deriveVerified(chris.matched, Some(requested.actionIndicator)),
        verificationNumber = chris.verificationNumber,
        taxTreatment = chris.taxTreatment
      )
    }
    
  private def findRequestedVerification(  
    chris: CisResponseSubcontractor,
    context: StoredVerificationContext
  ): Either[String, StoredRequestedVerification] = {
    val matches =
      context.requestedVerifications.filter { requested =>
        requested.subcontractorType.map(_.trim.toLowerCase) match {
          case Some("soletrader") =>
            same(requested.utr, chris.utr) &&
              same(requested.foreName, chris.foreName) &&
              same(requested.middleName, chris.middleName) &&
              same(requested.surname, chris.surname)

          case Some("company") | Some("trust") =>
            same(requested.utr, chris.utr) &&
              same(requested.tradingName, chris.tradingName)

          case Some("partnership")  => 
            same(requested.partnershipUtr, chris.partnershipUtr) &&
              same(requested.tradingName, chris.tradingName)

          case other =>  
            false
        }
      }
      
    matches.toList match {
      case List(one)  => Right(one)
      case Nil        => Left(s"No matching requested verification found for subcontractor: $chris")
      case _          => Left(s"Multiple matching requested verifications found for subcontractor: $chris")
    }  
  }
  
  private def same(left: Option[String], right: Option[String]): Boolean =
    normalise(left) == normalise(right)
    
  private def normalise(value: Option[String]): Option[String] =
    value.map(_.trim.toUpperCase()).filter(_.nonEmpty) 
    
  private def deriveVerified(
    matched: Option[String],
    actionIndicator: Option[String]
  ): Option[String] =
    (matched.map(_.trim.toUpperCase), actionIndicator.map(_.trim.toUpperCase)) match {
      case (Some("Y"), Some("MATCH"))  => Some("Y")
      case (Some("N"), Some("VERIFY")) => Some("Y")
      case (Some(_), Some(_))          => Some("N")
      case _                           => None
    }
}
