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

import uk.gov.hmrc.constructionindustryscheme.e2e.support.E2eConfig

/** The scenario matrix, ported verbatim from run-e2e-scenarios.sh.
  *
  * The TaxOfficeNumber reaches the stub through several legs, which dispatch on different sources, so expectations
  * differ per mode.
  */
object Scenarios {

  private val s3PollUrl = s"${E2eConfig.stubHost}/submission/ChRIS/poll/IR-CIS-VERIFY/2?final=SERVER_ERROR_500"

  val enrolment: Seq[Scenario] = Seq(
    Scenario("123", "EZ00100", "happy path (success on poll)", 202, Some("ACCEPTED"), Some("SUBMITTED")),
    Scenario(
      "500",
      "EZ00100",
      "F18 s1: ChRIS HTTP 500 on submit (taxpayer benign: body ref=123)",
      200,
      Some("FATAL_ERROR"),
      None
    ),
    Scenario("502", "EZ00100", "F18 s1: ChRIS HTTP 502 on submit", 200, Some("FATAL_ERROR"), None),
    Scenario("503", "EZ00100", "F18 s1: ChRIS HTTP 503 on submit", 200, Some("FATAL_ERROR"), None),
    Scenario("779", "EZ00125", "F18 s2: immediate FATAL_ERROR from ChRIS", 200, Some("FATAL_ERROR"), None),
    Scenario(
      "123",
      "EZ00100",
      "F18 s3: ChRIS HTTP 500 on poll (crafted count>=2 URL)",
      202,
      Some("ACCEPTED"),
      Some("ACCEPTED"),
      Some(500),
      Some(s3PollUrl)
    ),
    Scenario(
      "775",
      "EZ00100",
      "F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)",
      202,
      Some("ACCEPTED"),
      Some("FATAL_ERROR"),
      Some(1001)
    ),
    Scenario(
      "780",
      "EZ00100",
      "F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some(3000)
    ),
    Scenario(
      "776",
      "EZ00100",
      "F18 s6: DEPARTMENTAL_ERROR 3001/business on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some(3001)
    ),
    Scenario("777", "EZ00100", "SUBMITTED_NO_RECEIPT on poll", 202, Some("ACCEPTED"), Some("SUBMITTED_NO_RECEIPT")),
    Scenario("778", "EZ00100", "forever-pending ack (poll succeeds)", 202, Some("ACCEPTED"), Some("SUBMITTED"))
  )

  val agent: Seq[Scenario] = Seq(
    Scenario("123", "EZ00100", "happy path (success on poll)", 202, Some("ACCEPTED"), Some("SUBMITTED")),
    Scenario("500", "EZ00100", "F18 s1: ChRIS 500 + taxpayer 500 on failure leg", 500, Some("FATAL_ERROR"), None),
    Scenario("502", "EZ00100", "F18 s1: ChRIS HTTP 502 on submit", 200, Some("FATAL_ERROR"), None),
    Scenario("503", "EZ00100", "F18 s1: ChRIS HTTP 503 on submit", 200, Some("FATAL_ERROR"), None),
    Scenario("779", "EZ00125", "F18 s2: immediate FATAL_ERROR from ChRIS", 200, Some("FATAL_ERROR"), None),
    Scenario(
      "123",
      "EZ00100",
      "F18 s3: ChRIS HTTP 500 on poll (crafted count>=2 URL)",
      202,
      Some("ACCEPTED"),
      Some("ACCEPTED"),
      Some(500),
      Some(s3PollUrl)
    ),
    Scenario(
      "775",
      "EZ00100",
      "F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)",
      202,
      Some("ACCEPTED"),
      Some("FATAL_ERROR"),
      Some(1001)
    ),
    Scenario(
      "780",
      "EZ00100",
      "F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some(3000)
    ),
    Scenario(
      "776",
      "EZ00100",
      "F18 s6: DEPARTMENTAL_ERROR 3001/business on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some(3001)
    ),
    Scenario("777", "EZ00100", "SUBMITTED_NO_RECEIPT on poll", 202, Some("ACCEPTED"), Some("SUBMITTED_NO_RECEIPT")),
    Scenario("778", "EZ00100", "forever-pending ack (poll succeeds)", 202, Some("ACCEPTED"), Some("SUBMITTED"))
  )

  val noEnrolment: Scenario =
    Scenario("123", "EZ00100", "isAgent=false without HMRC-CIS-ORG -> 500", 500, None, None)
}
