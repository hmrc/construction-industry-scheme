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

package uk.gov.hmrc.constructionindustryscheme.services.irmark

import org.apache.commons.codec.binary.Base64

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import scala.xml.Elem
import org.apache.xml.security.Init
import org.apache.xml.security.signature.XMLSignatureInput
import org.apache.xml.security.transforms.Transforms
import org.w3c.dom.{Document, Node}
import play.api.Logging

import scala.util.Using

trait IrMarkGenerator extends Logging {

  private val DefaultSecHashAlgorithm = "SHA"

  private lazy val dbf: DocumentBuilderFactory = {
    val f = DocumentBuilderFactory.newInstance()
    f.setNamespaceAware(true)
    f
  }

  def generateIrMark(elem: Elem, envelopeNamespace: String): String =
    generateIrMark(parse(elemToBytes(elem)), envelopeNamespace)

  private def generateIrMark(document: org.w3c.dom.Document, envelopeNamespace: String): String =
    Base64.encodeBase64String(computeDigest(document, envelopeNamespace))

  private def elemToBytes(elem: Elem): Array[Byte] =
    elem.toString.getBytes(StandardCharsets.UTF_8)

  private def parse(bytes: Array[Byte]): Document = {
    val builder = dbf.newDocumentBuilder()
    Using.resource(new ByteArrayInputStream(bytes)) { is =>
      builder.parse(is)
    }
  }

  private def computeDigest(document: Document, envelopeNamespace: String): Array[Byte] = {
    Init.init()
    val signatureInput = transform(document, envelopeNamespace)
    val md = MessageDigest.getInstance(DefaultSecHashAlgorithm)
    val bytes = signatureInput.getBytes
    md.digest(bytes)
  }

  private def transform(node: Node, envelopeNamespace: String): XMLSignatureInput = {
    Init.init()
    val transformsDoc: Document = parse(GovTalkTransform(envelopeNamespace).getBytes(StandardCharsets.UTF_8))
    val transforms: Transforms = new Transforms(transformsDoc.getDocumentElement, null)
    val input: XMLSignatureInput = new XMLSignatureInput(node)

    val result = transforms.performTransforms(input)

    val outputBytes = result.getBytes

    if (outputBytes == null || outputBytes.isEmpty) {
      val msg = s"Input XML was not transformable for envelopeNamespace=$envelopeNamespace (empty transform output)"
      logger.error(msg)
      throw new IllegalArgumentException(msg)
    }

    result
  }

  private def GovTalkTransform(envelopeNamespace: String): String =
    s"""
    <dsig:Transforms xmlns:dsig='http://www.w3.org/2000/09/xmldsig#' xmlns:gt='http://www.govtalk.gov.uk/CM/envelope' xmlns:ns='$envelopeNamespace'>
      <dsig:Transform Algorithm='http://www.w3.org/TR/1999/REC-xpath-19991116'>
        <dsig:XPath>
          (count(ancestor-or-self::node()|/gt:GovTalkMessage/gt:Body)=count(ancestor-or-self::node()))
          and
          (count(ancestor-or-self::node()|/gt:GovTalkMessage/gt:Body/ns:IRenvelope/ns:IRheader/ns:IRmark)!=count(ancestor-or-self::node()))
        </dsig:XPath>
      </dsig:Transform>
      <dsig:Transform Algorithm='http://www.w3.org/TR/2001/REC-xml-c14n-20010315'/>
    </dsig:Transforms>""".trim()

}


object IrMarkGenerator extends IrMarkGenerator
