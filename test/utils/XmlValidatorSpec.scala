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

package utils

import base.SpecBase
import play.api.mvc.Results.InternalServerError
import uk.gov.hmrc.constructionindustryscheme.utils.XmlValidator

import scala.xml.NodeSeq

class XmlValidatorSpec extends SpecBase {

  val validXml: NodeSeq =
    <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
      <IRheader>
        <Keys>
          <Key Type="TaxOfficeNumber">754</Key>
          <Key Type="TaxOfficeReference">EZ00100</Key>
        </Keys>
        <PeriodEnd>2025-05-05</PeriodEnd>
        <DefaultCurrency>GBP</DefaultCurrency>
        <Manifest>
          <Contains>
            <Reference>
              <Namespace>http://www.govtalk.gov.uk/taxation/CISreturn</Namespace>
              <SchemaVersion>2005-v1.1</SchemaVersion>
              <TopElementName>CISreturn</TopElementName>
            </Reference>
          </Contains>
        </Manifest>
        <IRmark Type="generic">Fv1KhWmy3UvlCGU/skHcT01qiiI=</IRmark>
        <Sender>Company</Sender>
      </IRheader>
      <CISreturn>
        <Contractor>
          <UTR>1234567890</UTR>
          <AOref>754PT00002240</AOref>
        </Contractor>
        <NilReturn>yes</NilReturn>
        <Declarations>
          <InformationCorrect>yes</InformationCorrect>
        </Declarations>
      </CISreturn>
    </IRenvelope>

  val invalidXml: NodeSeq =
    <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
      <IRheader>
        <Keys>
          <Key Type="TaxOfficeNumber">754</Key>
          <Key Type="TaxOfficeReference">EZ00100</Key>
        </Keys>
        <PeriodEnd>2025-05-05</PeriodEnd>
        <DefaultCurrency>GBP</DefaultCurrency>
        <Manifest>
          <Contains>
            <Reference>
              <Namespace>http://www.govtalk.gov.uk/taxation/CISreturn</Namespace>
              <SchemaVersion>2005-v1.1</SchemaVersion>
              <TopElementName>CISreturn</TopElementName>
            </Reference>
          </Contains>
        </Manifest>
        <IRmark Type="generic">Fv1KhWmy3UvlCGU/skHcT01qiiI=</IRmark>
        <Sender>Company</Sender>
      </IRheader>
      <CISreturn>
        <Contractor>
          <UTR>1234567890</UTR>
          <AOref>00100131</AOref>
        </Contractor>
        <NilReturn>yes</NilReturn>
        <Declarations>
          <InformationCorrect>yes</InformationCorrect>
        </Declarations>
      </CISreturn>
    </IRenvelope>

  val schemaFileName = "CISreturn-v1-2.xsd"

  val xmlValidator = new XmlValidator

  "validateAgainstSchema returns Right for valid XML" in {
    val result = xmlValidator.validateAgainstSchema(validXml, schemaFileName)
    assert(result.isRight)
  }

  "validateAgainstSchema returns Left for invalid XML" in {
    val result = xmlValidator.validateAgainstSchema(invalidXml, schemaFileName)
    assert(result.isLeft)
    result.left.foreach { case (status, msg) =>
      assert(status == InternalServerError)
      assert(msg.text.nonEmpty)
      assert(msg.text.contains("The value '00100131' of element 'AOref' is not valid"))
    }
  }
}