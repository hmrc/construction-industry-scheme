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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig
import uk.gov.hmrc.constructionindustryscheme.utils.{SchemaValidator, XmlValidator}

import javax.xml.validation.Schema
import scala.util.Success
import scala.xml.NodeSeq

class XmlValidatorSpec extends AnyWordSpec with Matchers with MockitoSugar {

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

  "XmlValidator" should {

    val mockSchemaValidator = mock[SchemaValidator]
    val mockConfig = mock[AppConfig]
    val mockSchema = mock[Schema]

    when(mockConfig.schema).thenReturn(mockSchema)
    val validator = new XmlValidator(mockConfig, mockSchemaValidator)


    "validate returns Success(Unit) if schema validation returns true" in {
      when(mockSchemaValidator.validate(any[String], any[Schema])).thenReturn(true)
      val result = validator.validate(validXml)
      assert(result == Success(()))
    }

    "validate returns Failure(RuntimeException) if schema validation returns false" in {
      when(mockSchemaValidator.validate(any[String], any[Schema])).thenReturn(false)
      val result = validator.validate(invalidXml)
      assert(result.isFailure)
      assert(result.failed.get.getMessage.contains("XML validation failed against schema"))
    }

    "validate returns Failure if an exception is thrown" in {
      when(mockSchemaValidator.validate(any[String], any[Schema]))
        .thenThrow(new RuntimeException("Boom!"))
      val result = validator.validate(invalidXml)
      assert(result.isFailure)
      assert(result.failed.get.getMessage.contains("Boom!"))
    }

  }
}
