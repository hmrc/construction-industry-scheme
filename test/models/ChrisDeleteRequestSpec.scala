package models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.ChrisDeleteRequest
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeConstants

class ChrisDeleteRequestSpec extends AnyWordSpec with Matchers {

  "ChrisDeleteRequest.payload" should {

    "build GovTalk delete payload with correlationId" in {
      val correlationId = "ABC123"
      val request = ChrisDeleteRequest(correlationId)

      val xml = request.payload

      xml.label mustBe "GovTalkMessage"
      (xml \\ "CorrelationID").text mustBe correlationId
      (xml \\ "Function").text mustBe ChrisEnvelopeConstants.DeleteFunction
      (xml \\ "EnvelopeVersion").text mustBe "2.0"
    }

  }
}
