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

import javax.xml.parsers.DocumentBuilderFactory
import org.apache.xml.security.c14n.Canonicalizer
import org.apache.xml.security.Init

import java.security.MessageDigest
import java.util.Base64
import org.apache.commons.codec.binary.Base32

import scala.xml.*
import scala.xml.XML.loadString
import org.w3c.dom.Document
import play.api.Logging

object IrMarkProcessor extends Logging {
  Init.init()

  // Removes the single IRmark node within the Elem tree
  private def removeSingleIrmarkNode(elem: Elem): Elem = {
    def rec(ch: Seq[Node], removed: Boolean): Seq[Node] =
      if (removed) ch
      else ch match {
        case Seq() => Seq()
        case (e: Elem) +: tail if e.label == "IRmark" => tail
        case (e: Elem) +: tail =>
          e.copy(child = rec(e.child, removed)).asInstanceOf[Node] +: rec(tail, removed)
        case n +: tail => n +: rec(tail, removed)
      }

    elem.copy(child = rec(elem.child, removed = false))
  }

  // Inherit all namespace declarations
  private def inheritNamespaces(parent: Elem, target: Elem): Elem = {
    target.copy(scope = parent.scope)
  }

  // Convert scala.xml.Elem to DOM (via toString)
  private def elemToDom(elem: Elem): Document = {
    val xmlStr = elem.toString()
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    dbf.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xmlStr.getBytes("UTF-8")))
  }

  def GenerateFullIrMark(xml: String): (String, String) = {
    val normalizedXml = xml.replace("\r\n", "\n").replace("\r", "\n")
    val parsed = loadString(normalizedXml)
    val govMsg = parsed
    val bodyNode = (parsed \\ "Body").head.asInstanceOf[Elem]

    val cleanedBody = removeSingleIrmarkNode(bodyNode)
    val bodyWithNs = inheritNamespaces(govMsg, cleanedBody)
    val domDoc = elemToDom(bodyWithNs)
    val domElem = domDoc.getDocumentElement

    val canon = Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS)
    val baos = new java.io.ByteArrayOutputStream
    canon.canonicalizeSubtree(domElem, baos)
    val canonicalBytes = baos.toByteArray
    val canonicalXml = new String(canonicalBytes, "UTF-8")
    logger.info("GenerateFullIrMark canonicalized XML (no comments):")
    logger.info(canonicalXml)

    val digest = MessageDigest.getInstance("SHA-1").digest(canonicalBytes)
    val b64 = Base64.getEncoder.encodeToString(digest)
    val b32 = new Base32().encodeToString(digest)
    logger.info(s"GenerateFullIrMark base64: $b64")
    logger.info(s"GenerateFullIrMark base32: $b32")
    (b64, b32)
  }

  def UpdatedPayloadWithIrMark(xml: String): (Elem, String, String) = {
    val (b64, b32) = GenerateFullIrMark(xml)
    val inputElem = loadString(xml)
    val updatedXml = replaceSingleIrmarkWithValue(inputElem, b64)
    (updatedXml, b64, b32)
  }

  // Replace the single IRmark node with <IRmark Type="generic">base64</IRmark>
  private def replaceSingleIrmarkWithValue(elem: Elem, irmarkValue: String): Elem = {
    def rep(children: Seq[Node], replaced: Boolean): Seq[Node] = children match {
      case Seq() => Seq()
      case (e: Elem) +: tail if e.label == "IRmark" && !replaced => <IRmark Type="generic">{irmarkValue}</IRmark>.copy(scope = e.scope) +: rep(tail, replaced = true)
      case (e: Elem) +: tail =>
        e.copy(child = rep(e.child, replaced)).asInstanceOf[Node] +: rep(tail, replaced)
      case n +: tail => n +: rep(tail, replaced)
    }

    elem.copy(child = rep(elem.child, replaced = false))
  }
}
