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

package uk.gov.hmrc.constructionindustryscheme.utils

import play.api.mvc.RequestHeader
import uk.gov.hmrc.constructionindustryscheme.models.requests.AuthenticatedRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendHeaderCarrierProvider

/** Overrides the implicit `hc` derivation so that any outbound call made from within an `AuthenticatedRequest` action
  * block automatically carries `X-Tax-Office-Number` and `X-Tax-Office-Reference` headers. The stub
  * (construction-industry-scheme-external-stub) reads these headers when using internal-auth to reconstruct the user's
  * HMRC-CIS-ORG enrolment and return the correct stubbed response.
  */
trait CisEnrolmentHeaderForwarding extends BackendHeaderCarrierProvider {

  override implicit protected def hc(implicit request: RequestHeader): HeaderCarrier = {
    val base = super.hc(request)
    request match {
      case ar: AuthenticatedRequest[_] => CisEnrolmentHelper.withCisEnrolmentHeaders(ar.enrolments)(base)
      case _                           => base
    }
  }
}
