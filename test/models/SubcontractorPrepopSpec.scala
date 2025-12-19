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
import uk.gov.hmrc.constructionindustryscheme.models.{PrePopSubcontractor, PrePopSubcontractorsBody, PrePopSubcontractorsResponse, PrepopKnownFacts}

class SubcontractorPrepopSpec extends AnyFreeSpec with Matchers {

  "PrePopSubcontractorsResponse JSON" in {
    val model = PrePopSubcontractorsResponse(
      knownfacts = PrepopKnownFacts("123", "ABC123", "AGENT-REF"),
      prePopSubcontractors = PrePopSubcontractorsBody(
        response = 1,
        subcontractors = Seq(
          PrePopSubcontractor(
            subcontractorType = "individual",
            utr = "1234567890",
            verificationNumber = "VN123456",
            verificationSuffix = "A",
            title = "Mr",
            firstName = "John",
            secondName = "A",
            surname = "Doe"
          )
        )
      )
    )

    Json.toJson(model).as[PrePopSubcontractorsResponse] shouldBe model
  }
}
