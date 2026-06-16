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
        submissionType = "VERIFICATIONS",
        activeObjectId = 10L,
        hmrcMarkGenerated = Some("hmrc-mark-generated"),
        hmrcMarkGgis = Some("hmrc-mark-ggis"),
        emailRecipient = Some("test@example.com"),
        submissionRequestDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
        acceptedTime = Some("2026-06-15T10:05:00Z"),
        agentId = Some("agent-123"),
        submittableStatus = "ACCEPTED",
        govTalkErrorCode = Some("001"),
        govTalkErrorType = Some("business"),
        govTalkErrorMessage = Some("error message"),
        verifBatchResourceRef = 222L,
        verificationResourceRef = 333L,
        subbieResourceRef = 456L,
        matched = Some("Y"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("NET"),
        actionIndicator = Some("VERIFY"),
        proceed = Some("Y"),
        subcontractorName = "John Smith"
      )

      Json.toJson(model) mustBe Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "submissionType": "VERIFICATIONS",
          |  "activeObjectId": 10,
          |  "hmrcMarkGenerated": "hmrc-mark-generated",
          |  "hmrcMarkGgis": "hmrc-mark-ggis",
          |  "emailRecipient": "test@example.com",
          |  "submissionRequestDate": "2026-06-15T10:00:00",
          |  "acceptedTime": "2026-06-15T10:05:00Z",
          |  "agentId": "agent-123",
          |  "submittableStatus": "ACCEPTED",
          |  "govTalkErrorCode": "001",
          |  "govTalkErrorType": "business",
          |  "govTalkErrorMessage": "error message",
          |  "verifBatchResourceRef": 222,
          |  "verificationResourceRef": 333,
          |  "subbieResourceRef": 456,
          |  "matched": "Y",
          |  "verificationNumber": "V123456",
          |  "taxTreatment": "NET",
          |  "actionIndicator": "VERIFY",
          |  "proceed": "Y",
          |  "subcontractorName": "John Smith"
          |}
          |""".stripMargin
      )
    }

    "deserialize from JSON" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "submissionType": "VERIFICATIONS",
          |  "activeObjectId": 10,
          |  "hmrcMarkGenerated": "hmrc-mark-generated",
          |  "hmrcMarkGgis": "hmrc-mark-ggis",
          |  "emailRecipient": "test@example.com",
          |  "submissionRequestDate": "2026-06-15T10:00:00",
          |  "acceptedTime": "2026-06-15T10:05:00Z",
          |  "agentId": "agent-123",
          |  "submittableStatus": "ACCEPTED",
          |  "govTalkErrorCode": "001",
          |  "govTalkErrorType": "business",
          |  "govTalkErrorMessage": "error message",
          |  "verifBatchResourceRef": 222,
          |  "verificationResourceRef": 333,
          |  "subbieResourceRef": 456,
          |  "matched": "Y",
          |  "verificationNumber": "V123456",
          |  "taxTreatment": "NET",
          |  "actionIndicator": "VERIFY",
          |  "proceed": "Y",
          |  "subcontractorName": "John Smith"
          |}
          |""".stripMargin
      )

      val expected = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        submissionType = "VERIFICATIONS",
        activeObjectId = 10L,
        hmrcMarkGenerated = Some("hmrc-mark-generated"),
        hmrcMarkGgis = Some("hmrc-mark-ggis"),
        emailRecipient = Some("test@example.com"),
        submissionRequestDate = Some(LocalDateTime.parse("2026-06-15T10:00:00")),
        acceptedTime = Some("2026-06-15T10:05:00Z"),
        agentId = Some("agent-123"),
        submittableStatus = "ACCEPTED",
        govTalkErrorCode = Some("001"),
        govTalkErrorType = Some("business"),
        govTalkErrorMessage = Some("error message"),
        verifBatchResourceRef = 222L,
        verificationResourceRef = 333L,
        subbieResourceRef = 456L,
        matched = Some("Y"),
        verificationNumber = Some("V123456"),
        taxTreatment = Some("NET"),
        actionIndicator = Some("VERIFY"),
        proceed = Some("Y"),
        subcontractorName = "John Smith"
      )

      json.validate[ProcessVerificationResponseFromChrisRequest] mustBe JsSuccess(expected)
    }

    "deserialize when optional fields are missing" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "activeObjectId": 10,
          |  "submittableStatus": "ACCEPTED",
          |  "verifBatchResourceRef": 222,
          |  "verificationResourceRef": 333,
          |  "subbieResourceRef": 456,
          |  "subcontractorName": "John Smith"
          |}
          |""".stripMargin
      )

      val expected = ProcessVerificationResponseFromChrisRequest(
        instanceId = "abc-123",
        submissionType = "VERIFICATIONS",
        activeObjectId = 10L,
        hmrcMarkGenerated = None,
        hmrcMarkGgis = None,
        emailRecipient = None,
        submissionRequestDate = None,
        acceptedTime = None,
        agentId = None,
        submittableStatus = "ACCEPTED",
        govTalkErrorCode = None,
        govTalkErrorType = None,
        govTalkErrorMessage = None,
        verifBatchResourceRef = 222L,
        verificationResourceRef = 333L,
        subbieResourceRef = 456L,
        matched = None,
        verificationNumber = None,
        taxTreatment = None,
        actionIndicator = None,
        proceed = None,
        subcontractorName = "John Smith"
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
