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

package models

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.ChrisDeleteRequest
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeConstants

class ChrisDeleteRequestSpec extends AnyWordSpec with Matchers {

  "ChrisDeleteRequest.payload" should {

    "build GovTalk delete payload with correlationId" in {
      val correlationId = "ABC123"
      val request       = ChrisDeleteRequest(correlationId)

      val xml = request.payload

      xml.label mustBe "GovTalkMessage"
      (xml \\ "CorrelationID").text mustBe correlationId
      (xml \\ "Function").text mustBe ChrisEnvelopeConstants.DeleteFunction
      (xml \\ "EnvelopeVersion").text mustBe "2.0"
    }

  }
}
