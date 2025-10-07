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
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, EmployerReference, NilMonthlyReturnRequest, UserMonthlyReturns}

import scala.concurrent.Future
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MonthlyReturnService @Inject()(
                                      datacache: DatacacheProxyConnector,
                                      formp: FormpProxyConnector
                                    )(implicit ec: ExecutionContext) {

  def getCisTaxpayer(employerReference: EmployerReference)(implicit hc: HeaderCarrier): Future[CisTaxpayer] =
    datacache.getCisTaxpayer(employerReference)

  def getAllMonthlyReturnsByCisId(cisId: String)(implicit hc: HeaderCarrier): Future[UserMonthlyReturns] =
    formp.getMonthlyReturns(cisId)

  def createNilMonthlyReturn(req: NilMonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      existing <- formp.getMonthlyReturns(req.instanceId)
      duplicateExists = existing.monthlyReturnList.exists(r => r.taxYear == req.taxYear && r.taxMonth == req.taxMonth)
      schemeVersion = existing.schemeVersion.getOrElse(0)
      _ <- if (duplicateExists) Future.unit
           else for {
             _        <- formp.createMonthlyReturn(req)
             newVer   <- formp.updateSchemeVersion(req.instanceId, schemeVersion)
             _        <- formp.updateMonthlyReturn(req, newVer)
           } yield ()
    } yield ()
}

