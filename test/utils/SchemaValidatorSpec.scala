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

package utils

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.xml.sax.SAXParseException
import uk.gov.hmrc.constructionindustryscheme.utils.{SchemaValidator, ValidationHandler}
import org.mockito.ArgumentMatchers._

import java.io.File
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class SchemaValidatorSpec extends AnyWordSpec with Matchers {

  private def loadSchema(): javax.xml.validation.Schema = {
    val xsdFiles = Seq(
      "/xsds/CISreturn-v1-2.xsd",
      "/xsds/core-v2-0.xsd"
    ).map(path => new StreamSource(new File(getClass.getResource(path).toURI)))

    val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)

    schemaFactory.newSchema(xsdFiles.toArray.map(_.asInstanceOf[javax.xml.transform.Source]))
  }

  private def validateXml(xml: String, schema: javax.xml.validation.Schema): Boolean = {
    val validator = new SchemaValidator
    validator.validate(xml, schema)
  }

  private val schema = loadSchema()

  val validXml: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
      |      <IRheader>
      |        <Keys>
      |          <Key Type="TaxOfficeNumber">754</Key>
      |          <Key Type="TaxOfficeReference">EZ00100</Key>
      |        </Keys>
      |        <PeriodEnd>2025-05-05</PeriodEnd>
      |        <DefaultCurrency>GBP</DefaultCurrency>
      |        <Manifest>
      |          <Contains>
      |            <Reference>
      |              <Namespace>http://www.govtalk.gov.uk/taxation/CISreturn</Namespace>
      |              <SchemaVersion>2005-v1.1</SchemaVersion>
      |              <TopElementName>CISreturn</TopElementName>
      |            </Reference>
      |          </Contains>
      |        </Manifest>
      |        <IRmark Type="generic">Fv1KhWmy3UvlCGU/skHcT01qiiI=</IRmark>
      |        <Sender>Company</Sender>
      |      </IRheader>
      |      <CISreturn>
      |        <Contractor>
      |          <UTR>1234567890</UTR>
      |          <AOref>754PT00002240</AOref>
      |        </Contractor>
      |        <NilReturn>yes</NilReturn>
      |        <Declarations>
      |          <InformationCorrect>yes</InformationCorrect>
      |        </Declarations>
      |      </CISreturn>
      |    </IRenvelope>
      |""".stripMargin

  val invalidXml: String =
    """<?xml version="1.0" encoding="UTF-8"?>
      |<IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
      |      <IRheader>
      |        <Keys>
      |          <Key Type="TaxOfficeNumber">754</Key>
      |          <Key Type="TaxOfficeReference">EZ00100</Key>
      |        </Keys>
      |        <PeriodEnd>2025-05-05</PeriodEnd>
      |        <DefaultCurrency>GBP</DefaultCurrency>
      |        <Manifest>
      |          <Contains>
      |            <Reference>
      |              <Namespace>http://www.govtalk.gov.uk/taxation/CISreturn</Namespace>
      |              <SchemaVersion>2005-v1.1</SchemaVersion>
      |              <TopElementName>CISreturn</TopElementName>
      |            </Reference>
      |          </Contains>
      |        </Manifest>
      |        <IRmark Type="generic">Fv1KhWmy3UvlCGU/skHcT01qiiI=</IRmark>
      |        <Sender>Company</Sender>
      |      </IRheader>
      |      <CISreturn>
      |        <Contractor>
      |          <UTR>1234567890</UTR>
      |          <AOref>AOREF</AOref>
      |        </Contractor>
      |        <NilReturn>yes</NilReturn>
      |        <Declarations>
      |          <InformationCorrect>yes</InformationCorrect>
      |        </Declarations>
      |      </CISreturn>
      |    </IRenvelope>
      |""".stripMargin

  "ValidationHandler" should {

    "set error to true on warning" in {
      val handler = new ValidationHandler()
      handler.error shouldBe false
      val ex = new SAXParseException("warning", null)
      handler.warning(ex)
      handler.error shouldBe true
    }

    "set error to true on error" in {
      val handler = new ValidationHandler()
      handler.error shouldBe false
      val ex = new SAXParseException("error", null)
      handler.error(ex)
      handler.error shouldBe true
    }

    "set error to true on fatalError" in {
      val handler = new ValidationHandler()
      handler.error shouldBe false
      val ex = new SAXParseException("fatal", null)
      handler.fatalError(ex)
      handler.error shouldBe true
    }

  }

  "ValidationHandler.describe" should {

    "report only the code, element and position for a real pattern violation, never the value" in {
      val piiXml = invalidXml.replace("<AOref>AOREF</AOref>", "<AOref>SECRET-VALUE-JANE-SMITH</AOref>")
      val lines  = collectDescriptions(piiXml)

      lines                                       should not be empty
      lines.foreach { l =>
        l should not include "SECRET"
        l should not include "JANE"
        l should startWith("code=cvc-")
        l should include regex "line=\\d+ col=\\d+"
      }
      lines.exists(_.contains("element=AOref")) shouldBe true
    }

    "ignore element-like text inside a quoted user value" in {
      val e = new SAXParseException(
        "cvc-pattern-valid: Value 'x of element 'Smith'' is not facet-valid with respect to pattern 'p' for type 't'.",
        null,
        null,
        3,
        7
      )
      ValidationHandler.describe(e) shouldBe "code=cvc-pattern-valid line=3 col=7"
    }

    "take the real element name when a value contains element-like text" in {
      val e = new SAXParseException(
        "cvc-type.3.1.3: The value 'a of element 'Smith' b' of element 'UTR' is not valid.",
        null,
        null,
        1,
        9
      )
      ValidationHandler.describe(e) shouldBe "code=cvc-type.3.1.3 element=UTR line=1 col=9"
    }

    "name the unexpected element and the attribute for structural errors" in {
      ValidationHandler.describe(
        new SAXParseException(
          "cvc-complex-type.2.4.a: Invalid content was found starting with element 'Foo'. One of '{Bar}' is expected.",
          null,
          null,
          2,
          4
        )
      ) shouldBe "code=cvc-complex-type.2.4.a element=Foo line=2 col=4"

      ValidationHandler.describe(
        new SAXParseException(
          "cvc-complex-type.3.2.2: Attribute 'Type' is not allowed to appear in element 'Key'.",
          null,
          null,
          5,
          6
        )
      ) shouldBe "code=cvc-complex-type.3.2.2 element=Key attribute=Type line=5 col=6"
    }

    "fall back to unknown for messages without a recognisable code" in {
      ValidationHandler.describe(new SAXParseException("Content is not allowed in prolog. John Smith", null)) shouldBe
        "code=unknown line=-1 col=-1"
    }
  }

  private def collectDescriptions(xml: String): Seq[String] = {
    val buf       = scala.collection.mutable.ListBuffer.empty[String]
    val validator = schema.newValidator()
    validator.setErrorHandler(new org.xml.sax.ErrorHandler {
      def warning(e: SAXParseException): Unit    = buf += ValidationHandler.describe(e)
      def error(e: SAXParseException): Unit      = buf += ValidationHandler.describe(e)
      def fatalError(e: SAXParseException): Unit = buf += ValidationHandler.describe(e)
    })
    validator.validate(new StreamSource(new java.io.StringReader(xml)))
    buf.toList
  }

  "SchemaValidator" should {

    "return true for valid XML" in {
      validateXml(validXml, schema) shouldBe true
    }

    "return false for invalid XML - invalid AOref" in {
      validateXml(invalidXml, schema) shouldBe false
    }

    "return false for malformed XML" in {
      val malformedXml = "<greeting>Hello"
      validateXml(malformedXml, schema) shouldBe false
    }

  }
}
