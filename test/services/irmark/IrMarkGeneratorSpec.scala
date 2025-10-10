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

package services.irmark

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.services.irmark.IrMarkGenerator

class IrMarkGeneratorSpec extends SpecBase {

  private val validElem = scala.xml.XML.load(getClass.getResource("/irmark/ValidCisReturnEnvelope.xml"))
  private val invalidElem = scala.xml.XML.load(getClass.getResource("/irmark/InvalidCisReturnEnvelope.xml"))
  private val correctEnvelopeNamespace = "http://www.govtalk.gov.uk/taxation/CISreturn"
  private val incorrectEnvelopeNamespace = "http://www.govtalk.gov.uk/taxation/Incorrect"

  "generateIrMark" - {

    "generateIrMark successfully generates IrMark for a valid elem and correct EnvelopeNamespace" in {
      val irMark = IrMarkGenerator.generateIrMark(validElem, correctEnvelopeNamespace)
      irMark mustBe "j1Ap+GqVf7bJwdgOfj+p3dhnG8g="
    }

    "generateIrMark throws IllegalArgumentException for a valid elem and incorrect EnvelopeNamespace" in {
      assertThrows[IllegalArgumentException] {
        IrMarkGenerator.generateIrMark(validElem, incorrectEnvelopeNamespace)
      }
    }

    "generateIrMark throws IllegalArgumentException for an invalid elem and correct EnvelopeNamespace" in {
      assertThrows[IllegalArgumentException] {
        IrMarkGenerator.generateIrMark(invalidElem, correctEnvelopeNamespace)
      }
    }
  }

}
