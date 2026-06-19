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

import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisVerificationRequest

object VerificationActionIndicatorBuilder {
  def buildEither(request: ChrisVerificationRequest): Either[String, Seq[VerificationActionIndicator]] = {
    val indicators: Seq[Either[String, VerificationActionIndicator]] =
      request.verifications.map { verification =>
        parseLong(verification.verificationResourceRef, "verificationResourceRef").map { ref =>
          VerificationActionIndicator(
            verificationResourceRef = ref,
            actionIndicator = if (verification.proceedVerification) "verify" else "match"
          )
        }
      }
      
    sequence(indicators)  
  }

  def build(request: ChrisVerificationRequest): Seq[VerificationActionIndicator] =
    buildEither(request) match {
      case Right(indicators) => indicators
      case Left(error)       => throw new IllegalArgumentException(error)
    }
  
  private def parseLong(value: String, fieldName: String): Either[String, Long] =
    try {
      Right(value.trim.toLong)
    } catch {
      case _: NumberFormatException =>
        Left(s"Invalid long value for $fieldName: '$value'")
    }

  private def sequence[A](values: Seq[Either[String, A]]): Either[String, Seq[A]] =
    val errors = values.collect { case Left(err) => err }
    if errors.nonEmpty then Left(errors.mkString("; "))
    else Right(values.collect { case Right(v) => v })
}
