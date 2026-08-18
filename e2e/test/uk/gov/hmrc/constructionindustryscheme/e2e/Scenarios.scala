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

  // Poll leg for the connection-abort scenario: the stub aborts the connection mid-stream on this crafted URL (count>=2),
  // so the backend hits its transport-failure branch (ACCEPTED + "timed out"). Mirrors s3PollUrl.
  private val abortPollUrl = s"${E2eConfig.stubHost}/submission/ChRIS/poll/IR-CIS-VERIFY/2?final=CONNECTION_ABORT"

  private val connectionAbortRows: Seq[Scenario] = Seq(
    Scenario(
      "781",
      "EZ00100",
      "F18 s8: connection abort on submit -> FATAL_ERROR (transport failure, not 5xx), govtalk code xxxx",
      200,
      Some("FATAL_ERROR"),
      None,
      expectSubmitErrorText = Some("timed out"),
      expectSubmitErrorCode = Some("xxxx")
    ),
    Scenario(
      "123",
      "EZ00100",
      "F18 s9: connection abort on poll -> ACCEPTED (transport failure, not 5xx), govtalk code xxxx",
      202,
      Some("ACCEPTED"),
      Some("ACCEPTED"),
      Some("xxxx"),
      Some(abortPollUrl),
      expectPollErrorText = Some("timed out")
    )
  )

  val enrolment: Seq[Scenario] = Seq(
    Scenario("123", "EZ00100", "happy path (success on poll)", 202, Some("ACCEPTED"), Some("SUBMITTED")),
    // ChRIS 5xx on submit -> FATAL_ERROR persisted to a healthy FormP and returned to the user as HTTP 200. (The
    // taxpayer/FormP downstream calls use the benign body ref 123, so no formp-proxy outage collides here.)
    Scenario("500", "EZ00100", "F18 s1: ChRIS HTTP 500 on submit -> 200 FATAL_ERROR", 200, Some("FATAL_ERROR"), None),
    Scenario("502", "EZ00100", "F18 s1: ChRIS HTTP 502 on submit -> 200 FATAL_ERROR", 200, Some("FATAL_ERROR"), None),
    Scenario("503", "EZ00100", "F18 s1: ChRIS HTTP 503 on submit -> 200 FATAL_ERROR", 200, Some("FATAL_ERROR"), None),
    Scenario("779", "EZ00125", "F18 s2: immediate FATAL_ERROR from ChRIS", 200, Some("FATAL_ERROR"), None),
    Scenario(
      "123",
      "EZ00100",
      "F18 s3: ChRIS HTTP 500 on poll (crafted count>=2 URL)",
      202,
      Some("ACCEPTED"),
      Some("ACCEPTED"),
      Some("500"),
      Some(s3PollUrl)
    ),
    Scenario(
      "775",
      "EZ00100",
      "F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)",
      202,
      Some("ACCEPTED"),
      Some("FATAL_ERROR"),
      Some("1001")
    ),
    Scenario(
      "780",
      "EZ00100",
      "F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some("3000")
    ),
    Scenario(
      "776",
      "EZ00100",
      "F18 s6: DEPARTMENTAL_ERROR 3001/business on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some("3001")
    ),
    Scenario("777", "EZ00100", "SUBMITTED_NO_RECEIPT on poll", 202, Some("ACCEPTED"), Some("SUBMITTED_NO_RECEIPT")),
    Scenario("778", "EZ00100", "forever-pending ack (poll succeeds)", 202, Some("ACCEPTED"), Some("SUBMITTED"))
  ) ++ connectionAbortRows

  val agent: Seq[Scenario] = Seq(
    Scenario("123", "EZ00100", "happy path (success on poll)", 202, Some("ACCEPTED"), Some("SUBMITTED")),
    // TODO Re-enable this scenario as part of DTR-7959, with the FormP 500 Wire Mocked.
    //Scenario("500", "EZ00100", "F18 s1: ChRIS 500 + FormP 500 on failure leg", 500, None, None),
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
      Some("500"),
      Some(s3PollUrl)
    ),
    Scenario(
      "775",
      "EZ00100",
      "F18 s4+s7: FATAL_ERROR on poll (err 1001 = other)",
      202,
      Some("ACCEPTED"),
      Some("FATAL_ERROR"),
      Some("1001")
    ),
    Scenario(
      "780",
      "EZ00100",
      "F18 s5: DEPARTMENTAL_ERROR 3000/fatal on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some("3000")
    ),
    Scenario(
      "776",
      "EZ00100",
      "F18 s6: DEPARTMENTAL_ERROR 3001/business on poll",
      202,
      Some("ACCEPTED"),
      Some("DEPARTMENTAL_ERROR"),
      Some("3001")
    ),
    Scenario("777", "EZ00100", "SUBMITTED_NO_RECEIPT on poll", 202, Some("ACCEPTED"), Some("SUBMITTED_NO_RECEIPT")),
    Scenario("778", "EZ00100", "forever-pending ack (poll succeeds)", 202, Some("ACCEPTED"), Some("SUBMITTED"))
  ) ++ connectionAbortRows
}
