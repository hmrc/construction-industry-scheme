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

import play.api.libs.json.{JsSuccess, Json}
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.constructionindustryscheme.models.requests.*

import java.time.LocalDateTime

class ProcessVerificationResponseFromChrisRequestSpec extends PlaySpec {

  "ProcessVerificationResponseFromChrisRequest" should {

    "serialize to JSON" in {
      val model = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "SUBMITTED",
        irMarkReceived = "IR_MARK_RECEIVED",
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = "V123456",
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      Json.toJson(model) mustBe Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "verificationBatchResourceRef": 222,
          |  "acceptedTime": "2026-06-15T10:05:00Z",
          |  "submissionStatus": "SUBMITTED",
          |  "irMarkReceived": "IR_MARK_RECEIVED",
          |  "verificationResults": [
          |    {
          |      "resourceRef": 456,
          |      "matched": "Y",
          |      "verified": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verifiedDate": "2026-06-15T10:05:00"
          |    }
          |  ]
          |}
          |""".stripMargin
      )
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "verificationBatchResourceRef": 222,
          |  "acceptedTime": "2026-06-15T10:05:00Z",
          |  "submissionStatus": "SUBMITTED",
          |  "irMarkReceived": "IR_MARK_RECEIVED",
          |  "verificationResults": [
          |    {
          |      "resourceRef": 456,
          |      "matched": "Y",
          |      "verified": "Y",
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verifiedDate": "2026-06-15T10:05:00"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val expected = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "SUBMITTED",
        irMarkReceived = "IR_MARK_RECEIVED",
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = Some("Y"),
            verified = Some("Y"),
            verificationNumber = "V123456",
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      json.validate[ProcessVerificationResponseFromChrisRequest] mustBe JsSuccess(expected)
    }

    "deserialize when optional verification result fields are missing" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "verificationBatchResourceRef": 222,
          |  "acceptedTime": "2026-06-15T10:05:00Z",
          |  "submissionStatus": "SUBMITTED",
          |  "irMarkReceived": "IR_MARK_RECEIVED",
          |  "verificationResults": [
          |    {
          |      "resourceRef": 456,
          |      "verificationNumber": "V123456",
          |      "taxTreatment": "NET",
          |      "verifiedDate": "2026-06-15T10:05:00"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val expected = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        verificationBatchResourceRef = 222L,
        acceptedTime = "2026-06-15T10:05:00Z",
        submissionStatus = "SUBMITTED",
        irMarkReceived = "IR_MARK_RECEIVED",
        verificationResults = Seq(
          VerificationResult(
            resourceRef = 456L,
            matched = None,
            verified = None,
            verificationNumber = "V123456",
            taxTreatment = "NET",
            verifiedDate = LocalDateTime.parse("2026-06-15T10:05:00")
          )
        )
      )

      json.validate[ProcessVerificationResponseFromChrisRequest] mustBe JsSuccess(expected)
    }

    "fail to deserialize when required fields are missing" in {
      val json = Json.obj(
        "instanceId" -> "abc-123"
      )

      json.validate[ProcessVerificationResponseFromChrisRequest].isError mustBe true
    }
  }
}
