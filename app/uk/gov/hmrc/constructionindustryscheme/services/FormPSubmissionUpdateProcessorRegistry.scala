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

package uk.gov.hmrc.constructionindustryscheme.services

import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney
import javax.inject.{Inject, Singleton}

@Singleton
class FormPSubmissionUpdateProcessorRegistry @Inject() (
  monthlyReturnProcessor: MonthlyReturnFormPUpdateProcessor,
  verificationProcessor: VerificationFormPUpdateProcessor
) {

  private val processors: Map[ChrisPollJourney, FormPSubmissionUpdateProcessor] =
    Seq(monthlyReturnProcessor, verificationProcessor).map(p => p.journey -> p).toMap

  def processorFor(journey: ChrisPollJourney): FormPSubmissionUpdateProcessor =
    processors.getOrElse(
      journey,
      throw new IllegalArgumentException(s"No processor registered for journey: $journey")
    )
}
