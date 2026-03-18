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

package uk.gov.hmrc.constructionindustryscheme.services.chris.xml

import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeConstants
import scala.xml.{Elem, Node}

object ChrisEnvelopeXmlBuilder {
  def buildEnvelope(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    correlationId: String,
    gatewayTimestamp: String,
    periodEnd: String,
    sender: String,
    cisReturn: Node
  ): Elem =
    <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
      <EnvelopeVersion>2.0</EnvelopeVersion>
      <Header>
        <MessageDetails>
          <Class>{ChrisEnvelopeConstants.MessageDetailsClass}</Class>
          <Qualifier>{ChrisEnvelopeConstants.Qualifier}</Qualifier>
          <Function>{ChrisEnvelopeConstants.Function}</Function>
          <CorrelationID>{correlationId}</CorrelationID>
          <Transformation>{ChrisEnvelopeConstants.Transformation}</Transformation>
          <GatewayTimestamp>{gatewayTimestamp}</GatewayTimestamp>
        </MessageDetails>
        <SenderDetails/>
      </Header>
      <GovTalkDetails>
        <Keys>
          <Key Type="TaxOfficeNumber">{taxOfficeNumber}</Key>
          <Key Type="TaxOfficeReference">{taxOfficeReference}</Key>
        </Keys>
        <TargetDetails>
          <Organisation>{ChrisEnvelopeConstants.Organisation}</Organisation>
        </TargetDetails>
        <ChannelRouting>
          <Channel>
            <URI>{ChrisEnvelopeConstants.ChannelUri}</URI>
            <Product>{ChrisEnvelopeConstants.ChannelProduct}</Product>
            <Version>{ChrisEnvelopeConstants.ChannelVersion}</Version>
          </Channel>
        </ChannelRouting>
      </GovTalkDetails>
      <Body>
        <IRenvelope xmlns={ChrisEnvelopeConstants.Namespace}>
          <IRheader>
            <Keys>
              <Key Type="TaxOfficeNumber">{taxOfficeNumber}</Key>
              <Key Type="TaxOfficeReference">{taxOfficeReference}</Key>
            </Keys>
            <PeriodEnd>{periodEnd}</PeriodEnd>
            <DefaultCurrency>{ChrisEnvelopeConstants.DefaultCurrency}</DefaultCurrency>
            <Manifest>
              <Contains>
                <Reference>
                  <Namespace>{ChrisEnvelopeConstants.Namespace}</Namespace>
                  <SchemaVersion>{ChrisEnvelopeConstants.SchemaVersion}</SchemaVersion>
                  <TopElementName>{ChrisEnvelopeConstants.TopElementName}</TopElementName>
                </Reference>
              </Contains>
            </Manifest>
            <IRmark Type="generic">TBC</IRmark>
            <Sender>{sender}</Sender>
          </IRheader>
          {cisReturn}
        </IRenvelope>
      </Body>
    </GovTalkMessage>
}
