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

package uk.gov.hmrc.constructionindustryscheme.utils

import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.audit.XmlConversionResult

import scala.util.{Failure, Success, Try}
import scala.xml.*

object XmlToJsonConvertor {
  /** Recursively convert an XML Node to JsValue, supporting arrays and attributes */
  private def xmlToJson(node: Node): JsValue = {
      val attributes: Map[String, JsValue] = node.attributes.asAttrMap.map {
        case (k, v) => k -> JsString(v)
      }
      // Collect element and text children
      val children = node.child.collect {
        case e: Elem => e.label -> xmlToJson(e)
        case t if t.isInstanceOf[Text] && t.text.trim.nonEmpty => "#text" -> JsString(t.text.trim)
      }
      // Group repeated children into arrays
      val groupedChildren: Map[String, JsValue] = children
        .groupBy(_._1)
        .map {
          case (k, v) =>
            if (v.size == 1) k -> v.head._2
            else k -> JsArray(v.map(_._2).toSeq)
        }
      val combined = attributes ++ groupedChildren
      combined.toList match {
        // Case 1: Only text
        case List(("#text", JsString(value))) => JsString(value)
        // Case 2: Attributes + text → flatten, omit "#text"
        case _ if combined.contains("#text") && attributes.nonEmpty =>
          JsObject((attributes + (node.label -> combined("#text"))).toSeq)
        // Case 3: Regular object
        case _ => JsObject(combined.filterNot(_._1 == "#text").toSeq)
      }
  }
  /** Converts XML string to pretty JSON string (namespace removed) */
  def convertXmlToJson(xmlString: String): XmlConversionResult = {
    val conversionAttempt = for {
      // Step 1: Clean namespaces
      cleanedXmlString <- Try(xmlString.replaceAll("xmlns(:\\w+)?=\"[^\"]*\"", ""))
      // Step 2: Parse XML safely
      xml <- Try(XML.loadString(cleanedXmlString))
      // Step 3: Convert to JSON
      json <- Try(Json.obj(xml.label -> xmlToJson(xml)))
    } yield json
    conversionAttempt match {
      case Success(json) =>
        XmlConversionResult(success = true, json = Some(json))
      case Failure(ex) =>
        XmlConversionResult(success = false, error = Some(s"Invalid XML input: ${ex.getMessage}"))
    }
  }

}
