package services

import base.SpecBase
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateAmendedMonthlyReturnRequest
import uk.gov.hmrc.constructionindustryscheme.services.AmendMonthlyReturnService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class AmendMonthlyReturnServiceSpec extends SpecBase {

  "AmendMonthlyReturnService.createAmendedMonthlyReturn" - {

    "must delegate to FormpProxyConnector" in {
      val mockConnector = mock[FormpProxyConnector]

      val request = CreateAmendedMonthlyReturnRequest(
        instanceId = "1",
        taxYear = 2025,
        taxMonth = 1,
        version = 0
      )

      when(mockConnector.createAmendedMonthlyReturn(any[CreateAmendedMonthlyReturnRequest])(any[HeaderCarrier]))
        .thenReturn(Future.successful(()))

      val service = new AmendMonthlyReturnService(mockConnector)

      service.createAmendedMonthlyReturn(request).futureValue mustBe ((): Unit)

      verify(mockConnector).createAmendedMonthlyReturn(
        any[CreateAmendedMonthlyReturnRequest]
      )(any[HeaderCarrier])
    }
  }
}
