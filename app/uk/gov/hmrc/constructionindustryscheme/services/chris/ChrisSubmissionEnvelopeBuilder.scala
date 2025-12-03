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

package uk.gov.hmrc.constructionindustryscheme.services.chris

import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.constructionindustryscheme.models.BuiltSubmissionPayload
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedRequest, ChrisSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.services.irmark.IrMarkGenerator

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, YearMonth, ZoneOffset}
import scala.util.Try
import scala.xml.{Elem, NodeSeq, PrettyPrinter, XML}
import uk.gov.hmrc.constructionindustryscheme.utils.XxeHelper.secureSAXParser

import scala.xml.*

object ChrisSubmissionEnvelopeBuilder extends IrMarkGenerator {
  private val gatewayTimestampFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
  private val isoDateFmt = DateTimeFormatter.ISO_LOCAL_DATE
  private val prettyPrinter = new PrettyPrinter(120, 4)

  def build(
             request: ChrisSubmissionRequest,
             authRequest: AuthenticatedRequest[_],
             correlationId: String
           ): Elem = {

    val gatewayTimestamp = LocalDateTime.now(ZoneOffset.UTC).format(gatewayTimestampFormatter)

    val (taxOfficeNumber, taxOfficeReference) =
      extractTaxOfficeFromCisEnrolment(authRequest.enrolments)
        .getOrElse(throw new IllegalStateException(
          "Missing CIS enrolment identifiers (TaxOfficeNumber/TaxOfficeReference) in HMRC-CIS-ORG"
        ))

    val periodEnd = parsePeriodEnd(request.monthYear)

    val envelopeXml: Elem =
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
              <Sender>{ChrisEnvelopeConstants.Sender}</Sender>
            </IRheader>

            <CISreturn>
              <Contractor>
                <UTR>{request.utr}</UTR>
                <AOref>{request.aoReference}</AOref>
              </Contractor>
              <NilReturn>{ChrisEnvelopeConstants.NilReturn}</NilReturn>
              <Declarations>
                <InformationCorrect>{request.informationCorrect}</InformationCorrect>
                { if (request.inactivity.equalsIgnoreCase("yes")) <Inactivity>yes</Inactivity> else NodeSeq.Empty }
              </Declarations>
            </CISreturn>
          </IRenvelope>
        </Body>
      </GovTalkMessage>

    val irMarkBase64 = generateIrMark(envelopeXml, ChrisEnvelopeConstants.Namespace)
    val prettyXmlString = prettyPrinter.format(replaceIrMark(envelopeXml, irMarkBase64))

    logger.info(s"Chris Envelope XML:\n$prettyXmlString")
    XML.loadString(prettyXmlString)
  }

  private def replaceIrMark(root: Elem, irMark: String) = {
    val out = root.toString()
    val subbed = out.replaceFirst( """<IRmark Type="generic">TBC</IRmark>""", s"""<IRmark Type="generic">$irMark</IRmark>""")
    XML.withSAXParser(secureSAXParser).loadString(subbed)
  }

  private def extractTaxOfficeFromCisEnrolment(enrolments: Enrolments): Option[(String, String)] =
    enrolments
      .getEnrolment("HMRC-CIS-ORG")
      .flatMap { e =>
        for {
          ton <- e.getIdentifier("TaxOfficeNumber")
          tor <- e.getIdentifier("TaxOfficeReference")
        } yield (ton.value, tor.value)
      }

  private def parsePeriodEnd(monthYear: String): String = {
    val ymTry =
      Try(YearMonth.parse(monthYear))
        .orElse(Try(YearMonth.parse(monthYear.replace('/', '-'))))

    val ym = ymTry.getOrElse {
      throw new IllegalArgumentException(s"Invalid monthYear: $monthYear (expected YYYY-MM)")
    }
    ym.atDay(5).format(isoDateFmt)
  }

  def buildPayload(
                    request: ChrisSubmissionRequest,
                    authRequest: AuthenticatedRequest[_],
                    correlationId: String
                  ): BuiltSubmissionPayload = {
    val finalEnvelope: scala.xml.Elem = build(request, authRequest, correlationId)
    val irMarkBase64: String = (finalEnvelope \\ "IRmark").text.trim

    BuiltSubmissionPayload(
      envelope = finalEnvelope,
      correlationId = correlationId,
      irMark = irMarkBase64
    )
  }
}