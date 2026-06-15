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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.GovTalkErrorStatus.*
import uk.gov.hmrc.constructionindustryscheme.services.chris.GovTalkErrorStatusClassifier

class GovTalkErrorStatusClassifierSpec extends AnyWordSpec with Matchers {

  "fromXmlOutcome" should {

    "classify DEPARTMENTAL_ERROR as DepartmentalError" in {
      val err = GovTalkError("3001", "business", "departmental boom")

      GovTalkErrorStatusClassifier.fromXmlOutcome(DEPARTMENTAL_ERROR, Some(err)) mustBe DepartmentalError(
        "departmental boom"
      )
    }

    List("3000", "2005", "1000").foreach { code =>
      s"classify STARTED + recoverable code $code as RecoverableError" in {
        val err = GovTalkError(code, "fatal", s"recoverable $code")

        GovTalkErrorStatusClassifier.fromXmlOutcome(STARTED, Some(err)) mustBe RecoverableError(
          code,
          s"recoverable $code"
        )
      }

      s"classify FATAL_ERROR + recoverable code $code as RecoverableError" in {
        val err = GovTalkError(code, "fatal", s"recoverable $code")

        GovTalkErrorStatusClassifier.fromXmlOutcome(FATAL_ERROR, Some(err)) mustBe RecoverableError(
          code,
          s"recoverable $code"
        )
      }
    }

    "classify FATAL_ERROR with a non-recoverable code as FatalError" in {
      val err = GovTalkError("9999", "fatal", "fatal boom")

      GovTalkErrorStatusClassifier.fromXmlOutcome(FATAL_ERROR, Some(err)) mustBe FatalError("9999", "fatal boom")
    }

    "classify FATAL_ERROR with no error payload as OtherStatus" in {
      GovTalkErrorStatusClassifier.fromXmlOutcome(FATAL_ERROR, None) mustBe OtherStatus
    }

    "classify any other status as OtherStatus" in {
      GovTalkErrorStatusClassifier.fromXmlOutcome(ACCEPTED, None) mustBe OtherStatus
      GovTalkErrorStatusClassifier.fromXmlOutcome(SUBMITTED, None) mustBe OtherStatus
    }
  }

  "fromHttpStatus" should {

    (500 to 505).foreach { s =>
      s"classify HTTP $s as ServerError($s)" in {
        GovTalkErrorStatusClassifier.fromHttpStatus(s) mustBe ServerError(s)
      }
    }

    "classify non-5xx HTTP statuses as OtherStatus" in {
      List(200, 301, 400, 404, 499, 506, 599).foreach { s =>
        GovTalkErrorStatusClassifier.fromHttpStatus(s) mustBe OtherStatus
      }
    }
  }

  "noResponse" should {
    "always equal NoResponse" in {
      GovTalkErrorStatusClassifier.noResponse mustBe NoResponse
    }
  }
}
