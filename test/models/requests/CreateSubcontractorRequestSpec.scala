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

package models.requests

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.SoleTrader
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateSubcontractorRequest

class CreateSubcontractorRequestSpec extends AnyFreeSpec with Matchers {

  "CreateSubcontractorRequest JSON" - {

    "round-trips" in {
      val req = CreateSubcontractorRequest(123, SoleTrader, 1)
      Json.parse(Json.toJson(req).toString).as[CreateSubcontractorRequest] shouldBe req
    }

    "fails if required field missing" in {
      Json
        .obj("schemeId" -> 123, "version" -> 1)
        .validate[CreateSubcontractorRequest]
        .isError shouldBe true
    }
  }
}
