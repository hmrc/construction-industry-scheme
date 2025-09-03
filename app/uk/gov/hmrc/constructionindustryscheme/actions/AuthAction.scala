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

package uk.gov.hmrc.constructionindustryscheme.actions

import play.api.Logging
import play.api.libs.json.Json
import play.api.mvc.*
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, internalId}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisationException, AuthorisedFunctions, NoActiveSession}
import uk.gov.hmrc.constructionindustryscheme.models.requests.AuthenticatedRequest
import uk.gov.hmrc.http.{HeaderCarrier, UnauthorizedException}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DefaultAuthAction @Inject()(
                                   override val authConnector: AuthConnector,
                                   val parser: BodyParsers.Default
                                 )(implicit val executionContext: ExecutionContext)
  extends AuthAction with AuthorisedFunctions with Logging:

  override def invokeBlock[A](request: Request[A], block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    given hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(request)

    val sessionId = hc.sessionId.getOrElse(throw new UnauthorizedException("Unable to retrieve session ID from headers"))

    authorised()
      .retrieve(allEnrolments and internalId) {
        case enrols ~ Some(intId) =>
          block(AuthenticatedRequest(request, intId, sessionId, enrols))
        case _ =>
          Future.failed(new UnauthorizedException("Unable to retrieve internal ID from auth"))
      }
      .recover {
        case _: NoActiveSession =>
          Results.Unauthorized(Json.obj("message" -> "No active session"))
        case _: AuthorisationException =>
          val msg = "Failed to authorise request"
          logger.warn(msg)
          Results.Unauthorized(Json.obj("message" -> msg))
      }

trait AuthAction
  extends ActionBuilder[AuthenticatedRequest, AnyContent] with ActionFunction[Request, AuthenticatedRequest]