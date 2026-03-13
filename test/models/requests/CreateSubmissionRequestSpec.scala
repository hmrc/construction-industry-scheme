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

package models.requests

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.CreateSubmissionRequest

class CreateSubmissionRequestSpec extends AnyWordSpec with Matchers {

  "CreateSubmissionRequest JSON format" should {

    "round-trip (writes then reads) successfully with all fields populated " in {
      val model = CreateSubmissionRequest(
        instanceId = "abc-123",
        taxYear = 2025,
        taxMonth = 1,
        hmrcMarkGenerated = Some("abc123"),
        emailRecipient = Some("test@example.com"),
        agentId = Some("AGENT-001"),
        subcontractorCount = Some(5),
        totalPaymentsMade = Some(240),
        totalTaxDeducted = Some(150)
      )

      val json = Json.toJson(model)
      json.validate[CreateSubmissionRequest] mustBe JsSuccess(model)
    }

    "read minimal valid JSON for partnership (only required fields)" in {
      val json = Json.parse(
        s"""
           |{
           |  "instanceId": "CIS-777",
           |  "taxYear": 2024,
           |  "taxMonth": 3
           |}
           |""".stripMargin
      )

      val result = json.validate[CreateSubmissionRequest]

      result.isSuccess mustBe true
      result.get mustBe CreateSubmissionRequest(
        instanceId = "CIS-777",
        taxYear = 2024,
        taxMonth = 3
      )
    }

    "fail to read when 'instanceId' is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "taxYear": 2024,
           |  "taxMonth": 3
           |}
           |""".stripMargin
      )

      json.validate[CreateSubmissionRequest].isError mustBe true
    }

    "fail to read when 'taxYear' is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "instanceId": "CIS-777",
           |  "taxMonth": 3
           |}
           |""".stripMargin
      )

      json.validate[CreateSubmissionRequest].isError mustBe true
    }

    "fail to read when 'taxMonth' is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "instanceId": "CIS-777",
           |  "taxYear": 2024
           |}
           |""".stripMargin
      )

      json.validate[CreateSubmissionRequest].isError mustBe true
    }
  }
}
