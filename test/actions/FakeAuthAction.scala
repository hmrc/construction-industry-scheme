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

package actions

import play.api.mvc.{AnyContent, BodyParser, PlayBodyParsers, Request, Result}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.requests.AuthenticatedRequest

import scala.concurrent.{ExecutionContext, Future}

final class FakeAuthAction(enrols: Enrolments, parsers: PlayBodyParsers)
                          (implicit ec: ExecutionContext) extends AuthAction {

  override def parser: BodyParser[AnyContent] = parsers.defaultBodyParser
  override protected def executionContext: ExecutionContext = ec

  override def invokeBlock[A](request: Request[A],
                              block: AuthenticatedRequest[A] => Future[Result]): Future[Result] =
    block(AuthenticatedRequest(request, "internalId", SessionId("sessionId"), enrols))
}

object FakeAuthAction {
  def withCisIdentifiers(ton: String, tor: String, parsers: PlayBodyParsers)
                        (implicit ec: ExecutionContext): FakeAuthAction = {
    val cis = Enrolment(
      key = "HMRC-CIS-ORG",
      identifiers = Seq(
        EnrolmentIdentifier("TaxOfficeNumber", ton),
        EnrolmentIdentifier("TaxOfficeReference", tor)
      ),
      state = "Activated"
    )
    new FakeAuthAction(Enrolments(Set(cis)), parsers)
  }

  def empty(parsers: PlayBodyParsers)(implicit ec: ExecutionContext): FakeAuthAction =
    new FakeAuthAction(Enrolments(Set.empty), parsers)
}