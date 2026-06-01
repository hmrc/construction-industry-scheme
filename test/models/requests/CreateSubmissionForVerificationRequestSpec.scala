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
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionForVerificationRequest, VerificationToUpdate}

final class CreateSubmissionForVerificationRequestSpec extends AnyWordSpec with Matchers {

  "CreateSubmissionForVerificationRequest JSON format" should {

    "read JSON into model (including agentId)" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "verificationBatchId": 99,
          |  "verificationBatchResourceRef": 10,
          |  "emailRecipient": "ops@example.com",
          |  "irMarkGenerated": "IR_MARK",
          |  "verifications": [
          |    {
          |      "subcontractorName": "ACME LTD",
          |      "verificationResourceRef": 111,
          |      "proceedVerification": "Y"
          |    },
          |    {
          |      "subcontractorName": "BETA LTD",
          |      "verificationResourceRef": 222,
          |      "proceedVerification": "N"
          |    }
          |  ],
          |  "agentId": "agent-123"
          |}
          |""".stripMargin
      )

      val result = json.validate[CreateSubmissionForVerificationRequest]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.instanceId mustBe "abc-123"
      out.verificationBatchId mustBe 99L
      out.verificationBatchResourceRef mustBe 10L
      out.emailRecipient mustBe "ops@example.com"
      out.irMarkGenerated mustBe Some("IR_MARK")
      out.agentId mustBe Some("agent-123")

      out.verifications must have size 2
      out.verifications.head.subcontractorName mustBe "ACME LTD"
      out.verifications.head.verificationResourceRef mustBe 111L
      out.verifications.head.proceedVerification mustBe "Y"
      out.verifications(1).subcontractorName mustBe "BETA LTD"
      out.verifications(1).verificationResourceRef mustBe 222L
      out.verifications(1).proceedVerification mustBe "N"
    }

    "read JSON into model when agentId is missing (defaults to None)" in {
      val json = Json.parse(
        """
          |{
          |  "instanceId": "abc-123",
          |  "verificationBatchId": 99,
          |  "verificationBatchResourceRef": 10,
          |  "emailRecipient": "ops@example.com",
          |  "irMarkGenerated": "IR_MARK",
          |  "verifications": [
          |    {
          |      "subcontractorName": "ACME LTD",
          |      "verificationResourceRef": 111,
          |      "proceedVerification": "Y"
          |    }
          |  ]
          |}
          |""".stripMargin
      )

      val out = json.as[CreateSubmissionForVerificationRequest]
      out.agentId mustBe None
      out.verifications must have size 1
    }

    "write model to JSON (and include agentId only when defined)" in {
      val modelWithAgent = CreateSubmissionForVerificationRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate(
            subcontractorName = "ACME LTD",
            verificationResourceRef = 111L,
            proceedVerification = "Y"
          )
        ),
        agentId = Some("agent-123")
      )

      val jsonWithAgent = Json.toJson(modelWithAgent)

      (jsonWithAgent \ "instanceId").as[String] mustBe "abc-123"
      (jsonWithAgent \ "verificationBatchId").as[Long] mustBe 99L
      (jsonWithAgent \ "verificationBatchResourceRef").as[Long] mustBe 10L
      (jsonWithAgent \ "emailRecipient").as[String] mustBe "ops@example.com"
      (jsonWithAgent \ "irMarkGenerated").as[String] mustBe "IR_MARK"
      (jsonWithAgent \ "agentId").as[String] mustBe "agent-123"

      (jsonWithAgent \ "verifications").as[Seq[play.api.libs.json.JsValue]] must have size 1
      ((jsonWithAgent \ "verifications")(0) \ "subcontractorName").as[String] mustBe "ACME LTD"
      ((jsonWithAgent \ "verifications")(0) \ "verificationResourceRef").as[Long] mustBe 111L
      ((jsonWithAgent \ "verifications")(0) \ "proceedVerification").as[String] mustBe "Y"

      val modelWithoutAgent = modelWithAgent.copy(agentId = None)
      val jsonWithoutAgent  = Json.toJson(modelWithoutAgent)

      (jsonWithoutAgent \ "agentId").toOption mustBe None
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = CreateSubmissionForVerificationRequest(
        instanceId = "abc-123",
        verificationBatchId = 99L,
        verificationBatchResourceRef = 10L,
        emailRecipient = "ops@example.com",
        irMarkGenerated = Some("IR_MARK"),
        verifications = Seq(
          VerificationToUpdate("ACME LTD", 111L, "Y"),
          VerificationToUpdate("BETA LTD", 222L, "N")
        ),
        agentId = Some("agent-123")
      )

      val json = Json.toJson(model)
      json.validate[CreateSubmissionForVerificationRequest] mustBe JsSuccess(model)
    }
  }
}

