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

package uk.gov.hmrc.constructionindustryscheme.models

import play.api.Logging
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeConstants
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisReturnXmlBuilder
import uk.gov.hmrc.constructionindustryscheme.utils.IrMarkProcessor.UpdatedPayloadWithIrMark

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, YearMonth, ZoneOffset}
import java.util.UUID
import scala.util.Try
import scala.xml.{Elem, Node, PrettyPrinter}

case class ChRISSubmission(
  envelope: Elem,
  correlationId: String,
  irMark: String,
  irEnvelope: Elem
)

object ChRISSubmission extends Logging {
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

  private def buildChrisSubmission(
    request: ChrisSubmissionRequest,
    enrolments: Enrolments
  ): ChRISSubmission = {
    val correlationId    = UUID.randomUUID().toString.replace("-", "").toUpperCase
    val gatewayTimestamp = LocalDateTime.now(ZoneOffset.UTC).format(gatewayTimestampFormatter)

    val (taxOfficeNumber, taxOfficeReference) =
      if (!request.isAgent) {
        extractTaxOfficeFromCisEnrolment(enrolments)
          .getOrElse(
            throw new IllegalStateException(
              "Missing CIS enrolment identifiers (TaxOfficeNumber/TaxOfficeReference) in HMRC-CIS-ORG"
            )
          )
      } else {
        // TODO this circumvents auth checks?
        (request.clientTaxOfficeNumber, request.clientTaxOfficeRef)
      }

    val periodEnd = parsePeriodEnd(request.monthYear)
    val sender    = if (request.isAgent) "Agent" else "Company"

    val cisReturn: Elem = CisReturnXmlBuilder.build(request)

    val envelopeXml: Elem =
      buildXml(
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestamp,
        periodEnd = periodEnd,
        sender = sender,
        cisReturn = cisReturn
      )

    val (updatedXML, irMarkBase64, irMarkBase32, irEnvelope) = UpdatedPayloadWithIrMark(envelopeXml.toString)

    val prettyXmlString = prettyPrinter.format(updatedXML)

    logger.debug(s"Chris Envelope XML:\n$prettyXmlString")
    logger.debug(s"[ChrisSubmissionEnvelopeBuilder] finalEnvelope: ${updatedXML.toString}")
    logger.debug(s"[ChrisSubmissionEnvelopeBuilder] irMarkBase64: $irMarkBase64")

    ChRISSubmission(
      envelope = updatedXML,
      correlationId = correlationId,
      irMark = irMarkBase64,
      irEnvelope = irEnvelope
    )
  }

  def extractTaxOfficeFromCisEnrolment(enrolments: Enrolments): Option[(String, String)] =
    enrolments
      .getEnrolment("HMRC-CIS-ORG")
      .flatMap { e =>
        for {
          ton <- e.getIdentifier("TaxOfficeNumber")
          tor <- e.getIdentifier("TaxOfficeReference")
        } yield (ton.value, tor.value)
      }

  def parsePeriodEnd(monthYear: String): String = {
    val ymTry =
      Try(YearMonth.parse(monthYear))
        .orElse(Try(YearMonth.parse(monthYear.replace('/', '-'))))

    val ym = ymTry.getOrElse {
      throw new IllegalArgumentException(s"Invalid monthYear: $monthYear (expected YYYY-MM)")
    }
    ym.atDay(5).format(isoDateFmt)
  }

  def buildPayload(
    submission: ChrisSubmissionRequest,
    request: AuthenticatedRequest[_]
  ): ChRISSubmission =
    buildChrisSubmission(submission, request.enrolments)
}
