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

enum Mode(val label: String) {
  case Enrolment extends Mode("enrolment")
  case Agent extends Mode("agent")
  case NoEnrolment extends Mode("no-enrolment")
}

/** One row of the scenario matrix (a row of the bash ENROLMENT_SCENARIOS / AGENT_SCENARIOS tables).
  *
  * @param expectSubmitStatus
  *   expected `status` field of the submit response; None = accept any
  * @param expectPollStatus
  *   expected `status` field of the poll response; None = do not poll
  * @param expectPollErrorNumber
  *   expected `error.errorNumber` of the poll body, when checked
  * @param pollUrlOverride
  *   poll this URL instead of the submit's responseEndPoint.url (still after a successful submit, so the GovTalk record
  *   for the submissionId exists)
  */
final case class Scenario(
  ton: String,
  tor: String,
  label: String,
  expectSubmitHttp: Int,
  expectSubmitStatus: Option[String],
  expectPollStatus: Option[String],
  expectPollErrorNumber: Option[Int] = None,
  pollUrlOverride: Option[String] = None
)
