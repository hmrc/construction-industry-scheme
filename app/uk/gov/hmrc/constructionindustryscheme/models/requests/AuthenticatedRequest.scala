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

package uk.gov.hmrc.constructionindustryscheme.models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.http.{HeaderCarrier, SessionId, UnauthorizedException}

case class AuthenticatedRequest[A](
  private val request: Request[A],
  enrolments: Enrolments,
  credentialId: String
)(implicit hc: HeaderCarrier)
    extends WrappedRequest[A](request) {
  val sessionId: SessionId =
    hc.sessionId.getOrElse(throw new UnauthorizedException("Unable to retrieve session ID from headers"))

  def asAgent(agentIdentifier: String) =
    AuthenticatedAgentRequest(
      this.request,
      this.sessionId,
      this.enrolments,
      this.credentialId,
      agentIdentifier
    )
}

case class AuthenticatedAgentRequest[A](
  private val request: Request[A],
  sessionId: SessionId,
  enrolments: Enrolments,
  credentialId: String,
  agentId: String
) extends WrappedRequest[A](request)
