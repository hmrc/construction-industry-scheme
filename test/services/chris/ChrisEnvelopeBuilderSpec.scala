/*
 * Copyright 2025 HM Revenue & Customs
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

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.services.chris.ChrisEnvelopeBuilder

import scala.xml.Elem

class ChrisEnvelopeBuilderSpec extends SpecBase {
  
  "enableMissingMandatory=true removes <EnvelopeVersion>" in {
    val req  = createChrisRequest()
    val auth = createAuthReq()
    val corr = "CID-MISS-MAND"

    val xml: Elem =
      ChrisEnvelopeBuilder.build(
        request                = req,
        authRequest            = auth,
        correlationId          = corr,
        enableMissingMandatory = true,
        enableIrmarkBad        = false
      )

    (xml \\ "EnvelopeVersion") mustBe empty
    (xml \\ "IRmark").text.trim.length must be > 0
  }

  "enableMissingMandatory=false keeps <EnvelopeVersion>2.0</EnvelopeVersion>" in {
    val req  = createChrisRequest()
    val auth = createAuthReq()
    val corr = "CID-KEEP-MAND"

    val xml: Elem =
      ChrisEnvelopeBuilder.build(
        request                = req,
        authRequest            = auth,
        correlationId          = corr,
        enableMissingMandatory = false,
        enableIrmarkBad        = false
      )

    (xml \\ "EnvelopeVersion").map(_.text.trim) mustBe Seq("2.0")
  }

  "enableIrmarkBad=true corrupts only the first character of IRMark (length unchanged)" in {
    val req  = createChrisRequest()
    val auth = createAuthReq()

    val good: Elem =
      ChrisEnvelopeBuilder.build(
        request                = req,
        authRequest            = auth,
        correlationId          = "CID-GOOD",
        enableMissingMandatory = false,
        enableIrmarkBad        = false
      )
    val goodIR = (good \\ "IRmark").text.trim
    goodIR.length must be > 0

    val bad: Elem =
      ChrisEnvelopeBuilder.build(
        request                = req,
        authRequest            = auth,
        correlationId          = "CID-BAD",
        enableMissingMandatory = false,
        enableIrmarkBad        = true
      )
    val badIR = (bad \\ "IRmark").text.trim

    badIR.length mustBe goodIR.length
    badIR.head must not equal goodIR.head
    badIR.tail mustBe goodIR.tail
  }

  "buildPayload returns correlationId and IRMark matching the envelope" in {
    val req  = createChrisRequest()
    val auth = createAuthReq()
    val corr = "CID-PAYLOAD"

    val payload =
      ChrisEnvelopeBuilder.buildPayload(
        request                = req,
        authRequest            = auth,
        correlationId          = corr,
        enableMissingMandatory = false,
        enableIrmarkBad        = false
      )

    payload.correlationId mustBe corr
    val irFromXml = (payload.envelope \\ "IRmark").text.trim
    payload.irMark mustBe irFromXml
  }
}
