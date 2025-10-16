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

import uk.gov.hmrc.constructionindustryscheme.models.{ACCEPTED, FATAL_ERROR, GovTalkError, GovTalkMeta, ResponseEndPoint, SUBMITTED, SubmissionResult, SubmissionStatus}

import scala.xml.*

object ChrisXmlMapper {

  private def textRequired(scope: NodeSeq, tagName: String, fieldName: String): Either[String, String] = {
    val value = (scope \\ tagName).text.trim
    if (value.nonEmpty) Right(value) else Left(s"Missing mandatory field: $fieldName")
  }

  private def intAttrOptional(scope: NodeSeq, tagName: String, attrName: String): Option[Int] =
    (scope \\ tagName).headOption
      .flatMap(_.attribute(attrName).flatMap(_.headOption))
      .map(_.text.trim)
      .flatMap(s => scala.util.Try(s.toInt).toOption)

  private def textOptional(scope: NodeSeq, tagName: String): Option[String] =
    Option((scope \\ tagName).text.trim).filter(_.nonEmpty)

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
    val md = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier <- textRequired(md, "Qualifier", "Qualifier")
      function <- textRequired(md, "Function", "Function")
      className <- textRequired(md, "Class", "Class")
      correlationId <- textRequired(md, "CorrelationID", "CorrelationID")
      gatewayTimestamp <- textRequired(md, "GatewayTimestamp", "GatewayTimestamp")
      pollIntervalOpt: Option[Int] = intAttrOptional(md, "ResponseEndPoint", "PollInterval")
      endpointUrlOpt: Option[String] = textOptional(md, "ResponseEndPoint")
      errOpt <- parseError(qualifier, doc)
    } yield {
      val status: SubmissionStatus = qualifier.toLowerCase match {
        case "acknowledgement" => ACCEPTED
        case "response" => SUBMITTED
        case "error" => FATAL_ERROR
        case _ => FATAL_ERROR
      }

      val pollInt = pollIntervalOpt.getOrElse(0)
      val epUrl = endpointUrlOpt.getOrElse("")

      val meta = GovTalkMeta(
        qualifier = qualifier,
        function = function,
        className = className,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestamp,
        responseEndPoint = ResponseEndPoint(epUrl, pollInt),
        error = errOpt
      )

      SubmissionResult(status, xml, meta)
    }
  }
}
