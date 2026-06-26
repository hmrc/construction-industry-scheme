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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.response.{GetBatchPollSubmissionsResponse, MonthlyReturnSubmissionToPoll, VerificationSubmissionToPoll}

class GetBatchPollSubmissionsResponseSpec extends AnyFreeSpec with Matchers {

  "GetBatchPollSubmissionsResponse" - {

    "must read JSON into model" in {
      val json = Json.obj(
        "verificationSubmissions"  -> Json.arr(
          Json.obj(
            "submissionId"                 -> 90001,
            "submissionType"               -> "CISVERIFY",
            "agentId"                      -> "A123456",
            "taxOfficeNumber"              -> "123",
            "taxOfficeReference"           -> "ABC123",
            "instanceId"                   -> "instance-verification-001",
            "status"                       -> "SUBMITTED",
            "verificationBatchResourceRef" -> 70001
          )
        ),
        "monthlyReturnSubmissions" -> Json.arr(
          Json.obj(
            "submissionId"       -> 90002,
            "submissionType"     -> "CIS300MR",
            "status"             -> "SUBMITTED",
            "taxOfficeNumber"    -> "123",
            "taxOfficeReference" -> "456789",
            "taxYear"            -> 2025,
            "taxMonth"           -> 6,
            "instanceId"         -> "instance-monthly-return-001",
            "agentId"            -> "A123456",
            "amendment"          -> "N"
          )
        )
      )

      json.as[GetBatchPollSubmissionsResponse] mustBe GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(
          VerificationSubmissionToPoll(
            submissionId = 90001L,
            submissionType = "CISVERIFY",
            agentId = Some("A123456"),
            taxOfficeNumber = "123",
            taxOfficeReference = "ABC123",
            instanceId = "instance-verification-001",
            status = "SUBMITTED",
            verificationBatchResourceRef = 70001L
          )
        ),
        monthlyReturnSubmissions = Seq(
          MonthlyReturnSubmissionToPoll(
            submissionId = 90002L,
            submissionType = "CIS300MR",
            status = "SUBMITTED",
            taxOfficeNumber = "123",
            taxOfficeReference = "456789",
            taxYear = 2025,
            taxMonth = 6,
            instanceId = "instance-monthly-return-001",
            agentId = Some("A123456"),
            amendment = "N"
          )
        )
      )
    }

    "must write model to JSON" in {
      val model = GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq(
          VerificationSubmissionToPoll(
            submissionId = 90001L,
            submissionType = "CISVERIFY",
            agentId = Some("A123456"),
            taxOfficeNumber = "123",
            taxOfficeReference = "ABC123",
            instanceId = "instance-verification-001",
            status = "SUBMITTED",
            verificationBatchResourceRef = 70001L
          )
        ),
        monthlyReturnSubmissions = Seq(
          MonthlyReturnSubmissionToPoll(
            submissionId = 90002L,
            submissionType = "CIS300MR",
            status = "SUBMITTED",
            taxOfficeNumber = "123",
            taxOfficeReference = "456789",
            taxYear = 2025,
            taxMonth = 6,
            instanceId = "instance-monthly-return-001",
            agentId = Some("A123456"),
            amendment = "N"
          )
        )
      )

      Json.toJson(model) mustBe Json.obj(
        "verificationSubmissions"  -> Json.arr(
          Json.obj(
            "submissionId"                 -> 90001,
            "submissionType"               -> "CISVERIFY",
            "agentId"                      -> "A123456",
            "taxOfficeNumber"              -> "123",
            "taxOfficeReference"           -> "ABC123",
            "instanceId"                   -> "instance-verification-001",
            "status"                       -> "SUBMITTED",
            "verificationBatchResourceRef" -> 70001
          )
        ),
        "monthlyReturnSubmissions" -> Json.arr(
          Json.obj(
            "submissionId"       -> 90002,
            "submissionType"     -> "CIS300MR",
            "status"             -> "SUBMITTED",
            "taxOfficeNumber"    -> "123",
            "taxOfficeReference" -> "456789",
            "taxYear"            -> 2025,
            "taxMonth"           -> 6,
            "instanceId"         -> "instance-monthly-return-001",
            "agentId"            -> "A123456",
            "amendment"          -> "N"
          )
        )
      )
    }

    "must read empty lists" in {
      val json = Json.obj(
        "verificationSubmissions"  -> Json.arr(),
        "monthlyReturnSubmissions" -> Json.arr()
      )

      json.as[GetBatchPollSubmissionsResponse] mustBe GetBatchPollSubmissionsResponse(
        verificationSubmissions = Seq.empty,
        monthlyReturnSubmissions = Seq.empty
      )
    }
  }
}
