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
import uk.gov.hmrc.constructionindustryscheme.utils.IrMarkProcessor

class IrMarkProcessorTest extends SpecBase {
  "GenerateFullIrMark produces base64/base32 for canonicalized Body with IRmark removed" in {
    val xml        =
      """<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
    <EnvelopeVersion>2.0</EnvelopeVersion>
    <Header>
        <MessageDetails>
            <Class>IR-CIS-CIS300MR</Class>
            <Qualifier>request</Qualifier>
            <Function>submit</Function>
            <CorrelationID>C389CEF5BCC84EE2926CFDE96C89C767</CorrelationID>
            <Transformation>XML</Transformation>
            <GatewayTimestamp>2025-11-18T11:01:45.518</GatewayTimestamp>
        </MessageDetails>
        <SenderDetails/>
    </Header>
    <GovTalkDetails>
        <Keys>
            <Key Type="TaxOfficeNumber">754</Key>
            <Key Type="TaxOfficeReference">EZ00047</Key>
        </Keys>
        <TargetDetails>
            <Organisation>IR</Organisation>
        </TargetDetails>
        <ChannelRouting>
            <Channel>
                <URI>0126</URI>
                <Product>EzGov IR-CIS-CIS300MR</Product>
                <Version>3.4</Version>
            </Channel>
        </ChannelRouting>
    </GovTalkDetails>
    <Body>
        <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
            <IRheader>
                <Keys>
                    <Key Type="TaxOfficeNumber">754</Key>
                    <Key Type="TaxOfficeReference">EZ00047</Key>
                </Keys>
                <PeriodEnd>2021-01-05</PeriodEnd>
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
                <IRmark Type="generic">U8aI0sJHCxRfquaDcV0fhlhTU8s=</IRmark>
                <Sender>Company</Sender>
            </IRheader>
            <CISreturn>
                <Contractor>
                    <UTR>2234567890</UTR>
                    <AOref>754PT00002240</AOref>
                </Contractor>
                <NilReturn>yes</NilReturn>
                <Declarations>
                    <InformationCorrect>yes</InformationCorrect>
                    <Inactivity>yes</Inactivity>
                </Declarations>
            </CISreturn>
        </IRenvelope>
    </Body>
</GovTalkMessage>"""
    val (b64, b32) = IrMarkProcessor.GenerateFullIrMark(xml)
    assert(b64.length == 28)
    assert(b32.trim.length >= 32)
  }

  "UpdatedPayloadWithIrMark replaces single IRmark, returns the updatedXML, base64, base32 and irEnvelope" in {
    val xml                                      =
      """<GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
    <EnvelopeVersion>2.0</EnvelopeVersion>
    <Header>
        <MessageDetails>
            <Class>IR-CIS-CIS300MR</Class>
            <Qualifier>request</Qualifier>
            <Function>submit</Function>
            <CorrelationID>C389CEF5BCC84EE2926CFDE96C89C767</CorrelationID>
            <Transformation>XML</Transformation>
            <GatewayTimestamp>2025-11-18T11:01:45.518</GatewayTimestamp>
        </MessageDetails>
        <SenderDetails/>
    </Header>
    <GovTalkDetails>
        <Keys>
            <Key Type="TaxOfficeNumber">754</Key>
            <Key Type="TaxOfficeReference">EZ00047</Key>
        </Keys>
        <TargetDetails>
            <Organisation>IR</Organisation>
        </TargetDetails>
        <ChannelRouting>
            <Channel>
                <URI>0126</URI>
                <Product>EzGov IR-CIS-CIS300MR</Product>
                <Version>3.4</Version>
            </Channel>
        </ChannelRouting>
    </GovTalkDetails>
    <Body>
        <IRenvelope xmlns="http://www.govtalk.gov.uk/taxation/CISreturn">
            <IRheader>
                <Keys>
                    <Key Type="TaxOfficeNumber">754</Key>
                    <Key Type="TaxOfficeReference">EZ00047</Key>
                </Keys>
                <PeriodEnd>2021-01-05</PeriodEnd>
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
                <IRmark Type="generic">U8aI0sJHCxRfquaDcV0fhlhTU8s=</IRmark>
                <Sender>Company</Sender>
            </IRheader>
            <CISreturn>
                <Contractor>
                    <UTR>2234567890</UTR>
                    <AOref>754PT00002240</AOref>
                </Contractor>
                <NilReturn>yes</NilReturn>
                <Declarations>
                    <InformationCorrect>yes</InformationCorrect>
                    <Inactivity>yes</Inactivity>
                </Declarations>
            </CISreturn>
        </IRenvelope>
    </Body>
</GovTalkMessage>"""
    val (resultElem, base64, base32, irEnvelope) = IrMarkProcessor.UpdatedPayloadWithIrMark(xml)
    assert(resultElem.toString.contains("""<IRmark Type="generic">tpwOaKfCHJDirqJn31ceHrX1XYc=</IRmark>"""))
    assert(base64.length == 28)
    assert(base32.trim.length >= 32)
  }
}
