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

      "maps 3001 with any type to departmentalError with code 3001" in {
        val error  = GovTalkError("3001", "fatal", "Submission failed due to business validation errors")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3001", "departmentalError", "Submission failed due to business validation errors")
      }

      "maps business type to departmentalError preserving the code" in {
        val error  = GovTalkError("3999", "business", "Business processing error")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3999", "departmentalError", "Business processing error")
      }

      "maps Department raisedBy to departmentalError preserving the code" in {
        val error  = GovTalkError("3998", "warning", "Department processing error", Some("Department"))
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3998", "departmentalError", "Department processing error")
      }

      "maps fatal type to systemError preserving the code" in {
        val error  = GovTalkError("3000", "fatal", "Fatal processing error")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3000", "systemError", "Fatal processing error")
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

      "does not treat upper-case BUSINESS as departmentalError" in {
        val error  = GovTalkError("3997", "BUSINESS", "Error text")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3997", "systemError", "Error text")
      }

      "does not treat upper-case FATAL as fatal" in {
        val error  = GovTalkError("3000", "FATAL", "Error text")
        val result = GovTalkErrorMapper.map(error)

        result mustBe GovTalkError("3000", "systemError", "Error text")
      }
    }

    "fromHttpTimeout" - {
      "returns code 500, type timeOut, message timeOut" in {
        val result = GovTalkErrorMapper.fromHttpTimeout()

        result mustBe GovTalkError("500", "timeOut", "timeOut")
      }

    }

    "fromInitialConnectionRefused" - {
      "returns code xxxx, type timeOut, message 'timed out'" in {
        val result = GovTalkErrorMapper.fromInitialConnectionRefused()

        result mustBe GovTalkError("xxxx", "timeOut", "timed out")
      }
    }

    "fromPollConnectionRefused" - {
      "returns code xxxx, type timeOut, message 'timeOut'" in {
        val result = GovTalkErrorMapper.fromPollConnectionRefused()

        result mustBe GovTalkError("xxxx", "timeOut", "timeOut")
      }
    }
  }
}
