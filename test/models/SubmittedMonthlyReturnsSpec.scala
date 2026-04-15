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
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, Submission, SubmittedMonthlyReturn, SubmittedMonthlyReturns}

class SubmittedMonthlyReturnsSpec extends AnyWordSpec with Matchers {

  "SubmittedMonthlyReturns" should {

    "round-trip to/from JSON" in {
      val model = SubmittedMonthlyReturns(
        scheme = ContractorScheme(
          schemeId = 1,
          instanceId = "123",
          accountsOfficeReference = "ABC",
          taxOfficeNumber = "123",
          taxOfficeReference = "ABC",
          name = Some("Scheme Name")
        ),
        monthlyReturns = Seq(
          SubmittedMonthlyReturn(
            monthlyReturnId = 1L,
            taxYear = 2025,
            taxMonth = 1
          )
        ),
        submissions = Seq(
          Submission(
            submissionId = 1L,
            submissionType = "Type",
            activeObjectId = Some(1L),
            status = Some("Status"),
            hmrcMarkGenerated = Some("Mark"),
            hmrcMarkGgis = Some("Ggis"),
            emailRecipient = Some("Email"),
            acceptedTime = Some("2025-01-01T00:00:00Z"),
            createDate = None,
            lastUpdate = None,
            schemeId = 1L,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val json = Json.toJson(model)
      json.as[SubmittedMonthlyReturns] mustBe model
    }
  }
}
