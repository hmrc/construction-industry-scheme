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

package models.response

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.ACCEPTED
import uk.gov.hmrc.constructionindustryscheme.models.response.ChrisPollResponse

class ChrisPollResponseSpec extends AnyWordSpec with Matchers {
  "ChrisPollResponse" should {
    "create an instance with the expected values" in {
      val response = ChrisPollResponse(
        status = ACCEPTED,
        correlationId = "corr-123",
        pollUrl = Some("/poll/123"),
        pollInterval = Some(10),
        error = None,
        irMarkReceived = None,
        lastMessageDate = Some("2025-01-01T00:00:00Z"),
        acceptedTime = None
      )

      response.status mustBe ACCEPTED
      response.correlationId mustBe "corr-123"
      response.pollUrl mustBe Some("/poll/123")
      response.pollInterval mustBe Some(10)
      response.lastMessageDate mustBe Some("2025-01-01T00:00:00Z")
    }
  }
}
