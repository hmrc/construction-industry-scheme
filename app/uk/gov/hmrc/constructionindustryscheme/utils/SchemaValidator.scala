/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryscheme.utils

import org.xml.sax.{ErrorHandler, SAXParseException}
import play.api.Logging

import java.io.StringReader
import javax.inject.Singleton
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.Schema

/** Logs schema validation problems without exposing personal data.
  *
  * Xerces messages for value errors embed the offending value (e.g. "Value 'AB123456C' is not facet-valid ..."), which
  * in a CIS return could be a name, UTR, NINO or bank detail. So the raw message is never logged. Only the `cvc-...`
  * error code, the position and the element/attribute names (which come from the schema, not user data) are recorded.
  */
object ValidationHandler {

  private val ErrorCode             = """^([A-Za-z0-9._-]{1,60}):""".r
  private val XmlName               = "[A-Za-z_][A-Za-z0-9_.-]{0,63}"
  private val ElementName           = s"""(?i)element '($XmlName)'""".r
  private val AttributeName         = s"""(?i)attribute '($XmlName)'""".r
  // Only these message families name the element/attribute outside of a quoted user value.
  private val CodesThatNameElements = Seq("cvc-complex-type", "cvc-type", "cvc-attribute", "cvc-elt")

  def describe(e: SAXParseException): String = {
    val message = Option(e.getMessage).getOrElse("")
    val code    = ErrorCode.findFirstMatchIn(message).map(_.group(1)).getOrElse("unknown")

    // Use the last match: in "The value '<user value>' of element 'E' is not valid" the real name comes last
    val (element, attribute) =
      if (CodesThatNameElements.exists(code.startsWith))
        (
          ElementName.findAllMatchIn(message).map(_.group(1)).toSeq.lastOption,
          AttributeName.findAllMatchIn(message).map(_.group(1)).toSeq.lastOption
        )
      else (None, None)

    Seq(
      Some(s"code=$code"),
      element.map(n => s"element=$n"),
      attribute.map(n => s"attribute=$n"),
      Some(s"line=${e.getLineNumber}"),
      Some(s"col=${e.getColumnNumber}")
    ).flatten.mkString(" ")
  }
}

class ValidationHandler extends ErrorHandler with Logging {

  var error = false

  override def warning(e: SAXParseException): Unit = {
    logger.warn(s"[ValidationHandler] SAX Warning ${ValidationHandler.describe(e)}")
    error = true
  }

  override def error(e: SAXParseException): Unit = {
    logger.error(s"[ValidationHandler] SAX Error ${ValidationHandler.describe(e)}")
    error = true
  }

  override def fatalError(e: SAXParseException): Unit = {
    logger.error(s"[ValidationHandler] SAX Fatal Error ${ValidationHandler.describe(e)}")
    error = true
  }
}

@Singleton
class SchemaValidator extends Logging {
  def validate(xml: String, schema: Schema): Boolean = {
    val validator = schema.newValidator()
    val handler   = new ValidationHandler
    validator.setErrorHandler(handler)
    try {
      validator.validate(new StreamSource(new StringReader(xml)))
      !handler.error
    } catch {
      case e: SAXParseException =>
        logger.error(s"[SchemaValidator] Schema validation exception: ${ValidationHandler.describe(e)}")
        false
      case e: Exception         =>
        // the message and stack trace are not logged as they can echo parts of the submitted XML
        logger.error(s"[SchemaValidator] Schema validation exception: ${e.getClass.getName}")
        false
    }
  }
}
