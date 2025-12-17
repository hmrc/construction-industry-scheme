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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.constructionindustryscheme.connectors.{DatacacheProxyConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.*

import scala.concurrent.Future
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PrepopulationService @Inject()(
  monthlyReturnService: MonthlyReturnService,
  formp: FormpProxyConnector,
  datacache: DatacacheProxyConnector
)(implicit ec: ExecutionContext) {

  def prepopulateContractorKnownFacts(
    instanceId: String,
    employerRef: EmployerReference
  )(implicit hc: HeaderCarrier): Future[Unit] =
    monthlyReturnService.getCisTaxpayer(employerRef).flatMap { cis =>
      ensureContractorKnownFactsInFormp(instanceId, cis)
    }

  def getContractorScheme(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[ContractorScheme]] =
    formp.getContractorScheme(instanceId)
    
  private def ensureContractorKnownFactsInFormp(
    instanceId: String,
    cis: CisTaxpayer
  )(implicit hc: HeaderCarrier): Future[Unit] = {

    // AO reference = aoDistrict + aoPayType + aoCheckCode + aoReference
    val accountsOfficeRef: String =
      List(cis.aoDistrict, cis.aoPayType, cis.aoCheckCode, cis.aoReference)
        .flatten
        .mkString

    formp.getContractorScheme(instanceId).flatMap {
      case None =>
        val createParams = CreateContractorSchemeParams(
          instanceId = instanceId,
          accountsOfficeReference = accountsOfficeRef,
          taxOfficeNumber = cis.taxOfficeNumber,
          taxOfficeReference = cis.taxOfficeRef
        )

        formp.createContractorScheme(createParams).map(_ => ())

      case Some(existing) =>
        val needsUpdate =
          existing.accountsOfficeReference != accountsOfficeRef ||
            existing.taxOfficeNumber        != cis.taxOfficeNumber ||
            existing.taxOfficeReference     != cis.taxOfficeRef

        if (!needsUpdate) {
          Future.unit
        } else {

          val updateParams = UpdateContractorSchemeParams(
            schemeId = existing.schemeId,
            instanceId = instanceId,
            accountsOfficeReference = accountsOfficeRef,
            taxOfficeNumber = cis.taxOfficeNumber,
            taxOfficeReference = cis.taxOfficeRef,
            utr = existing.utr,
            name = existing.name,
            emailAddress = existing.emailAddress,
            prePopCount = existing.prePopCount,
            prePopSuccessful = existing.prePopSuccessful,
            version = existing.version
          )

          formp.updateContractorScheme(updateParams)
        }
    }
  }

  def prepopulateContractorAndSubcontractors(
    instanceId: String,
    employerRef: EmployerReference
  )(implicit hc: HeaderCarrier): Future[Unit] =
    monthlyReturnService.getCisTaxpayer(employerRef).flatMap { cis =>
      val accountsOfficeRef: String =
        List(cis.aoDistrict, cis.aoPayType, cis.aoCheckCode, cis.aoReference)
          .flatten
          .mkString

      val knownFacts = PrepopKnownFacts(
        taxOfficeNumber = cis.taxOfficeNumber,
        taxOfficeReference = cis.taxOfficeRef,
        agentOwnReference = accountsOfficeRef
      )

      for {
        maybeScheme <- formp.getContractorScheme(instanceId)
        existing <- maybeScheme match {
          case Some(s) => Future.successful(s)
          case None =>
            Future.failed(
              new IllegalStateException(
                s"No FORMP scheme found for instanceId=$instanceId; F1 must run first"
              )
            )
        }

        contractorPrepop <- datacache.getSchemePrepopByKnownFacts(knownFacts)
        subcontractors <- datacache.getSubcontractorsPrepopByKnownFacts(knownFacts)
        _ <- contractorPrepop match {

          case None =>
            val nextPrePopCount = Some(existing.prePopCount.getOrElse(0) + 1)
            val currentVersion = existing.version.getOrElse(0)

            val updated = UpdateContractorSchemeParams(
              schemeId = existing.schemeId,
              instanceId = instanceId,
              accountsOfficeReference = existing.accountsOfficeReference,
              taxOfficeNumber = existing.taxOfficeNumber,
              taxOfficeReference = existing.taxOfficeReference,
              emailAddress = existing.emailAddress,
              prePopCount = nextPrePopCount,
              prePopSuccessful = Some("N"),
              version = existing.version
            )

            for {
              _ <- formp.updateContractorScheme(updated)
              _ <- formp.updateSchemeVersion(UpdateSchemeVersionRequest(instanceId, currentVersion)).map(_ => ())
            } yield ()

          case Some(prepopBody) =>
            val name = prepopBody.schemeName
            val utr = Option(prepopBody.utr).filter(_.nonEmpty)

            val currentVersion = existing.version.getOrElse(0)
            val nextPrePopCount = existing.prePopCount.getOrElse(0) + 1

            val subTypes: Seq[SubcontractorType] =
              subcontractors.map(s => toSubcontractorType(s.subcontractorType))

            val req = ApplyPrepopulationRequest(
              schemeId = existing.schemeId,
              instanceId = instanceId,
              accountsOfficeReference = existing.accountsOfficeReference,
              taxOfficeNumber = existing.taxOfficeNumber,
              taxOfficeReference = existing.taxOfficeReference,
              utr = utr,
              name = name,
              emailAddress = existing.emailAddress,
              displayWelcomePage = existing.displayWelcomePage,
              prePopCount = nextPrePopCount,
              prePopSuccessful = "Y",
              version = currentVersion,
              subcontractorTypes = subTypes
            )

            // This is atomic in FORMP: Update_Scheme + Create_Subcontractor* + Update_Version_Number
            formp.applyPrepopulation(req).map(_ => ())
        }
      } yield ()
    }

  private def toSubcontractorType(raw: String): SubcontractorType =
    raw.trim.toUpperCase match {
      case "S" => SoleTrader
      case "C" => Company
      case "P" => Partnership
      case "T" => Trust
      case other =>
        throw new IllegalArgumentException(s"Unknown TAXPAYER_TYPE '$other'")
    }
}
