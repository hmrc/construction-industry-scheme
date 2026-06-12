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
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.CisVerificationRequestXmlBuilder
import uk.gov.hmrc.constructionindustryscheme.services.chris.{EnvelopeProfile, GovTalkEnvelopeBuilder}
import uk.gov.hmrc.constructionindustryscheme.utils.CisEnrolmentHelper.extractTaxOfficeIdentifiers
import uk.gov.hmrc.constructionindustryscheme.utils.IrMarkProcessor.UpdatedPayloadWithIrMark

import java.time.format.DateTimeFormatter
import java.time.{Clock, LocalDate, LocalDateTime, YearMonth, ZoneId}
import java.util.UUID
import scala.xml.{Elem, PrettyPrinter}

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

  def buildPayload(
    request: ChrisVerificationRequest,
    enrolments: Enrolments,
    clock: Clock = Clock.systemUTC()
  ): CisVerificationSubmission = {

    val correlationId    = UUID.randomUUID().toString.replace("-", "").toUpperCase
    val gatewayTimestamp = LocalDateTime.now(ZoneId.of("Europe/London")).format(gatewayTimestampFormatter)

    val periodEnd = YearMonth
      .from(LocalDate.now(clock))
      .atEndOfMonth()
      .format(isoDateFmt)
    val sender    = if (request.isAgent) "Agent" else "Company"

    val cisRequest: Elem =
      CisVerificationRequestXmlBuilder.build(
        request = request,
        subs = request.subcontractors
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
      GovTalkEnvelopeBuilder.build(
        profile = EnvelopeProfile.Verification,
        taxOfficeNumber = taxOfficeNumber,
        taxOfficeReference = taxOfficeReference,
        correlationId = correlationId,
        gatewayTimestamp = gatewayTimestamp,
        periodEnd = periodEnd,
        sender = sender,
        payload = cisRequest
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
