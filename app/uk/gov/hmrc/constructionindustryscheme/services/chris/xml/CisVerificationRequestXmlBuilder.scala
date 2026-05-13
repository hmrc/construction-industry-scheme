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
import uk.gov.hmrc.constructionindustryscheme.utils.Normalise.*

import scala.xml.{Elem, NodeSeq}

object CisVerificationRequestXmlBuilder {

  def build(
    contractorUtr: String,
    contractorAoRef: String,
    subs: Seq[SubcontractorCurrentVerification],
    action: String,
    declaration: String = "yes"
  ): Elem =
    <CISrequest>
      {contractorNode(contractorUtr, contractorAoRef)}
      {subs.flatMap(sub => buildSubcontractor(sub, action))}
      <Declaration>{declaration}</Declaration>
    </CISrequest>

  private def contractorNode(utr: String, aoRef: String): Elem =
    <Contractor>
      <UTR>{utr}</UTR>
      <AOref>{aoRef}</AOref>
    </Contractor>

  private def buildSubcontractor(sub: SubcontractorCurrentVerification, action: String): NodeSeq = {
    val st: SubcontractorType = parseSubcontractorType(sub.subcontractorType)

    val worksRef: String = nonBlank(sub.worksReferenceNumber).getOrElse("")
    val utr: String      = nonBlank(sub.utr).getOrElse("")

    val addressNode: Elem = buildAddress(sub)

    val nameNode: NodeSeq =
      st match {
        case SoleTrader => buildName(sub)
        case _          => NodeSeq.Empty
      }

    val tradingNameNode: NodeSeq =
      st match {
        case SoleTrader => NodeSeq.Empty
        case _          =>
          chooseTradingName(st, sub)
            .map(tn => <TradingName>{tn}</TradingName>)
            .getOrElse(NodeSeq.Empty)
      }

    val crnNode: NodeSeq =
      st match {
        case Partnership | Company =>
          nonBlank(sub.crn)
            .map(crn => <CRN>{crn}</CRN>)
            .getOrElse(NodeSeq.Empty)
        case _                     => NodeSeq.Empty
      }

    val ninoNode: NodeSeq =
      st match {
        case SoleTrader | Partnership =>
          nonBlank(sub.nino)
            .map(nino => <NINO>{nino}</NINO>)
            .getOrElse(NodeSeq.Empty)
        case _                        => NodeSeq.Empty
      }

    val partnershipNode: NodeSeq =
      st match {
        case Partnership =>
          buildPartnership(sub).getOrElse(NodeSeq.Empty)
        case _           => NodeSeq.Empty
      }

    <Subcontractor>
      <Action>{action}</Action>
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

  private def buildName(sub: SubcontractorCurrentVerification): Elem = {
    val first  = nonBlank(sub.firstName).getOrElse("")
    val second = nonBlank(sub.secondName).getOrElse("")
    val sur    = nonBlank(sub.surname).getOrElse("")

    val fore = (first, second) match {
      case ("", "") => ""
      case (f, "")  => f
      case ("", s)  => s
      case (f, s)   => s"$f $s"
    }

    <Name>
      <Fore>{fore}</Fore>
      <Sur>{sur}</Sur>
    </Name>
  }

  private def chooseTradingName(st: SubcontractorType, sub: SubcontractorCurrentVerification): Option[String] =
    st match {
      case Partnership     =>
        nonBlank(sub.partnershipTradingName).orElse(nonBlank(sub.tradingName))
      case Company | Trust =>
        nonBlank(sub.tradingName).orElse(nonBlank(sub.partnershipTradingName))
      case SoleTrader      =>
        None
    }

  private def buildPartnership(sub: SubcontractorCurrentVerification): Option[Elem] = {
    val nameOpt = nonBlank(sub.partnershipTradingName)
    val utrOpt  = nonBlank(sub.partnerUtr)

    if (nameOpt.isEmpty && utrOpt.isEmpty) None
    else
      Some(
        <Partnership>
        <Name>{nameOpt.getOrElse("")}</Name>
        <UTR>{utrOpt.getOrElse("")}</UTR>
      </Partnership>
      )
  }

  private def buildAddress(sub: SubcontractorCurrentVerification): Elem = {
    val lines: Seq[String] =
      Seq(sub.addressLine1, sub.addressLine2, sub.addressLine3, sub.addressLine4)
        .map(v => nonBlank(v).getOrElse(""))

    val postcode = nonBlank(sub.postcode).getOrElse("")
    val country  = nonBlank(sub.country).getOrElse("")

    <Address>
      {
      lines.map(l => <Line>{l}</Line>)
    }<PostCode>{postcode}</PostCode>
      <Country>{country}</Country>
    </Address>
  }
}
