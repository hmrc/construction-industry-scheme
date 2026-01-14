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

package uk.gov.hmrc.constructionindustryscheme.utils

import org.xml.sax.{ErrorHandler, SAXParseException}
import play.api.Logging

import java.io.{File, StringReader}
import java.nio.file.Paths
import play.api.mvc.Results.{InternalServerError, Status}

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{Schema, SchemaFactory, Validator}
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq

class XmlValidator extends Logging {

  def validateAgainstSchema(xml: NodeSeq, xmlSchemaPath: String): Either[(Status, NodeSeq), Unit] = {

    var exceptions = List[String]()
    try {
      val xmlResponseSource: Source = new StreamSource(new StringReader(xml.toString()))
      val xsdPath =
        Try {
          Paths.get(getClass.getResource(s"/resources/$xmlSchemaPath").toURI).toString
        } match {
          case Failure(e) =>
            logger.error(s"Failed to find resource for schema: $xmlSchemaPath")
            throw e
          case Success(value) => value
        }
      val xsdFile = new File(xsdPath)

      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schema: Schema = schemaFactory.newSchema(xsdFile)

      val validator: Validator = schema.newValidator()
      validator.setErrorHandler(new ErrorHandler() {
        @Override
        def warning(exception: SAXParseException): Unit =
          exceptions = exception.getMessage :: exceptions

        @Override
        def fatalError(exception: SAXParseException): Unit =
          exceptions = exception.getMessage :: exceptions

        @Override
        def error(exception: SAXParseException): Unit =
          exceptions = exception.getMessage :: exceptions
      })

      validator.validate(xmlResponseSource)

      if (exceptions.nonEmpty) {
        logger.error(s"Errors validating XML against schema: ${exceptions.mkString(",\n")}")
        Left((InternalServerError, <Message>
          {exceptions.mkString(",\n")}
        </Message>))
      } else Right(())
    } catch {
      case ex: Throwable =>
        logger.error(s"Errors validating XML against schema: ${ex.getMessage}")
        Left((InternalServerError, <Message>
          {ex.getMessage}
        </Message>))
    }
  }

}

