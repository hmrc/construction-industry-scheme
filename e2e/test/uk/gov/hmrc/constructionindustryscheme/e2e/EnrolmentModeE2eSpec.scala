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

package uk.gov.hmrc.constructionindustryscheme.e2e

/** Enrolment mode (isAgent=false): the scenario TaxOfficeNumber travels in the HMRC-CIS-ORG enrolment of the bearer
  * token and must be passed down by the backend into the ChRIS XML.
  */
class EnrolmentModeE2eSpec extends E2eBaseSpec {
  Scenarios.enrolment.foreach { s =>
    test(s"[enrolment] TON=${s.ton} ${s.label}")(runScenario(Mode.Enrolment, s))
  }
}
