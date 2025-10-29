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

import base.SpecBase
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.utils.XmlToJsonConvertor

class XmlToJsonConvertorSpec extends SpecBase {

  "should return success = true for valid XML" in {
    val xml =
          """<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
            |  <GovTalkDetails>
            |    <Keys>
            |      <Key Type="TaxOfficeNumber">754</Key>
            |      <Key Type="TaxOfficeReference">EZ00047</Key>
            |    </Keys>
            |  </GovTalkDetails>
            |</GovTalkMessage>""".stripMargin
    val result = XmlToJsonConvertor.convertXmlToJson(xml)
    assert(result.success)
    assert(result.json.isDefined)
    val keysArray = (result.json.get \ "GovTalkMessage" \ "GovTalkDetails" \ "Keys" \ "Key").as[JsArray]
    assert(keysArray.value.size == 2)
    val firstKey = keysArray.value.head.as[JsObject]
    assert((firstKey \ "Type").as[String] == "TaxOfficeNumber")
    assert((firstKey \ "Key").as[String] == "754")
    val secondKey = keysArray.value(1).as[JsObject]
    assert((secondKey \ "Type").as[String] == "TaxOfficeReference")
    assert((secondKey \ "Key").as[String] == "EZ00047")
  }

  "should return success = false for invalid XML" in {
    val invalidXml =
      """<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
        |  <GovTalkDetails>
        |    <Keys>
        |      <Key Type="TaxOfficeNumber">754
        |      <Key Type="TaxOfficeReference">EZ00047</Key>
        |    </Keys>
        |  </GovTalkDetails>
        |</GovTalkMessage>""".stripMargin // malformed XML
    val result = XmlToJsonConvertor.convertXmlToJson(invalidXml)
    assert(!result.success)
    assert(result.json.isEmpty)
    assert(result.error.exists(_.startsWith("Invalid XML input:")))
  }
  
}
