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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.{UpdateContractorSchemeRequest, UpdateContractorSchemeVersionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.UpdateContractorSchemeVersionResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

@Singleton
class ContractorSchemeService @Inject() (formpProxyConnector: FormpProxyConnector) {

  def updateScheme(request: UpdateContractorSchemeRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateContractorSchemeDetails(request)

  def updateSchemeVersion(
    request: UpdateContractorSchemeVersionRequest
  )(implicit hc: HeaderCarrier): Future[UpdateContractorSchemeVersionResponse] =
    formpProxyConnector.updateContractorSchemeVersion(request)
}
