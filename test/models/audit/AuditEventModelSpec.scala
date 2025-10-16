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

package models.audit

import base.SpecBase
import org.scalatest.matchers.should.Matchers.{should, shouldBe}
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.models.audit.{AuditEventModel, AuditResponseReceivedModel, NilReturnSubmissionAuditEvent}
import uk.gov.hmrc.constructionindustryscheme.utils.XmlToJsonConvertor.convertXmlToJson

class AuditEventModelSpec extends SpecBase {
  
  "ChrisSubmissionAuditEvent" - {

    val validElem = scala.xml.XML.load(getClass.getResource("/irmark/ValidCisReturnEnvelope.xml"))
    val submissionData = convertXmlToJson(validElem.toString)
    val validNiReturnElem = scala.xml.XML.load(getClass.getResource("/ValidNilReturnSubmissionResponse.xml"))
    val responseData = convertXmlToJson(validNiReturnElem.toString)

    val dto = NilReturnSubmissionAuditEvent(
      payload = submissionData,
      response = AuditResponseReceivedModel(200, responseData)
    )

    val json = Json.toJson(dto)
    val expected = Json.parse("""{
                                |"payload": {
                                |  "GovTalkMessage" : {
                                |    "Header" : {
                                |      "MessageDetails" : {
                                |        "Qualifier" : "request",
                                |        "Function" : "submit",
                                |        "CorrelationID" : "7DCB7535A50F4BFC9D4F5C69C0F677D3",
                                |        "Class" : "IR-CIS-CIS300MR",
                                |        "GatewayTimestamp" : "2025-10-07T08:39:42.752",
                                |        "Transformation" : "XML"
                                |      },
                                |      "SenderDetails" : { }
                                |    },
                                |    "EnvelopeVersion" : "2.0",
                                |    "Body" : {
                                |      "IRenvelope" : {
                                |        "IRheader" : {
                                |          "Keys" : {
                                |            "Key" : [ {
                                |              "Type" : "TaxOfficeNumber",
                                |              "Key" : "123"
                                |            }, {
                                |              "Type" : "TaxOfficeReference",
                                |              "Key" : "AB456"
                                |            } ]
                                |          },
                                |          "Sender" : "Company",
                                |          "PeriodEnd" : "2013-09-05",
                                |          "IRmark" : {
                                |            "Type" : "generic"
                                |          },
                                |          "Manifest" : {
                                |            "Contains" : {
                                |              "Reference" : {
                                |                "TopElementName" : "CISreturn",
                                |                "SchemaVersion" : "2005-v1.1",
                                |                "Namespace" : "http://www.govtalk.gov.uk/taxation/CISreturn"
                                |              }
                                |            }
                                |          },
                                |          "DefaultCurrency" : "GBP"
                                |        },
                                |        "CISreturn" : {
                                |          "NilReturn" : "yes",
                                |          "Contractor" : {
                                |            "UTR" : "1234567890",
                                |            "AOref" : "1234567XY"
                                |          },
                                |          "Declarations" : {
                                |            "Inactivity" : "yes",
                                |            "InformationCorrect" : "yes"
                                |          }
                                |        }
                                |      }
                                |    },
                                |    "GovTalkDetails" : {
                                |      "ChannelRouting" : {
                                |        "Channel" : {
                                |          "URI" : "0126",
                                |          "Version" : "3.4",
                                |          "Product" : "EzGov IR-CIS-CIS300MR"
                                |        }
                                |      },
                                |      "Keys" : {
                                |        "Key" : [ {
                                |          "Type" : "TaxOfficeNumber",
                                |          "Key" : "123"
                                |        }, {
                                |          "Type" : "TaxOfficeReference",
                                |          "Key" : "AB456"
                                |        } ]
                                |      },
                                |      "TargetDetails" : {
                                |        "Organisation" : "IR"
                                |      }
                                |    }
                                |  }
                                |},
                                |"response":{
                                |   "status":200,
                                |   "responseData":{
                                |       "GovTalkMessage":{
                                |         "Header":{
                                |           "MessageDetails":{
                                |             "ResponseEndPoint":{},
                                |             "Qualifier":"response",
                                |             "Function":"submit",
                                |             "CorrelationID":"49654E0E5535489F97B6F504E0ACE7C7",
                                |             "Class":"IR-CIS-CIS300MR",
                                |             "GatewayTimestamp":"2025-10-16T13:25:28.720",
                                |             "Transformation":"XML"
                                |           }
                                |         },
                                |         "EnvelopeVersion":"2.0",
                                |         "Body":{
                                |           "SuccessResponse":{
                                |             "AcceptedTime":"2017-04-06T08:46:08.081",
                                |             "IRmarkReceipt":{
                                |               "Signature":{
                                |                 "SignedInfo":{
                                |                   "Reference":{
                                |                     "Transforms":{
                                |                       "Transform":[
                                |                         {
                                |                           "Algorithm":"http://www.w3.org/TR/1999/REC-xpath-19991116",
                                |                           "XPath":"(count(ancestor-or-self::node()|/gti:GovTalkMessage/gti:Body)=count(ancestor-or-self::node())) and (count(ancestor-or-self::node()|/gti:GovTalkMessage/gti:Body/*[name()='IRenvelope']/*[name()='IRheader']/*[name()='IRmark'])!=count(ancestor-or-self::node()))"
                                |                         },
                                |                         {
                                |                           "Algorithm":"http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments"
                                |                         }
                                |                       ]
                                |                     },
                                |                     "DigestValue":"mMnIokxfPI0/v44JEqDDIr1iQvU=",
                                |                     "DigestMethod":{
                                |                       "Algorithm":"http://www.w3.org/2000/09/xmldsig#sha1"
                                |                     }
                                |                   },
                                |                   "CanonicalizationMethod":{
                                |                     "Algorithm":"http://www.w3.org/TR/2001/REC-xml-c14n-20010315"
                                |                   },
                                |                   "SignatureMethod":{
                                |                     "Algorithm":"http://www.w3.org/2000/09/xmldsig#rsa-sha1"
                                |                   }
                                |                 },
                                |                 "SignatureValue":"xjd0lzhAQrnHZsE5inNCOVsmwcQ9HTu+CFUoyqEcOhVvxj2jvYGcjkhu7sZkZJ9RBjBcEP/eQTbesMTrnUgofuMqaROt8ZyD/RJKFIwh5TtNzYzDM55Pa3GDd2ZXcmfR38mS9KPwqc5Ty+Eqv69FxqivCQk46H20F8fnWnx85H4=",
                                |                 "KeyInfo":{
                                |                   "X509Data":{
                                |                     "X509Certificate":"MIID0zCCAzygAwIBAgIBADANBgkqhkiG9w0BAQQFADCBqDELMAkGA1UEBhMCbmwxFjAUBgNVBAgTDU5vb3JkLUhvbGxhbmQxFzAVBgNVBAoTDk1vYmlsZWZpc2guY29tMRAwDgYDVQQHEwdaYWFuZGFtMRIwEAYDVQQLEwlNYXJrZXRpbmcxGzAZBgNVBAMTEnd3dy5tb2JpbGVmaXNoLmNvbTElMCMGCSqGSIb3DQEJARYWY29udGFjdEBtb2JpbGVmaXNoLmNvbTAeFw0xMTEwMTMxMDI2NTZaFw0xMjEwMTIxMDI2NTZaMIGoMQswCQYDVQQGEwJubDEWMBQGA1UECBMNTm9vcmQtSG9sbGFuZDEXMBUGA1UEChMOTW9iaWxlZmlzaC5jb20xEDAOBgNVBAcTB1phYW5kYW0xEjAQBgNVBAsTCU1hcmtldGluZzEbMBkGA1UEAxMSd3d3Lm1vYmlsZWZpc2guY29tMSUwIwYJKoZIhvcNAQkBFhZjb250YWN0QG1vYmlsZWZpc2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQD3o83CcmMMOC/fnjVv2puirJTs36+al6RDBe2tbFLKKODd29DZbmH9/6R77VPZACvXxBdRzMls//YRVHoJyJVudy+B4siUfHP80pssg2ZXCmCtUZGS71ohmlHcGQGTVLj8wmicf/DfmMAgq19OFZJP5LUn3md/MQBOUYrFXt21dQIDAQABo4IBCTCCAQUwHQYDVR0OBBYEFAIuWYA/BMx8Gn/YOILevnJthkIZMIHVBgNVHSMEgc0wgcqAFAIuWYA/BMx8Gn/YOILevnJthkIZoYGupIGrMIGoMQswCQYDVQQGEwJubDEWMBQGA1UECBMNTm9vcmQtSG9sbGFuZDEXMBUGA1UEChMOTW9iaWxlZmlzaC5jb20xEDAOBgNVBAcTB1phYW5kYW0xEjAQBgNVBAsTCU1hcmtldGluZzEbMBkGA1UEAxMSd3d3Lm1vYmlsZWZpc2guY29tMSUwIwYJKoZIhvcNAQkBFhZjb250YWN0QG1vYmlsZWZpc2guY29tggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADgYEABCb+f82DKWIWBczTeKGc6Ka5U7oys/itCY7XOYMIvXYPj+tb+5PBrmTO3jZNoZso9cYYFcDGXySbk6wSZiEPlbMqkoYE62E6dVXAmbza3ZNNIX/yEpkE3ZeBBtYzJMPQme9jrMgwgMIhgVzQNL2KPkbWOtQfoYgnThHQKLBry6Y="
                                |                   }
                                |                 }
                                |               },
                                |               "Message":{
                                |                 "code":"1\\",
                                |                 "Message":"HMRC has received the IR-CIS-CIS300MR document ref: 123/GL01 at 08.46 on 06/04/2017. The associated IRmark was: TBPJFWEAYSD4GFVRMHY7KLWEBHB5BLA5. We advise you to keep this receipt in both electronic and hardcopy versions for your records. You may wish to use them to identify your submission in the future."
                                |               }
                                |             },
                                |             "Message":{
                                |               "code":"9004",
                                |               "Message":"The Monthly Return has been processed and passed full validation"
                                |             }
                                |           }
                                |         },
                                |       "GovTalkDetails":{
                                |         "Keys":{}
                                |       }
                                |     }
                                |   }
                                |}
                                |}""".stripMargin)

    "must serialise correctly" in {
      json mustBe expected
    }
  }
  
  "extendedDataEvent" - {
    val testAuditType: String = "test-audit-type"
    val testDetailJson: JsValue = Json.toJson(testAuditType)
    "behave as expected" in {
      val event = new AuditEventModel {
        override val auditType: String = testAuditType
        override val detailJson: JsValue = testDetailJson
      }
      val extended = event.extendedDataEvent
      extended.auditType shouldBe testAuditType
      extended.detail shouldBe testDetailJson
    }
  }
  
}
