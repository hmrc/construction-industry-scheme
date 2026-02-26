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

import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.constructionindustryscheme.connectors.{DatacacheProxyConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{DeleteMonthlyReturnItemProxyRequest, DeleteMonthlyReturnItemRequest, GetMonthlyReturnForEditRequest, MonthlyReturnRequest, SelectedSubcontractorsRequest, SyncMonthlyReturnItemsRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.*
import uk.gov.hmrc.constructionindustryscheme.models.{CisTaxpayer, EmployerReference, MonthlyReturn, NilMonthlyReturnRequest, UnsubmittedMonthlyReturnStatus, UserMonthlyReturns}

import scala.concurrent.Future
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class MonthlyReturnService @Inject() (
  datacache: DatacacheProxyConnector,
  formp: FormpProxyConnector
)(implicit ec: ExecutionContext) {

  def getCisTaxpayer(employerReference: EmployerReference)(implicit hc: HeaderCarrier): Future[CisTaxpayer] =
    datacache.getCisTaxpayer(employerReference)

  def getAllMonthlyReturnsByCisId(cisId: String)(implicit hc: HeaderCarrier): Future[UserMonthlyReturns] =
    formp.getMonthlyReturns(cisId)

  def getUnsubmittedMonthlyReturns(
    cisId: String
  )(implicit hc: HeaderCarrier): Future[UnsubmittedMonthlyReturnsResponse] =
    formp.getUnsubmittedMonthlyReturns(cisId).map { unsubmitted =>
      UnsubmittedMonthlyReturnsResponse(
        unsubmittedCisReturns = unsubmitted.monthlyReturn.map { monthlyReturn =>
          UnsubmittedMonthlyReturnsRow(
            taxYear = monthlyReturn.taxYear,
            taxMonth = monthlyReturn.taxMonth,
            returnType = mapType(monthlyReturn.nilReturnIndicator),
            status = mapStatus(monthlyReturn.status),
            lastUpdate = monthlyReturn.lastUpdate
          )
        }
      )
    }

  def createNilMonthlyReturn(
    req: NilMonthlyReturnRequest
  )(implicit hc: HeaderCarrier): Future[CreateNilMonthlyReturnResponse] =
    formp.getMonthlyReturns(req.instanceId).flatMap { existing =>
      existing.monthlyReturnList.find(r => r.taxYear == req.taxYear && r.taxMonth == req.taxMonth) match {
        case Some(mr) =>
          mr.status match {
            case Some(s) => Future.successful(CreateNilMonthlyReturnResponse(status = s))
            case None    =>
              Future.failed(
                new IllegalStateException(
                  s"Existing monthly return has no status (instanceId=${req.instanceId}, taxYear=${req.taxYear}, taxMonth=${req.taxMonth})"
                )
              )
          }
        case None     =>
          formp.createNilMonthlyReturn(req)
      }
    }

  def updateNilMonthlyReturn(
    req: NilMonthlyReturnRequest
  )(implicit hc: HeaderCarrier): Future[Unit] =
    formp.updateNilMonthlyReturn(req)

  def createMonthlyReturn(req: MonthlyReturnRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formp.createMonthlyReturn(req)

  def getSchemeEmail(instanceId: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    formp.getSchemeEmail(instanceId)

  def getMonthlyReturnForEdit(request: GetMonthlyReturnForEditRequest)(implicit
    hc: HeaderCarrier
  ): Future[GetMonthlyReturnForEditResponse] =
    formp.getMonthlyReturnForEdit(request)

  def syncMonthlyReturnItems(request: SelectedSubcontractorsRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    for {
      edit                    <- formp.getMonthlyReturnForEdit(
                                   GetMonthlyReturnForEditRequest(
                                     instanceId = request.instanceId,
                                     taxYear = request.taxYear,
                                     taxMonth = request.taxMonth
                                   )
                                 )

      idToRef: Map[Long, Long] =
        edit.subcontractors
          .flatMap(subcontractor => subcontractor.subbieResourceRef.map(ref => subcontractor.subcontractorId -> ref))
          .toMap

      selectedIds: Seq[Long]   = request.selectedSubcontractorIds.distinct
      missingIds: Seq[Long]    = selectedIds.filterNot(idToRef.contains)

      _ <- if (missingIds.nonEmpty) {
             Future.failed(
               UpstreamErrorResponse(
                 message = s"Subcontractor IDs not found: ${missingIds.mkString(",")}",
                 statusCode = 400,
                 reportAs = 400
               )
             )
           } else Future.successful(())

      selectedResourceRefs = selectedIds.map(idToRef)

      existingResourceRefs = edit.monthlyReturnItems.flatMap(_.itemResourceReference).distinct

      toCreate = selectedResourceRefs.toSet.diff(existingResourceRefs.toSet).toSeq.sorted
      toDelete = existingResourceRefs.toSet.diff(selectedResourceRefs.toSet).toSeq.sorted

      _ <- formp.syncMonthlyReturnItems(
             SyncMonthlyReturnItemsRequest(
               instanceId = request.instanceId,
               taxYear = request.taxYear,
               taxMonth = request.taxMonth,
               amendment = "N",
               createResourceReferences = toCreate,
               deleteResourceReferences = toDelete
             )
           )
    } yield ()

  def deleteMonthlyReturnItem(request: DeleteMonthlyReturnItemRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    for {

      edit <- formp.getMonthlyReturnForEdit(
                GetMonthlyReturnForEditRequest(
                  instanceId = request.instanceId,
                  taxYear = request.taxYear,
                  taxMonth = request.taxMonth
                )
              )

      resourceRefToDelete <- edit.subcontractors
                               .flatMap(s => s.subbieResourceRef.map(ref => s.subcontractorId -> ref))
                               .toMap
                               .get(request.subcontractorId)
                               .fold[Future[Long]](
                                 Future.failed(
                                   UpstreamErrorResponse(
                                     message = s"Subcontractor ID not found: ${request.subcontractorId}",
                                     statusCode = 400,
                                     reportAs = 400
                                   )
                                 )
                               )(Future.successful)

      _ <- formp.deleteMonthlyReturnItem(
             DeleteMonthlyReturnItemProxyRequest(
               instanceId = request.instanceId,
               taxYear = request.taxYear,
               taxMonth = request.taxMonth,
               amendment = "N",
               resourceReference = resourceRefToDelete
             )
           )
    } yield ()

  private def mapType(nilReturnIndicator: Option[String]): String =
    if (nilReturnIndicator.exists(_.trim.equalsIgnoreCase("Y"))) "Nil"
    else "Standard"

  private def mapStatus(raw: Option[String]): String =
    UnsubmittedMonthlyReturnStatus.fromRaw(raw).asText
}
