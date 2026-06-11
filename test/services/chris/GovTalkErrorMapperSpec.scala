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

package services.chris

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.models.GovTalkError
import uk.gov.hmrc.constructionindustryscheme.services.chris.GovTalkErrorMapper

final class GovTalkErrorMapperSpec extends AnyFreeSpec with Matchers {

  "GovTalkErrorMapper" - {

    "normalise" - {

      "maps 3001/business to departmentalError with code 3001" in {
        val error  = GovTalkError("3001", "business", "Submission failed due to business validation errors")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3001", "departmentalError", "Submission failed due to business validation errors")
      }

      "maps 3000/fatal to departmentalError with code 3001 (remapped)" in {
        val error  = GovTalkError("3000", "fatal", "Fatal processing error")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3001", "departmentalError", "Fatal processing error")
      }

      "maps other error codes to systemError with actual code" in {
        val error  = GovTalkError("1020", "fatal", "Gateway error")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("1020", "systemError", "Gateway error")
      }

      "maps non-numeric error code to systemError preserving the code" in {
        val error  = GovTalkError("UNKNOWN", "fatal", "Unknown error")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("UNKNOWN", "systemError", "Unknown error")
      }

      "is case insensitive on error type matching" in {
        val error  = GovTalkError("3001", "BUSINESS", "Error text")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3001", "departmentalError", "Error text")
      }

      "is case insensitive on error type matching for 3000/fatal" in {
        val error  = GovTalkError("3000", "FATAL", "Error text")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3001", "departmentalError", "Error text")
      }
    }

    "fromHttpTimeout" - {
      "returns code 500, type timeOut, message timeOut" in {
        val result = GovTalkErrorMapper.fromHttpTimeout()

        result mustBe GovTalkError("500", "timeOut", "timeOut")
      }
    }

    "fromConnectionRefused" - {
      "returns code 500, type timeOut, message 'timed out'" in {
        val result = GovTalkErrorMapper.fromConnectionRefused()

        result mustBe GovTalkError("500", "timeOut", "timed out")
      }
    }
  }
}
