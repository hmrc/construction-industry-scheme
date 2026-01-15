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
import uk.gov.hmrc.constructionindustryscheme.models.requests.MonthlyReturnRequest
import uk.gov.hmrc.constructionindustryscheme.models.response.CreateNilMonthlyReturnResponse
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UserMonthlyReturns}

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

  def createNilMonthlyReturn(req: NilMonthlyReturnRequest)
                            (implicit hc: HeaderCarrier): Future[CreateNilMonthlyReturnResponse] =
    formp.getMonthlyReturns(req.instanceId).flatMap { existing =>
      existing.monthlyReturnList.find(r => r.taxYear == req.taxYear && r.taxMonth == req.taxMonth) match {
        case Some(mr) =>
          mr.status match {
            case Some(s) => Future.successful(CreateNilMonthlyReturnResponse(status = s))
            case None =>
              Future.failed(new IllegalStateException(
                s"Existing monthly return has no status (instanceId=${req.instanceId}, taxYear=${req.taxYear}, taxMonth=${req.taxMonth})"
              ))
          }
        case None =>
          formp.createNilMonthlyReturn(req)
      }
    }

  def createMonthlyReturn(req: MonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formp.createMonthlyReturn(req)  

  def getSchemeEmail(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    formp.getSchemeEmail(instanceId)

}

