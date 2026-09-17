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

package models.finalvalidation

import base.SpecBase
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.finalvalidation.FinalValidationSubcontractorDetails

class FinalValidationSubcontractorDetailsSpec extends SpecBase {

  "FinalValidationSubcontractorDetails" - {

    "must serialize and deserialize empty details correctly" in {
      val details = FinalValidationSubcontractorDetails()
      Json.toJson(details).as[FinalValidationSubcontractorDetails] mustBe details
    }

    "must serialize and deserialize populated details correctly" in {
      val details = FinalValidationSubcontractorDetails(
        firstName = Some("John"),
        surname = Some("Smith"),
        tradingName = Some("JS Plumbing"),
        utr = Some("1234567890"),
        emailAddress = Some("john@example.com")
      )

      Json.toJson(details).as[FinalValidationSubcontractorDetails] mustBe details
    }
  }
}
