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

import play.api.libs.json.{JsString, Reads}
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{ChrisVerificationRequest, VerificationDetails}
import uk.gov.hmrc.constructionindustryscheme.utils.Normalise.*

import scala.xml.{Elem, NodeSeq}

object CisVerificationRequestXmlBuilder {

  def build(
    request: ChrisVerificationRequest,
    subs: Seq[SubcontractorCurrentVerification],
    declaration: String = "yes"
  ): Elem = {

    val verificationByResourceRef: Map[String, VerificationDetails] =
      request.verifications.map(v => v.verificationResourceRef.trim -> v).toMap

    <CISrequest>
      {contractorNode(request.contractorUTR, request.contractorAORef)}
      {subs.map(sub => buildSubcontractor(sub, actionFor(sub, verificationByResourceRef)))}
      <Declaration>{declaration}</Declaration>
    </CISrequest>
  }

  private def contractorNode(utr: String, aoRef: String): Elem =
    <Contractor>
      <UTR>{utr}</UTR>
      <AOref>{aoRef}</AOref>
    </Contractor>

  private def buildSubcontractor(sub: SubcontractorCurrentVerification, action: VerificationAction): Elem = {
    val st: SubcontractorType = parseSubcontractorType(sub.subcontractorType)

    val worksRef: String = nonBlank(sub.worksReferenceNumber).getOrElse("")
    val utr: String      =
      st match {
        case Partnership =>
          nonBlank(sub.partnerUtr).getOrElse("")

        case SoleTrader | Company | Trust =>
          nonBlank(sub.utr).getOrElse("")
      }

    val actionValue = VerificationAction.toXmlValue(action)

    val hasTradingName = chooseTradingName(sub).isDefined

    val hasName =
      nonBlank(sub.firstName).isDefined ||
        nonBlank(sub.secondName).isDefined ||
        nonBlank(sub.surname).isDefined

    val nameNode: NodeSeq =
      st match {
        case SoleTrader if !hasTradingName && hasName =>
          buildName(sub).getOrElse(NodeSeq.Empty)
        case _                                        =>
          NodeSeq.Empty
      }

    val tradingNameNode: NodeSeq =
      chooseTradingName(sub)
        .map(tn => <TradingName>{tn}</TradingName>)
        .getOrElse(NodeSeq.Empty)

    val crnNode: NodeSeq =
      st match {
        case Partnership | Company =>
          nonBlank(sub.crn)
            .map(crn => <CRN>{crn}</CRN>)
            .getOrElse(NodeSeq.Empty)
        case _                     =>
          NodeSeq.Empty
      }

    val ninoNode: NodeSeq =
      st match {
        case SoleTrader | Partnership =>
          nonBlank(sub.nino)
            .map(nino => <NINO>{nino}</NINO>)
            .getOrElse(NodeSeq.Empty)
        case _                        =>
          NodeSeq.Empty
      }

    val partnershipNode: NodeSeq =
      st match {
        case Partnership =>
          buildPartnership(sub).getOrElse(NodeSeq.Empty)
        case _           =>
          NodeSeq.Empty
      }

    val addressNode: NodeSeq =
      buildAddress(sub).getOrElse(NodeSeq.Empty)

    <Subcontractor>
      <Action>{actionValue}</Action>
      <Type>{st.toString}</Type>
      {tradingNameNode}
      {nameNode}
      <WorksRef>{worksRef}</WorksRef>
      <UTR>{utr}</UTR>
      {crnNode}
      {ninoNode}
      {partnershipNode}
      {addressNode}
    </Subcontractor>
  }

  private def parseSubcontractorType(raw: Option[String]): SubcontractorType = {
    val value = nonBlank(raw).getOrElse(
      throw new IllegalArgumentException("Missing subcontractorType in SubcontractorCurrentVerification")
    )

    val reads = summon[Reads[SubcontractorType]]
    reads.reads(JsString(value)).getOrElse {
      throw new IllegalArgumentException(s"Invalid SubcontractorType value: $value")
    }
  }

  private def buildName(sub: SubcontractorCurrentVerification): Option[Elem] = {
    val fore = nonBlank(sub.firstName).orElse(nonBlank(sub.secondName))
    val sur  = nonBlank(sub.surname)

    if (fore.isEmpty && sur.isEmpty) None
    else {
      Some(
        <Name>
          <Fore>{fore.getOrElse("")}</Fore>
          <Sur>{sur.getOrElse("")}</Sur>
        </Name>
      )
    }
  }

  private def chooseTradingName(
    sub: SubcontractorCurrentVerification
  ): Option[String] =
    nonBlank(sub.tradingName)

  private def buildPartnership(sub: SubcontractorCurrentVerification): Option[Elem] = {
    val nameOpt = nonBlank(sub.partnershipTradingName)
    val utrOpt  = nonBlank(sub.utr)

    if (nameOpt.isEmpty && utrOpt.isEmpty) None
    else {
      Some(
        <Partnership>
          <Name>{nameOpt.getOrElse("")}</Name>
          <UTR>{utrOpt.getOrElse("")}</UTR>
        </Partnership>
      )
    }
  }

  private def buildAddress(sub: SubcontractorCurrentVerification): Option[Elem] = {
    val line1    = nonBlank(sub.addressLine1)
    val line2    = nonBlank(sub.addressLine2)
    val line3    = nonBlank(sub.addressLine3)
    val line4    = nonBlank(sub.addressLine4)
    val postcode = nonBlank(sub.postcode)
    val country  = nonBlank(sub.country)

    val hasAnyAddressField =
      Seq(line1, line2, line3, line4, postcode, country).exists(_.isDefined)

    if (!hasAnyAddressField) {
      None
    } else if (line1.isEmpty) {
      throw new IllegalArgumentException(
        s"ADDRESS_LINE_1 is mandatory when address fields are present for subcontractorId=${sub.subcontractorId}"
      )
    } else {
      Some(
        <Address>
          <Line>{line1.get}</Line>
          <Line>{line2.getOrElse("")}</Line>
          <Line>{line3.getOrElse("")}</Line>
          <Line>{line4.getOrElse("")}</Line>
          <PostCode>{postcode.getOrElse("")}</PostCode>
          <Country>{country.getOrElse("")}</Country>
        </Address>
      )
    }
  }

  private def actionFor(
    sub: SubcontractorCurrentVerification,
    verificationByResourceRef: Map[String, VerificationDetails]
  ): VerificationAction = {

    val maybeVerification =
      sub.subbieResourceRef
        .map(_.toString)
        .flatMap(verificationByResourceRef.get)

    maybeVerification match {
      case Some(v) if v.proceedVerification => Verify
      case _                                => Match
    }
  }
}
