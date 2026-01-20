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

package models

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.PrepopKnownFacts

class PrepopKnownFactsSpec extends AnyFreeSpec with Matchers {

  "PrepopKnownFacts JSON" - {

    "round-trips" in {
      val model = PrepopKnownFacts("123", "ABC123", "AGENT-REF")
      Json.toJson(model).as[PrepopKnownFacts] shouldBe model
    }

    "fails if a required field is missing" in {
      Json
        .obj(
          "taxOfficeNumber"    -> "123",
          "taxOfficeReference" -> "ABC123"
          // accountOfficeReference missing
        )
        .validate[PrepopKnownFacts]
        .isError shouldBe true
    }
  }
}
