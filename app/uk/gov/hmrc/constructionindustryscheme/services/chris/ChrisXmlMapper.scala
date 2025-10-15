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

import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, FATAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SubmissionResult, SubmissionStatus}

import scala.util.Try
import scala.xml.*

object ChrisXmlMapper {

  private def textRequired(scope: NodeSeq, tagName: String, fieldName: String): Either[String, String] = {
    val value = (scope \\ tagName).text.trim
    if (value.nonEmpty) Right(value) else Left(s"Missing mandatory field: $fieldName")
  }

  private def intAttrRequired(scope: NodeSeq, tagName: String, attrName: String): Either[String, Int] = {
    val valueOpt = (scope \\ tagName).headOption
      .flatMap(_.attribute(attrName).flatMap(_.headOption))
      .map(_.text.trim)

    valueOpt match {
      case Some(s) if s.nonEmpty => Try(s.toInt).toOption.toRight(s"Invalid integer for $tagName@$attrName: '$s'")
      case _ => Left(s"Missing mandatory attribute: $tagName@$attrName")
    }
  }

  private def parseError(qualifier: String, doc: Elem): Either[String, Option[GovTalkError]] =
    if (qualifier.equalsIgnoreCase("error")) {
      val e = doc \\ "GovTalkErrors" \\ "Error"
      for {
        errorNumber <- textRequired(e, "Number", "GovTalkErrors/Error/Number")
        errorType <- textRequired(e, "Type", "GovTalkErrors/Error/Type")
        errorText <- textRequired(e, "Text", "GovTalkErrors/Error/Text")
      } yield Some(GovTalkError(errorNumber = errorNumber, errorType = errorType, errorText = errorText))
    } else Right(None)

  def parse(xml: String): Either[String, SubmissionResult] = {
    val doc = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier <- textRequired(messageDetails, "Qualifier", "Qualifier")
      function <- textRequired(messageDetails, "Function", "Function")
      className <- textRequired(messageDetails, "Class", "Class")
      correlationId <- textRequired(messageDetails, "CorrelationID", "CorrelationID")
      gatewayTimestamp <- textRequired(messageDetails, "GatewayTimestamp", "GatewayTimestamp")
      pollInt <- intAttrRequired(messageDetails, "ResponseEndPoint", "PollInterval")
      endpointUrl <- textRequired(messageDetails, "ResponseEndPoint", "ResponseEndPoint")
      errorOpt <- parseError(qualifier, doc)
    } yield {
      val meta = GovTalkMeta(
        qualifier = qualifier,
        function = function,
        className = className,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestamp,
        responseEndPoint = ResponseEndPoint(endpointUrl, pollInt),
        error = errorOpt
      )

      val status: SubmissionStatus =
        qualifier.toLowerCase match {
          case "acknowledgement" => ACCEPTED
          case "error" => FATAL_ERROR
          case _ => FATAL_ERROR
        }

      SubmissionResult(status, xml, meta)
    }
  }
}
