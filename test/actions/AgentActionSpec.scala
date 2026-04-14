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

package actions

import base.SpecBase
import org.scalatest.RecoverMethods.recoverToExceptionIf
import play.api.mvc.BodyParsers
import uk.gov.hmrc.auth.core.{AuthConnector, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.constructionindustryscheme.actions.DefaultAgentAction
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedAgentRequest, AuthenticatedRequest}
import uk.gov.hmrc.http.UnauthorizedException

import scala.concurrent.{ExecutionContext, Future}

class AgentActionSpec extends SpecBase {

  private class TestDefaultAgentAction(override val parser: BodyParsers.Default)(implicit
    override val executionContext: ExecutionContext
  ) extends DefaultAgentAction(
        authConnector = mock[AuthConnector],
        parser = parser
      ) {

    def transformPublic[A](req: AuthenticatedRequest[A]): Future[AuthenticatedAgentRequest[A]] = transform(req)
  }

  private val defaultBodyParsers: BodyParsers.Default =
    app.injector.instanceOf[BodyParsers.Default]

  private val action =
    new TestDefaultAgentAction(defaultBodyParsers)(using ec)

  "DefaultAgentAction.transform" - {

    "should return AuthenticatedAgentRequest when IR-PAYE-AGENT enrolment has IRAgentReference" in {
      val enrols = Enrolments(
        Set(
          Enrolment(
            key = "IR-PAYE-AGENT",
            identifiers = Seq(EnrolmentIdentifier("IRAgentReference", "AGENT-123")),
            state = "Activated"
          )
        )
      )

      val req =
        createAuthReq(
          request = fakeRequest,
          enrols = enrols
        )

      whenReady(action.transformPublic(req)) { agentReq =>
        agentReq.agentId mustBe "AGENT-123"
        agentReq.credentialId mustBe "credId-123"
        agentReq.sessionId mustBe uk.gov.hmrc.http.SessionId("sessionId")
      }
    }

    "should fail with UnauthorizedException when IR-PAYE-AGENT enrolment is missing" in {
      val req =
        createAuthReq(
          request = fakeRequest,
          enrols = Enrolments(Set.empty)
        )

      val exF = recoverToExceptionIf[UnauthorizedException](action.transformPublic(req))

      whenReady(exF) { (ex: UnauthorizedException) =>
        ex.getMessage must include("Unable to retrieve internal ID from auth")
      }
    }

    "should fail with UnauthorizedException when IR-PAYE-AGENT enrolment exists but IRAgentReference is missing" in {
      val enrols = Enrolments(
        Set(
          Enrolment(
            key = "IR-PAYE-AGENT",
            identifiers = Seq(EnrolmentIdentifier("SOMETHING_ELSE", "X")),
            state = "Activated"
          )
        )
      )

      val req =
        createAuthReq(
          request = fakeRequest,
          enrols = enrols
        )

      val exF = recoverToExceptionIf[UnauthorizedException](action.transformPublic(req))

      whenReady(exF) { (ex: UnauthorizedException) =>
        ex.getMessage must include("Unable to retrieve internal ID from auth")
      }
    }
  }
}
