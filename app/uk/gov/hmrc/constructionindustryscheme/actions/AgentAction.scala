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

package uk.gov.hmrc.constructionindustryscheme.actions

import play.api.Logging
import play.api.mvc.*
import uk.gov.hmrc.auth.core.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{AuthenticatedAgentRequest, AuthenticatedRequest}
import uk.gov.hmrc.http.UnauthorizedException

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultAgentAction @Inject() (
  override val authConnector: AuthConnector,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AgentAction
    with AuthorisedFunctions
    with Logging:

  override protected def transform[A](
    request: AuthenticatedRequest[A]
  ): Future[AuthenticatedAgentRequest[A]] =
    request.enrolments.getEnrolment("IR-PAYE-AGENT").flatMap(_.getIdentifier("IRAgentReference")) match {
      case Some(agentEnrolment) => Future.successful(request.asAgent(agentEnrolment.value))
      case _                    =>
        logger.warn("No IR_PAYE_AGENT enrolment with IRAgentReference identifier for Agent user")
        Future.failed(new UnauthorizedException("Unable to retrieve internal ID from auth"))
    }

trait AgentAction extends ActionTransformer[AuthenticatedRequest, AuthenticatedAgentRequest]
