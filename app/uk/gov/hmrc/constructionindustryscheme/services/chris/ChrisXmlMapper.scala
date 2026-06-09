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

import java.time.{Instant, LocalDateTime, ZoneId}
import scala.util.Try
import scala.xml.*

trait ChrisXmlMapper {

  protected def textRequired(scope: NodeSeq, tagName: String, fieldName: String): Either[String, String] = {
    val value = (scope \\ tagName).text.trim
    if (value.nonEmpty) Right(value) else Left(s"Missing mandatory field: $fieldName")
  }

  protected def intAttrOptional(scope: NodeSeq, tagName: String, attrName: String): Option[Int] =
    (scope \\ tagName).headOption
      .flatMap(_.attribute(attrName).flatMap(_.headOption))
      .map(_.text.trim)
      .flatMap(s => scala.util.Try(s.toInt).toOption)

  protected def textOptional(scope: NodeSeq, tagName: String): Option[String] =
    Option((scope \\ tagName).text.trim).filter(_.nonEmpty)

  private def normalizeErrorText(text: String): String =
    text.replaceAll("\\s+", " ").trim

  protected def parseError(qualifier: String, doc: Elem): Either[String, Option[GovTalkError]] =
    if (qualifier.equalsIgnoreCase("error")) {
      val e = doc \\ "GovTalkErrors" \\ "Error"
      for {
        errorNumber <- textRequired(e, "Number", "GovTalkErrors/Error/Number")
        errorType   <- textRequired(e, "Type", "GovTalkErrors/Error/Type")
        errorText   <- textRequired(e, "Text", "GovTalkErrors/Error/Text").map(normalizeErrorText)
      } yield Some(GovTalkError(errorNumber = errorNumber, errorType = errorType, errorText = errorText))
    } else Right(None)

  /** Shared parsing for stage-1 (submit) GovTalk responses. Callers supply the qualifier => status mapping appropriate
    * to their flow.
    */
  protected def parseSubmission(xml: String)(
    deriveStatus: (String, Option[GovTalkError]) => SubmissionStatus
  ): Either[String, SubmissionResult] = {
    val doc            = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier                     <- textRequired(messageDetails, "Qualifier", "Qualifier")
      function                      <- textRequired(messageDetails, "Function", "Function")
      className                     <- textRequired(messageDetails, "Class", "Class")
      correlationId                 <- textRequired(messageDetails, "CorrelationID", "CorrelationID")
      gatewayTimestampOpt            = textOptional(messageDetails, "GatewayTimestamp")
      acceptedTime                   = textOptional(doc \\ "Body" \ "SuccessResponse", "AcceptedTime")
      pollIntervalOpt: Option[Int]   = intAttrOptional(messageDetails, "ResponseEndPoint", "PollInterval")
      endpointUrlOpt: Option[String] = textOptional(messageDetails, "ResponseEndPoint")
      errOpt                        <- parseError(qualifier, doc)
    } yield {
      val status = deriveStatus(qualifier, errOpt)
      val meta   = GovTalkMeta(
        qualifier = qualifier,
        function = function,
        className = className,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestampOpt,
        responseEndPoint = ResponseEndPoint(endpointUrlOpt.getOrElse(""), pollIntervalOpt.getOrElse(0)),
        error = errOpt,
        acceptedTime = acceptedTime
      )

      SubmissionResult(status, xml, meta, Some(GovTalkErrorStatusClassifier.fromXmlOutcome(status, errOpt)))
    }
  }

  /** Shared parsing for stage-2 (poll) GovTalk responses. Callers supply the status mapping appropriate to their flow.
    */
  protected def parsePoll(xml: String, now: Instant)(
    deriveStatus: (String, Option[GovTalkError], Elem) => SubmissionStatus
  ): Either[String, ChrisPollResponse] = {
    val doc            = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier                     <- textRequired(messageDetails, "Qualifier", "Qualifier")
      correlationId                 <- textRequired(messageDetails, "CorrelationID", "CorrelationID")
      endpointUrlOpt: Option[String] = textOptional(messageDetails, "ResponseEndPoint")
      pollIntervalOpt: Option[Int]   = intAttrOptional(messageDetails, "ResponseEndPoint", "PollInterval")
      lastMessageDateOpt            <- gatewayTimeStampOrNow(messageDetails, now)
      acceptedTime                   = textOptional(doc \\ "Body" \ "SuccessResponse", "AcceptedTime")
      errOpt                        <- parseError(qualifier, doc)
      irMark                         = textOptional(
                                         doc \\ "Body" \ "SuccessResponse" \ "IRmarkReceipt" \ "Signature" \ "SignedInfo" \ "Reference",
                                         "DigestValue"
                                       )
    } yield {
      val status = deriveStatus(qualifier, errOpt, doc)
      ChrisPollResponse(
        status,
        correlationId,
        endpointUrlOpt,
        pollIntervalOpt,
        errOpt.map(Json.toJson(_)),
        irMark,
        lastMessageDateOpt,
        acceptedTime,
        Some(GovTalkErrorStatusClassifier.fromXmlOutcome(status, errOpt))
      )
    }
  }

  /** Detects an IRMark mismatch error inside the <Body> ErrorResponse. */
  protected def isIrmarkMismatch(doc: Elem): Boolean = {
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

  private val UkZone = ZoneId.of("Europe/London")

  private def gatewayTimeStampOrNow(messageDetails: NodeSeq, now: Instant): Either[String, Option[String]] =
    textOptional(messageDetails, "GatewayTimestamp") match {
      case Some(raw) => normaliseGatewayTimestamp(raw).map(ts => Some(ts): Option[String])
      case None      => Right(Some(now.toString): Option[String])
    }

  private def normaliseGatewayTimestamp(raw: String): Either[String, String] =
    Try(LocalDateTime.parse(raw).atZone(UkZone).toInstant).toEither match {
      case Right(value) => Right(value.toString)
      case Left(err)    => Left(s"Failed to parse GatewayTimestamp '$raw': ${err.getMessage}")
    }

}
