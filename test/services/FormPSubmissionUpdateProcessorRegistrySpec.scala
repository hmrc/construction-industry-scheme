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

package services

import base.SpecBase
import org.mockito.Mockito.when
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney
import uk.gov.hmrc.constructionindustryscheme.services.{FormPSubmissionUpdateProcessorRegistry, MonthlyReturnFormPUpdateProcessor, VerificationFormPUpdateProcessor}

class FormPSubmissionUpdateProcessorRegistrySpec extends SpecBase {

  "FormPSubmissionUpdateProcessorRegistry" - {

    "return monthly return processor for MonthlyReturn journey" in {
      val monthlyReturnProcessor = mock[MonthlyReturnFormPUpdateProcessor]
      val verificationProcessor  = mock[VerificationFormPUpdateProcessor]

      when(monthlyReturnProcessor.journey).thenReturn(ChrisPollJourney.MonthlyReturn)
      when(verificationProcessor.journey).thenReturn(ChrisPollJourney.Verification)

      val registry = new FormPSubmissionUpdateProcessorRegistry(
        monthlyReturnProcessor,
        verificationProcessor
      )

      registry.processorFor(ChrisPollJourney.MonthlyReturn) mustBe monthlyReturnProcessor
    }

    "return verification processor for Verification journey" in {
      val monthlyReturnProcessor = mock[MonthlyReturnFormPUpdateProcessor]
      val verificationProcessor  = mock[VerificationFormPUpdateProcessor]

      when(monthlyReturnProcessor.journey).thenReturn(ChrisPollJourney.MonthlyReturn)
      when(verificationProcessor.journey).thenReturn(ChrisPollJourney.Verification)

      val registry = new FormPSubmissionUpdateProcessorRegistry(
        monthlyReturnProcessor,
        verificationProcessor
      )

      registry.processorFor(ChrisPollJourney.Verification) mustBe verificationProcessor
    }
  }
}
