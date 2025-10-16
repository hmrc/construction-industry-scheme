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
import uk.gov.hmrc.constructionindustryscheme.utils.XmlToJsonConvertor.convertXmlToJson


class XmlToJsonConvertorSpec extends SpecBase {

  "convert XML with multiple elements into JSON arrays using Play JSON" in {
    val xml =
      """<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
        |  <GovTalkDetails>
        |    <Keys>
        |      <Key Type="TaxOfficeNumber">754</Key>
        |      <Key Type="TaxOfficeReference">EZ00047</Key>
        |    </Keys>
        |  </GovTalkDetails>
        |</GovTalkMessage>""".stripMargin
    val jsonStr = convertXmlToJson(xml)
    val json = jsonStr
    val keysArray = (json \ "GovTalkMessage" \ "GovTalkDetails" \ "Keys" \ "Key").as[JsArray]
    assert(keysArray.value.size == 2)
    val firstKey = keysArray.value.head.as[JsObject]
    assert((firstKey \ "Type").as[String] == "TaxOfficeNumber")
    assert((firstKey \ "Key").as[String] == "754")
    val secondKey = keysArray.value(1).as[JsObject]
    assert((secondKey \ "Type").as[String] == "TaxOfficeReference")
    assert((secondKey \ "Key").as[String] == "EZ00047")
  }

  "convert XML with attributes and text correctly" in {
    val xml =
      """<Envelope>
        |  <IRmark Type="generic">mMnIokxfPI0/v44JEqDDIr1iQvU=</IRmark>
        |  <NilReturn>yes</NilReturn>
        |</Envelope>""".stripMargin
    val jsonStr = convertXmlToJson(xml)
    val json = jsonStr
    val irMark = (json \ "Envelope" \ "IRmark").as[JsObject]
    assert((irMark \ "Type").as[String] == "generic")
    assert((irMark \ "IRmark").as[String] == "mMnIokxfPI0/v44JEqDDIr1iQvU=")
    assert((json \ "Envelope" \ "NilReturn").as[String] == "yes")
  }

}
