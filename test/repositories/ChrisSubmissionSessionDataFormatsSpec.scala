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

package repositories

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.VerificationActionIndicator
import uk.gov.hmrc.constructionindustryscheme.repositories.*

import java.time.{Instant, LocalDateTime}

class ChrisSubmissionSessionDataFormatsSpec extends AnyFreeSpec with Matchers {

  private val dt = LocalDateTime.of(2025, 5, 6, 7, 8, 9)

  private val requested = StoredRequestedVerification(
    1L,
    2L,
    Some(3L),
    "Sub One",
    "V",
    proceedVerification = true,
    Some("John"),
    Some("A"),
    Some("Smith"),
    Some("Trading"),
    Some("1234567890"),
    Some("AB123456C"),
    Some("CRN1"),
    Some("individual"),
    Some("0987654321")
  )

  private val verificationContext = StoredVerificationContext(
    10L,
    "mark",
    dt,
    Seq(VerificationActionIndicator(1L, "V")),
    Seq(requested),
    Some("a@b.com"),
    Some("en")
  )

  "StoredRequestedVerification" - {
    "round-trip a fully populated value" in {
      Json.toJson(requested).validate[StoredRequestedVerification] mustBe JsSuccess(requested)
    }

    "round-trip with optionals absent" in {
      val minimal = requested.copy(
        subbieResourceRef = None,
        foreName = None,
        middleName = None,
        surname = None,
        tradingName = None,
        utr = None,
        nino = None,
        crn = None,
        subcontractorType = None,
        partnershipUtr = None
      )
      Json.toJson(minimal).as[StoredRequestedVerification] mustBe minimal
    }

    "fail when a required field is missing" in {
      Json.obj("verificationResourceRef" -> 1).validate[StoredRequestedVerification].isError mustBe true
    }
  }

  "StoredVerificationContext" - {
    "round-trip a fully populated value" in {
      Json.toJson(verificationContext).validate[StoredVerificationContext] mustBe JsSuccess(verificationContext)
    }

    "round-trip with empty lists and no optionals" in {
      val model = StoredVerificationContext(10L, "mark", dt, Nil, Nil)
      Json.toJson(model).as[StoredVerificationContext] mustBe model
    }
  }

  "StoredMonthlyReturnContext" - {
    "round-trip" in {
      val model = StoredMonthlyReturnContext("mark", dt)
      Json.toJson(model).validate[StoredMonthlyReturnContext] mustBe JsSuccess(model)
    }

    "fail when the date is invalid" in {
      Json
        .obj("hmrcMarkGenerated" -> "m", "submissionRequestDate" -> "bad")
        .validate[StoredMonthlyReturnContext]
        .isError mustBe true
    }
  }

  "ChrisSubmissionSessionData" - {
    val session = ChrisSubmissionSessionData(
      "sub",
      "inst",
      "corr",
      Instant.parse("2025-05-06T07:08:09Z"),
      3,
      30,
      "http://poll",
      None,
      Some(StoredMonthlyReturnContext("mark", dt)),
      Some(verificationContext)
    )

    "round-trip a fully populated session" in {
      Json.toJson(session).validate[ChrisSubmissionSessionData] mustBe JsSuccess(session)
    }

    "round-trip without optional contexts" in {
      val model = session.copy(monthlyReturnContext = None, verificationContext = None)
      Json.toJson(model).as[ChrisSubmissionSessionData] mustBe model
    }

    "fail when submissionId is missing" in {
      Json.obj("instanceId" -> "inst").validate[ChrisSubmissionSessionData].isError mustBe true
    }
  }
}
