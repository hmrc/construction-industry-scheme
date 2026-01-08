package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionRequest, SubcontractorCreateRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.SubcontractorCreateResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorService @Inject()(
                                      formpProxyConnector: FormpProxyConnector
                                    )(implicit ec: ExecutionContext) {

  def createSubcontractor(request: SubcontractorCreateRequest)(implicit hc: HeaderCarrier): Future[SubcontractorCreateResponse] =
    formpProxyConnector.createSubcontractor(request)
}
