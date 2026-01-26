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

import base.SpecBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.constructionindustryscheme.models.BuiltSubmissionPayload

import scala.xml.Elem

class BuiltSubmissionPayloadSpec extends SpecBase {

  "BuiltSubmissionPayload" - {

    "correctly construct and compare instances" in {
      val envelope: Elem   = <envelope><id>1</id></envelope>
      val irEnvelope: Elem = <irEnvelope><mark>X</mark></irEnvelope>
      val payload1         = BuiltSubmissionPayload(
        envelope = envelope,
        correlationId = "abc123",
        irMark = "mark456",
        irEnvelope = irEnvelope
      )
      val payload2         = BuiltSubmissionPayload(
        envelope = envelope,
        correlationId = "abc123",
        irMark = "mark456",
        irEnvelope = irEnvelope
      )

      payload1.envelope      shouldBe envelope
      payload1.correlationId shouldBe "abc123"
      payload1.irMark        shouldBe "mark456"
      payload1.irEnvelope    shouldBe irEnvelope

      payload1 shouldBe payload2
    }

  }
}
