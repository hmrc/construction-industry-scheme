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
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.ProceedInsufficientVerificationRequest

class ProceedInsufficientVerificationRequestSpec extends AnyWordSpec with Matchers {

  "ProceedInsufficientVerificationRequest (JSON)" should {

    "read and write with mandatory fields" in {
      val json = Json.parse("""
                              |{
                              |  "instanceId": "1",
                              |  "verificationBatchResourceRef": 9,
                              |  "verificationResourceRef": 10,
                              |  "proceed": "Y"
                              |}
        """.stripMargin)

      val model = json.as[ProceedInsufficientVerificationRequest]
      model.instanceId mustBe "1"
      model.verificationBatchResourceRef mustBe 9
      model.verificationResourceRef mustBe 10
      model.proceed mustBe "Y"

      Json.toJson(model) mustBe json
    }

    "fail to read missing instanceId" in {
      val json = Json.parse("""
                              |{
                              |  "verificationBatchResourceRef": 9,
                              |  "verificationResourceRef": 10,
                              |  "proceed": "Y"
                              |}
        """.stripMargin)

      val result = json.validate[ProceedInsufficientVerificationRequest]
      result.isError mustBe true
    }

    "fail to read missing verificationBatchResourceRef" in {
      val json = Json.parse("""
                              |{
                              |  "instanceId": "1",
                              |  "verificationResourceRef": 10,
                              |  "proceed": "Y"
                              |}
        """.stripMargin)

      val result = json.validate[ProceedInsufficientVerificationRequest]
      result.isError mustBe true
    }

    "fail to read missing verificationResourceRef" in {
      val json = Json.parse("""
                              |{
                              |  "instanceId": "1",
                              |  "verificationBatchResourceRef": 9,
                              |  "proceed": "Y"
                              |}
        """.stripMargin)

      val result = json.validate[ProceedInsufficientVerificationRequest]
      result.isError mustBe true
    }

    "fail to read missing proceed" in {
      val json = Json.parse("""
                              |{
                              |  "instanceId": "1",
                              |  "verificationBatchResourceRef": 9,
                              |  "verificationResourceRef": 10
                              |}
        """.stripMargin)

      val result = json.validate[ProceedInsufficientVerificationRequest]
      result.isError mustBe true
    }
  }
}
