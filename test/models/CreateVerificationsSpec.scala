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

package models

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.CreateVerifications

class CreateVerificationsSpec extends AnyWordSpec with Matchers {

  "CreateVerifications JSON format" should {

    "serialize and deserialize correctly" in {
      val model = CreateVerifications(
        verificationBatchResourceRef = 10L,
        verificationResourceReferences = Seq(333L, 444L)
      )

      val json = Json.toJson(model)
      json.as[CreateVerifications] mustBe model
    }
  }
}
