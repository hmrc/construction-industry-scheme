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

import uk.gov.hmrc.constructionindustryscheme.models.*
import scala.xml.*

object ChrisXmlSubmissionMapper extends ChrisXmlMapper {

  def parse(xml: String): Either[String, SubmissionResult] = {
    val doc = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"

    for {
      qualifier <- textRequired(messageDetails, "Qualifier", "Qualifier")
      function <- textRequired(messageDetails, "Function", "Function")
      className <- textRequired(messageDetails, "Class", "Class")
      correlationId <- textRequired(messageDetails, "CorrelationID", "CorrelationID")
      gatewayTimestampOpt = textOptional(messageDetails, "GatewayTimestamp")
      pollIntervalOpt: Option[Int] = intAttrOptional(messageDetails, "ResponseEndPoint", "PollInterval")
      endpointUrlOpt: Option[String] = textOptional(messageDetails, "ResponseEndPoint")
      errOpt <- parseError(qualifier, doc)
    } yield {
      val status: SubmissionStatus = qualifier.toLowerCase match {
        case "acknowledgement" => ACCEPTED
        case "response" => SUBMITTED
        case "error" =>
          errOpt.map(_.errorType.toLowerCase) match {
            case Some("business") => DEPARTMENTAL_ERROR
            case Some("fatal") => FATAL_ERROR
            case _ => FATAL_ERROR
          }
        case _ => FATAL_ERROR
      }

      val pollInt = pollIntervalOpt.getOrElse(0)
      val epUrl = endpointUrlOpt.getOrElse("")

      val meta = GovTalkMeta(
        qualifier = qualifier,
        function = function,
        className = className,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestampOpt,
        responseEndPoint = ResponseEndPoint(epUrl, pollInt),
        error = errOpt
      )

      SubmissionResult(status, xml, meta)
    }
  }
  
}
