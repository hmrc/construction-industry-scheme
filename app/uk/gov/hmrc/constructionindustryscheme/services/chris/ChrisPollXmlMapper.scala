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
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisSubmissionXmlMapper.{intAttrOptional, textOptional}

import scala.xml.*

object ChrisPollXmlMapper extends ChrisXmlMapper {

  def parse(xml: String): Either[String, ChrisPollResponse] = {
    val doc = XML.loadString(xml)
    val messageDetails = doc \\ "Header" \\ "MessageDetails"
    val bodyErrorResponse = doc \\ "Body" \\ "ErrorResponse" \\ "Error"

    for {
      qualifier <- textRequired(messageDetails, "Qualifier", "Qualifier")
      endpointUrlOpt: Option[String] = textOptional(messageDetails, "ResponseEndPoint")
      pollIntervalOpt: Option[Int] = intAttrOptional(messageDetails, "ResponseEndPoint", "PollInterval")
      errOpt <- parseError(qualifier, doc)
      bodyErrorNumber = textOptional(bodyErrorResponse, "Number")
      bodyErrorType = textOptional(bodyErrorResponse, "Type")
    } yield {
      val status: SubmissionStatus = qualifier.toLowerCase match {
        case "acknowledgement" => ACCEPTED
        case "response" => SUBMITTED
        case "error" =>
          errOpt.map(_.errorType.toLowerCase) match {
            case Some("business") =>
              (bodyErrorType, bodyErrorNumber) match {
                case (Some("business"), Some("2021")) => SUBMITTED_NO_RECEIPT
                case _ => DEPARTMENTAL_ERROR
              }
            case Some("fatal") => FATAL_ERROR
            case _ => FATAL_ERROR
          }
        case _ => FATAL_ERROR
      }

      ChrisPollResponse(status, endpointUrlOpt, pollIntervalOpt)
    }
  }
  
}
