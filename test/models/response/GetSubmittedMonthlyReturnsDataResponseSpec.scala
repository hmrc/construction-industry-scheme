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
import uk.gov.hmrc.constructionindustryscheme.models.{ContractorScheme, MonthlyReturn, Submission}
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetSubmittedMonthlyReturnsDataProxyResponse, GetSubmittedMonthlyReturnsDataResponse, SchemeData, SubmissionData}

import java.time.{Instant, ZoneOffset, ZonedDateTime}

class GetSubmittedMonthlyReturnsDataResponseSpec extends AnyWordSpec with Matchers {

  "GetSubmittedMonthlyReturnsDataResponse JSON format" should {

    "serialize and deserialize correctly" in {
      val model = GetSubmittedMonthlyReturnsDataResponse(
        scheme = SchemeData("Scheme Name", "163", "AB0063"),
        monthlyReturnId = 3000L,
        taxYear = 2025,
        taxMonth = 1,
        nilReturnIndicator = "Y",
        monthlyReturnItems = Seq.empty,
        submission = SubmissionData(
          submissionId = 1000L,
          submissionType = Some("Monthly Return"),
          activeObjectId = None,
          status = None,
          hmrcMarkGenerated = None,
          hmrcMarkGgis = None,
          emailRecipient = None,
          acceptedTime = Some(Instant.now())
        )
      )

      val json = Json.toJson(model)
      json.as[GetSubmittedMonthlyReturnsDataResponse] mustBe model
    }

    ".fromProxyResponse map proxy response correctly" in {
      val proxyResponse = GetSubmittedMonthlyReturnsDataProxyResponse(
        scheme = ContractorScheme(
          schemeId = 100,
          instanceId = "1",
          accountsOfficeReference = "accountsOfficeReference",
          taxOfficeNumber = "163",
          taxOfficeReference = "AB0063",
          name = Some("Scheme Name")
        ),
        monthlyReturn =
          Seq(MonthlyReturn(monthlyReturnId = 3000L, taxYear = 2025, taxMonth = 1, nilReturnIndicator = Some("Y"))),
        monthlyReturnItems = Seq.empty,
        submission = Seq(
          Submission(
            submissionId = 1000L,
            submissionType = "Monthly Return",
            activeObjectId = None,
            status = None,
            hmrcMarkGenerated = None,
            hmrcMarkGgis = None,
            emailRecipient = None,
            acceptedTime = Some("2026-04-06T09:50:08.000"),
            createDate = None,
            lastUpdate = None,
            schemeId = 100,
            agentId = None,
            l_Migrated = None,
            submissionRequestDate = None,
            govTalkErrorCode = None,
            govTalkErrorType = None,
            govTalkErrorMessage = None
          )
        )
      )

      val expectedResult = GetSubmittedMonthlyReturnsDataResponse(
        scheme = SchemeData("Scheme Name", "163", "AB0063"),
        monthlyReturnId = 3000L,
        taxYear = 2025,
        taxMonth = 1,
        nilReturnIndicator = "Y",
        monthlyReturnItems = Seq.empty,
        submission = SubmissionData(
          submissionId = 1000L,
          submissionType = Some("Monthly Return"),
          activeObjectId = None,
          status = None,
          hmrcMarkGenerated = None,
          hmrcMarkGgis = None,
          emailRecipient = None,
          acceptedTime = Some(ZonedDateTime.of(2026, 4, 6, 9, 50, 8, 0, ZoneOffset.UTC).toInstant)
        )
      )

      val result =
        GetSubmittedMonthlyReturnsDataResponse.fromProxyResponse(proxyResponse)

      result mustBe expectedResult
    }
  }
}
