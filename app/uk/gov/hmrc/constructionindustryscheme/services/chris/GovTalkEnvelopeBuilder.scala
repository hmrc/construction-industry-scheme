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

package uk.gov.hmrc.constructionindustryscheme.services.chris

import scala.xml.{Elem, Node}

final case class EnvelopeProfile(
  messageDetailsClass: String,
  qualifier: String,
  function: String,
  transformation: String,
  organisation: String,
  channelUri: String,
  channelProduct: String,
  channelVersion: String,
  defaultCurrency: String,
  namespace: String,
  schemaVersion: String,
  topElementName: String
)

object EnvelopeProfile {
  val Return: EnvelopeProfile = EnvelopeProfile(
    messageDetailsClass = ChrisEnvelopeConstants.MessageDetailsClass,
    qualifier = ChrisEnvelopeConstants.Qualifier,
    function = ChrisEnvelopeConstants.Function,
    transformation = ChrisEnvelopeConstants.Transformation,
    organisation = ChrisEnvelopeConstants.Organisation,
    channelUri = ChrisEnvelopeConstants.ChannelUri,
    channelProduct = ChrisEnvelopeConstants.ChannelProduct,
    channelVersion = ChrisEnvelopeConstants.ChannelVersion,
    defaultCurrency = ChrisEnvelopeConstants.DefaultCurrency,
    namespace = ChrisEnvelopeConstants.Namespace,
    schemaVersion = ChrisEnvelopeConstants.SchemaVersion,
    topElementName = ChrisEnvelopeConstants.TopElementName
  )

  val Verification: EnvelopeProfile = EnvelopeProfile(
    messageDetailsClass = ChrisVerificationEnvelopeConstants.MessageDetailsClass,
    qualifier = ChrisVerificationEnvelopeConstants.Qualifier,
    function = ChrisVerificationEnvelopeConstants.Function,
    transformation = ChrisVerificationEnvelopeConstants.Transformation,
    organisation = ChrisVerificationEnvelopeConstants.Organisation,
    channelUri = ChrisVerificationEnvelopeConstants.ChannelUri,
    channelProduct = ChrisVerificationEnvelopeConstants.ChannelProduct,
    channelVersion = ChrisVerificationEnvelopeConstants.ChannelVersion,
    defaultCurrency = ChrisVerificationEnvelopeConstants.DefaultCurrency,
    namespace = ChrisVerificationEnvelopeConstants.Namespace,
    schemaVersion = ChrisVerificationEnvelopeConstants.SchemaVersion,
    topElementName = ChrisVerificationEnvelopeConstants.TopElementName
  )
}

object GovTalkEnvelopeBuilder {

  def build(
    profile: EnvelopeProfile,
    taxOfficeNumber: String,
    taxOfficeReference: String,
    correlationId: String,
    gatewayTimestamp: String,
    periodEnd: String,
    sender: String,
    payload: Node
  ): Elem =
    <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
      <EnvelopeVersion>2.0</EnvelopeVersion>
      <Header>
        <MessageDetails>
          <Class>{profile.messageDetailsClass}</Class>
          <Qualifier>{profile.qualifier}</Qualifier>
          <Function>{profile.function}</Function>
          <CorrelationID>{correlationId}</CorrelationID>
          <Transformation>{profile.transformation}</Transformation>
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
          <Organisation>{profile.organisation}</Organisation>
        </TargetDetails>
        <ChannelRouting>
          <Channel>
            <URI>{profile.channelUri}</URI>
            <Product>{profile.channelProduct}</Product>
            <Version>{profile.channelVersion}</Version>
          </Channel>
        </ChannelRouting>
      </GovTalkDetails>
      <Body>
        <IRenvelope xmlns={profile.namespace}>
          <IRheader>
            <Keys>
              <Key Type="TaxOfficeNumber">{taxOfficeNumber}</Key>
              <Key Type="TaxOfficeReference">{taxOfficeReference}</Key>
            </Keys>
            <PeriodEnd>{periodEnd}</PeriodEnd>
            <DefaultCurrency>{profile.defaultCurrency}</DefaultCurrency>
            <Manifest>
              <Contains>
                <Reference>
                  <Namespace>{profile.namespace}</Namespace>
                  <SchemaVersion>{profile.schemaVersion}</SchemaVersion>
                  <TopElementName>{profile.topElementName}</TopElementName>
                </Reference>
              </Contains>
            </Manifest>
            <IRmark Type="generic">TBC</IRmark>
            <Sender>{sender}</Sender>
          </IRheader>
          {payload}
        </IRenvelope>
      </Body>
    </GovTalkMessage>
}
