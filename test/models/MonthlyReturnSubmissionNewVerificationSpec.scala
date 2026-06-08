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
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models._

import java.time.LocalDateTime

final class MonthlyReturnSubmissionNewVerificationSpec extends AnyWordSpec with Matchers {
  "MonthlyReturnSubmissionNewVerification JSON format" should {

    "read JSON into model (including missing optional fields)" in {
      val json = Json.parse(
        """
          |{
          |  "submissionId": 555
          |}
          |""".stripMargin
      )

      val result = json.validate[MonthlyReturnSubmissionNewVerification]
      result mustBe a[JsSuccess[?]]

      val out = result.get
      out.submissionId mustBe 555L
      out.submissionRequestDate mustBe None
    }

    "write model to JSON" in {
      val model = MonthlyReturnSubmissionNewVerification(
        submissionId = 555L,
        submissionRequestDate = Some(LocalDateTime.of(2026, 4, 1, 10, 0, 0))
      )

      val json = Json.toJson(model)

      (json \ "submissionId").as[Long] mustBe 555L
      (json \ "submissionRequestDate").as[String] mustBe "2026-04-01T10:00:00"
    }

    "round-trip (model -> json -> model) without losing data" in {
      val model = MonthlyReturnSubmissionNewVerification(
        submissionId = 1L,
        submissionRequestDate = Some(LocalDateTime.of(2026, 1, 1, 9, 30, 0))
      )

      val json = Json.toJson(model)
      json.validate[MonthlyReturnSubmissionNewVerification] mustBe JsSuccess(model)
    }
  }
}
