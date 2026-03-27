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

import uk.gov.hmrc.constructionindustryscheme.models.{ChrisPersonName, ChrisStandardMonthlyReturn, ChrisStandardSubcontractor, MonthlyReturnType, Partnership, SoleTrader, SubcontractorType}
import uk.gov.hmrc.constructionindustryscheme.models.requests.ChrisSubmissionRequest
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeConstants
import uk.gov.hmrc.constructionindustryscheme.utils.Normalise.*

import scala.xml.{Elem, NodeSeq}

object CisReturnXmlBuilder {

  def build(request: ChrisSubmissionRequest): Elem =
    request.returnType match {
      case MonthlyReturnType.Nil      => buildNil(request)
      case MonthlyReturnType.Standard => buildStandard(request)
    }

  private def buildNil(request: ChrisSubmissionRequest): Elem = {
    val contractor = contractorNode(request)

    <CISreturn>
      {contractor}
      <NilReturn>{ChrisEnvelopeConstants.NilReturn}</NilReturn>
      <Declarations>
        <InformationCorrect>{request.informationCorrect}</InformationCorrect>
        {if (isYes(request.inactivity)) <Inactivity>yes</Inactivity> else NodeSeq.Empty}
      </Declarations>
    </CISreturn>
  }

  private def buildStandard(request: ChrisSubmissionRequest): Elem = {
    val contractor                           = contractorNode(request)
    val standard: ChrisStandardMonthlyReturn = request.standard.getOrElse(
      throw new IllegalArgumentException("standard payload is required when returnType=standard")
    )

    if (standard.subcontractors.isEmpty) {
      throw new IllegalArgumentException("At least one subcontractor is required for standard monthly return")
    }

    val declarations = standard.declarations

    <CISreturn>
      {contractor}
      {standard.subcontractors.flatMap(buildStandardSubcontractor)}
      <Declarations>
        <EmploymentStatus>{declarations.employmentStatus}</EmploymentStatus>
        <Verification>{declarations.verification}</Verification>
        <InformationCorrect>{request.informationCorrect}</InformationCorrect>
        {if (isYes(request.inactivity)) <Inactivity>yes</Inactivity> else NodeSeq.Empty}
      </Declarations>
    </CISreturn>
  }

  private def contractorNode(request: ChrisSubmissionRequest): Elem =
    <Contractor>
      <UTR>{request.utr}</UTR>
      <AOref>{request.aoReference}</AOref>
    </Contractor>

  private def buildStandardSubcontractor(sub: ChrisStandardSubcontractor): NodeSeq = {
    val nameNode: NodeSeq =
      if (sub.subcontractorType == SoleTrader && isBlank(sub.tradingName)) {
        sub.name.map(buildName).getOrElse(NodeSeq.Empty)
      } else NodeSeq.Empty

    val tradingNameNode: NodeSeq =
      buildTradingName(sub.subcontractorType, sub.tradingName, sub.partnershipTradingName)

    val utrOrUnmatched: NodeSeq =
      nonBlank(sub.utr)
        .map(utr => <UTR>{utr}</UTR>)
        .getOrElse(<UnmatchedRate>yes</UnmatchedRate>)

    val crnNode: NodeSeq =
      if (sub.subcontractorType == Partnership) sub.crn.map(crn => <CRN>{crn}</CRN>).getOrElse(NodeSeq.Empty)
      else NodeSeq.Empty

    val ninoNode: NodeSeq =
      if (sub.subcontractorType == Partnership) sub.nino.map(nino => <NINO>{nino}</NINO>).getOrElse(NodeSeq.Empty)
      else NodeSeq.Empty

    val verificationNode: NodeSeq =
      sub.verificationNumber.map(vn => <VerificationNumber>{vn}</VerificationNumber>).getOrElse(NodeSeq.Empty)

    val totalPayments   = amountOrZero(sub.totalPayments)
    val costOfMaterials = amountOrZero(sub.costOfMaterials)
    val totalDeducted   = amountOrZero(sub.totalDeducted)

    <Subcontractor>
      {nameNode}
      {tradingNameNode}
      {utrOrUnmatched}
      {crnNode}
      {ninoNode}
      {verificationNode}
      <TotalPayments>{totalPayments}</TotalPayments>
      <CostOfMaterials>{costOfMaterials}</CostOfMaterials>
      <TotalDeducted>{totalDeducted}</TotalDeducted>
    </Subcontractor>
  }

  private def buildName(name: ChrisPersonName): Elem = {
    val middleFore: NodeSeq = nonBlank(name.middle).map(middle => <Fore>{middle}</Fore>).getOrElse(NodeSeq.Empty)

    <Name>
      <Fore>{name.first.trim}</Fore>
      {middleFore}
      <Sur>{name.last.trim}</Sur>
    </Name>
  }

  private def buildTradingName(
    subcontractorType: SubcontractorType,
    tradingName: Option[String],
    partnershipTradingName: Option[String]
  ): NodeSeq = {
    val chosenTradingName: Option[String] = subcontractorType match {
      case Partnership => nonBlank(partnershipTradingName)
      case _           => nonBlank(tradingName)
    }

    chosenTradingName.map(name => <TradingName>{name}</TradingName>).getOrElse(NodeSeq.Empty)
  }

  private def amountOrZero(maybe: Option[BigDecimal]): String =
    maybe.getOrElse(BigDecimal(0)).setScale(2, BigDecimal.RoundingMode.HALF_UP).toString()

}
