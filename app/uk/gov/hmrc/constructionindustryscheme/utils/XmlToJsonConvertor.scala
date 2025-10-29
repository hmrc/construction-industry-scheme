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
  private def xmlToJson(node: Node): JsValue = {
      val attributes: Map[String, JsValue] = node.attributes.asAttrMap.map {
        case (k, v) => k -> JsString(v)
      }
      val children = node.child.collect {
        case e: Elem => e.label -> xmlToJson(e)
        case t if t.isInstanceOf[Text] && t.text.trim.nonEmpty => "#text" -> JsString(t.text.trim)
      }
      val groupedChildren: Map[String, JsValue] = children
        .groupBy(_._1)
        .map {
          case (k, v) =>
            if (v.size == 1) k -> v.head._2
            else k -> JsArray(v.map(_._2).toSeq)
        }
      val combined = attributes ++ groupedChildren
      combined.toList match {
        case List(("#text", JsString(value))) => JsString(value)
        case _ if combined.contains("#text") && attributes.nonEmpty =>
          JsObject((attributes + (node.label -> combined("#text"))).toSeq)
        case _ => JsObject(combined.filterNot(_._1 == "#text").toSeq)
      }
  }
  def convertXmlToJson(xmlString: String): XmlConversionResult = {
    val conversionAttempt = for {
      cleanedXmlString <- Try(xmlString.replaceAll("xmlns(:\\w+)?=\"[^\"]*\"", ""))
      xml <- Try(XML.loadString(cleanedXmlString))
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
