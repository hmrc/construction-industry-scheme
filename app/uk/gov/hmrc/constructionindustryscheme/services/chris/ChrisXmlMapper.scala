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

import uk.gov.hmrc.constructionindustryscheme.models.GovTalkError

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

  protected def parseError(qualifier: String, doc: Elem): Either[String, Option[GovTalkError]] =
    if (qualifier.equalsIgnoreCase("error")) {
      val e = doc \\ "GovTalkErrors" \\ "Error"
      for {
        errorNumber <- textRequired(e, "Number", "GovTalkErrors/Error/Number")
        errorType   <- textRequired(e, "Type", "GovTalkErrors/Error/Type")
        errorText   <- textRequired(e, "Text", "GovTalkErrors/Error/Text")
      } yield Some(GovTalkError(errorNumber = errorNumber, errorType = errorType, errorText = errorText))
    } else Right(None)

}
