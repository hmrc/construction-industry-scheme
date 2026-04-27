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

package models.response

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.{MonthlyReturnData, SchemeData, SubmissionData, SubmittedMonthlyReturnsResponse}

import java.time.Instant

class SubmittedMonthlyReturnsResponseSpec extends AnyWordSpec with Matchers {

  "SubmittedMonthlyReturnsResponse" should {

    "round-trip to/from JSON" in {
      val model = SubmittedMonthlyReturnsResponse(
        scheme = SchemeData(
          name = "Scheme Name",
          taxOfficeNumber = "123",
          taxOfficeReference = "ABC"
        ),
        monthlyReturns = Seq(
          MonthlyReturnData(
            monthlyReturnId = 1L,
            taxYear = 2025,
            taxMonth = 1,
            nilReturnIndicator = "Y",
            status = "Open",
            amendment = "N",
            supersededBy = Some(2L),
            amendmentStatus = Some("Status"),
            monthlyReturnItems = Some("Items")
          )
        ),
        submissions = Seq(
          SubmissionData(
            submissionId = 1L,
            submissionType = Some("Type"),
            activeObjectId = Some(1L),
            status = Some("Status"),
            hmrcMarkGenerated = Some("Mark"),
            hmrcMarkGgis = Some("Ggis"),
            emailRecipient = Some("Email"),
            acceptedTime = Some(Instant.parse("2025-01-01T00:00:00Z"))
          )
        )
      )

      val json = Json.toJson(model)
      json.as[SubmittedMonthlyReturnsResponse] mustBe model
    }
  }
}
