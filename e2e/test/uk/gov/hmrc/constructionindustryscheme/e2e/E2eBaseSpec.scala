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

import org.scalatest.exceptions.TestCanceledException
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Canceled, Failed, Outcome}
import play.api.libs.json.JsString
import uk.gov.hmrc.constructionindustryscheme.e2e.support.{AuthLoginApi, BackendClient, E2eConfig, Preflight}

import scala.util.Random

/** Base for the e2e suites: cancels every test with a clear message when the required local services are not running
  * (fails instead when E2E_STRICT=true) and runs one scenario row end-to-end (token -> submit -> optional poll).
  */
abstract class E2eBaseSpec extends AnyFunSuite with Matchers {

  override def withFixture(test: NoArgTest): Outcome =
    Preflight.failure match {
      case Some(msg) if E2eConfig.strict => Failed(new IllegalStateException(msg))
      case Some(msg)                     => Canceled(new TestCanceledException(msg, 0))
      case None                          => super.withFixture(test)
    }

  protected def runScenario(mode: Mode, s: Scenario): Unit = {
    val submissionId = s"e2e-${mode.label}-${s.ton}-${System.currentTimeMillis()}-${Random.nextInt(100000)}"
    val sessionId    = s"session-$submissionId"

    val (tokenEnrolment, bodyTon, bodyTor, isAgent) = mode match {
      case Mode.Enrolment   => (Some(s.ton -> s.tor), "123", "EZ00100", false)
      case Mode.Agent       => (Some("123" -> "EZ00100"), s.ton, s.tor, true)
      case Mode.NoEnrolment => (None, "123", "EZ00100", false)
    }

    val token  = AuthLoginApi.bearerToken(tokenEnrolment)
    val submit = BackendClient.submitVerification(submissionId, isAgent, bodyTon, bodyTor, token, sessionId)

    withClue(s"submit response body: ${submit.body}\n") {
      submit.status shouldBe s.expectSubmitHttp
      s.expectSubmitStatus.foreach(expected => (submit.json \ "status").asOpt[String] shouldBe Some(expected))
    }

    s.expectPollStatus.foreach { expectedPollStatus =>
      val pollUrl = s.pollUrlOverride
        .orElse((submit.json \ "responseEndPoint" \ "url").asOpt[String])
        .getOrElse(fail(s"no responseEndPoint.url to poll in: ${submit.body}"))

      val poll = BackendClient.poll(pollUrl, submissionId, token, sessionId)

      withClue(s"poll response body: ${poll.body}\n") {
        poll.status                          shouldBe 200
        (poll.json \ "status").asOpt[String] shouldBe Some(expectedPollStatus)
        // the backend renders errorNumber as a JSON string ("1001"); accept either form
        s.expectPollErrorNumber.foreach { n =>
          val actual = (poll.json \ "error" \ "errorNumber").toOption.map {
            case JsString(v) => v
            case other       => other.toString
          }
          actual shouldBe Some(n.toString)
        }
      }
    }
  }
}
