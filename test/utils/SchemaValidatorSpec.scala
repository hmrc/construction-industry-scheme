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
import uk.gov.hmrc.constructionindustryscheme.utils.SchemaValidator

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

  "SchemaValidator" should {

    "validate XML against all related XSDs from resources" in {
      val validXml =
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

      validateXml(validXml, schema) shouldBe true
    }

    "fail validation for invalid AOref" in {
      val invalidXml =
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

      validateXml(invalidXml, schema) shouldBe false
    }
  }
}
