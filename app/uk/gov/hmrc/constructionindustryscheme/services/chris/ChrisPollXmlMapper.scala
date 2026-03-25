/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryscheme.services.chris

import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisSubmissionXmlMapper.{intAttrOptional, textOptional}

import java.time.{Instant, LocalDateTime, ZoneOffset}
import scala.util.Try
import scala.xml.*

object ChrisPollXmlMapper extends ChrisXmlMapper {

  def parse(xml: String, now: Instant = Instant.now()): Either[String, ChrisPollResponse] = {
    val doc            = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier                     <- textRequired(messageDetails, "Qualifier", "Qualifier")
      correlationId                 <- textRequired(messageDetails, "CorrelationID", "CorrelationID")
      endpointUrlOpt: Option[String] = textOptional(messageDetails, "ResponseEndPoint")
      pollIntervalOpt: Option[Int]   = intAttrOptional(messageDetails, "ResponseEndPoint", "PollInterval")
      lastMessageDateOpt            <- gatewayTimeStampOrNow(messageDetails, now)
      errOpt                        <- parseError(qualifier, doc)
      irMark                         = textOptional(
                                         doc \\ "Body" \ "SuccessResponse" \ "IRmarkReceipt" \ "Signature" \ "SignedInfo" \ "Reference",
                                         "DigestValue"
                                       )
    } yield {
      val status: SubmissionStatus = derivePollStatus(qualifier, errOpt, doc)
      ChrisPollResponse(
        status,
        correlationId,
        endpointUrlOpt,
        pollIntervalOpt,
        errOpt.map(Json.toJson(_)),
        irMark,
        lastMessageDateOpt
      )
    }
  }

  /** Stage 2 (polling) status mapping. */
  private def derivePollStatus(
    qualifier: String,
    errOpt: Option[GovTalkError],
    doc: Elem
  ): SubmissionStatus =
    qualifier.toLowerCase match {
      case "acknowledgement" => ACCEPTED
      case "response"        => SUBMITTED
      case "error"           =>
        // Special case: IRMark mismatch ⇒ SUBMITTED_NO_RECEIPT
        if (isIrmarkMismatch(doc)) {
          SUBMITTED_NO_RECEIPT
        } else {
          errOpt match {
            // 3001 + business => departmental error
            case Some(err)
                if err.errorNumber == "3001" &&
                  err.errorType.equalsIgnoreCase("business") =>
              DEPARTMENTAL_ERROR

            // 3000 + fatal => fatal error
            case Some(err)
                if err.errorNumber == "3000" &&
                  err.errorType.equalsIgnoreCase("fatal") =>
              FATAL_ERROR

            case _ => FATAL_ERROR
          }
        }
      case _                 => FATAL_ERROR
    }

  /** Detects IRMark mismatch error inside the <Body> ErrorResponse. */
  private def isIrmarkMismatch(doc: Elem): Boolean = {
    val bodyErrors = (doc \\ "Body") \\ "Error"

    bodyErrors.exists { e =>
      val number    = (e \ "Number").text.trim
      val errorType = (e \ "Type").text.trim
      val text      = (e \ "Text").text.trim.toLowerCase

      number == "2021" &&
      errorType.equalsIgnoreCase("business") &&
      text.contains("irmark") &&
      text.contains("incorrect")
    }
  }

  private def gatewayTimeStampOrNow(messageDetails: NodeSeq, now: Instant): Either[String, Option[String]] =
    textOptional(messageDetails, "GatewayTimestamp") match {
      case Some(raw) => normaliseGatewayTimestamp(raw).map(ts => Some(ts): Option[String])
      case None      => Right(Some(now.toString): Option[String])
    }

  private def normaliseGatewayTimestamp(raw: String): Either[String, String] = {
    val instant =
      Try(Instant.parse(raw))
        .orElse(Try(LocalDateTime.parse(raw).toInstant(ZoneOffset.UTC)))
        .toEither

    instant match {
      case Right(value) => Right(value.toString)
      case Left(err)    => Left(s"Failed to parse GatewayTimestamp '$raw': ${err.getMessage}")
    }
  }
}
