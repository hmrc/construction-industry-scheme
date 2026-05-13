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

package uk.gov.hmrc.constructionindustryscheme.models

import play.api.Logging
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisVerificationRequest
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisVerificationEnvelopeConstants
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisVerificationRequestXmlBuilder
import uk.gov.hmrc.constructionindustryscheme.utils.CisEnrolmentHelper.extractTaxOfficeIdentifiers
import uk.gov.hmrc.constructionindustryscheme.utils.IrMarkProcessor.UpdatedPayloadWithIrMark

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneOffset}
import java.util.UUID
import scala.xml.{Elem, Node, PrettyPrinter}

case class CisVerificationSubmission(
  envelope: Elem,
  correlationId: String,
  irMark: String,
  irEnvelope: Elem
)

object CisVerificationSubmission extends Logging {

  private val gatewayTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
  private val isoDateFmt                = DateTimeFormatter.ISO_LOCAL_DATE
  private val prettyPrinter             = new PrettyPrinter(120, 4)

  private def buildXml(
    taxOfficeNumber: String,
    taxOfficeReference: String,
    correlationId: String,
    gatewayTimestamp: String,
    periodEnd: String,
    sender: String,
    cisRequest: Node
  ): Elem =
    <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
      <EnvelopeVersion>2.0</EnvelopeVersion>

      <Header>
        <MessageDetails>
          <Class>{ChrisVerificationEnvelopeConstants.MessageDetailsClass}</Class>
          <Qualifier>{ChrisVerificationEnvelopeConstants.Qualifier}</Qualifier>
          <Function>{ChrisVerificationEnvelopeConstants.Function}</Function>
          <CorrelationID>{correlationId}</CorrelationID>
          <Transformation>{ChrisVerificationEnvelopeConstants.Transformation}</Transformation>
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
          <Organisation>{ChrisVerificationEnvelopeConstants.Organisation}</Organisation>
        </TargetDetails>

        <ChannelRouting>
          <Channel>
            <URI>{ChrisVerificationEnvelopeConstants.ChannelUri}</URI>
            <Product>{ChrisVerificationEnvelopeConstants.ChannelProduct}</Product>
            <Version>{ChrisVerificationEnvelopeConstants.ChannelVersion}</Version>
          </Channel>
        </ChannelRouting>
      </GovTalkDetails>

      <Body>
        <IRenvelope xmlns={ChrisVerificationEnvelopeConstants.Namespace}>
          <IRheader>
            <Keys>
              <Key Type="TaxOfficeNumber">{taxOfficeNumber}</Key>
              <Key Type="TaxOfficeReference">{taxOfficeReference}</Key>
            </Keys>

            <PeriodEnd>{periodEnd}</PeriodEnd>
            <DefaultCurrency>{ChrisVerificationEnvelopeConstants.DefaultCurrency}</DefaultCurrency>

            <Manifest>
              <Contains>
                <Reference>
                  <Namespace>{ChrisVerificationEnvelopeConstants.Namespace}</Namespace>
                  <SchemaVersion>{ChrisVerificationEnvelopeConstants.SchemaVersion}</SchemaVersion>
                  <TopElementName>{"CISrequest"}</TopElementName>
                </Reference>
              </Contains>
            </Manifest>

            <IRmark Type="generic">TBC</IRmark>

            <Sender>{sender}</Sender>
          </IRheader>

          {cisRequest}

        </IRenvelope>
      </Body>
    </GovTalkMessage>

  def buildPayload(
    request: ChrisVerificationRequest,
    enrolments: Enrolments,
    subcontractors: Seq[SubcontractorCurrentVerification]
  ): CisVerificationSubmission = {

    val correlationId    = UUID.randomUUID().toString.replace("-", "").toUpperCase
    val gatewayTimestamp = LocalDateTime.now(ZoneOffset.UTC).format(gatewayTimestampFormatter)

    val periodEnd = LocalDate.now(ZoneOffset.UTC).format(isoDateFmt)
    val sender    = if (request.isAgent) "Agent" else "Company"

    val cisRequest: Elem =
      CisVerificationRequestXmlBuilder.build(
        contractorUtr = request.contractorUTR,
        contractorAoRef = request.contractorAORef,
        subs = subcontractors,
        action = request.action,
        declaration = request.declaration
      )

    val (taxOfficeNumber, taxOfficeReference) =
      if (!request.isAgent) {
        extractTaxOfficeIdentifiers(enrolments)
          .getOrElse(
            throw new IllegalStateException(
              "Missing CIS enrolment identifiers (TaxOfficeNumber/TaxOfficeReference) in HMRC-CIS-ORG"
            )
          )
      } else {
        // TODO this circumvents auth checks?
        (request.clientTaxOfficeNumber, request.clientTaxOfficeRef)
      }

    val envelopeXml: Elem =
      buildXml(
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestamp,
        periodEnd = periodEnd,
        sender = sender,
        cisRequest = cisRequest
      )

    val (updatedXML, irMarkBase64, irMarkBase32, irEnvelope) =
      UpdatedPayloadWithIrMark(envelopeXml.toString)

    val prettyXml = prettyPrinter.format(updatedXML)

    logger.debug(s"CIS Verification Envelope XML:\n$prettyXml")
    logger.debug(s"[CisVerificationSubmission] irMarkBase64: $irMarkBase64")

    CisVerificationSubmission(
      envelope = updatedXML,
      correlationId = correlationId,
      irMark = irMarkBase64,
      irEnvelope = irEnvelope
    )
  }

}
