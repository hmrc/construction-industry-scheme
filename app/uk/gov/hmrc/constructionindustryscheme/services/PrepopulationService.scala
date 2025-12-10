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
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, CreateContractorSchemeParams, EmployerReference, UpdateContractorSchemeParams}
import scala.concurrent.Future
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class PrepopulationService @Inject()(
  monthlyReturnService: MonthlyReturnService,
  formp: FormpProxyConnector
)(implicit ec: ExecutionContext) {

  def prepopulateContractorKnownFacts(
    instanceId: String,
    employerRef: EmployerReference
  )(implicit hc: HeaderCarrier): Future[Unit] =
    monthlyReturnService.getCisTaxpayer(employerRef).flatMap { cis =>
      ensureContractorKnownFactsInFormp(instanceId, cis)
    }

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
          taxOfficeReference = cis.taxOfficeRef,
          utr = cis.utr,
          name = cis.schemeName
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
}