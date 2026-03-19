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

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsSuccess, Json}
import uk.gov.hmrc.constructionindustryscheme.models.GovTalkStatusRecord

import java.time.LocalDateTime

class GovTalkStatusRecordSpec extends AnyWordSpec with Matchers {

  "GovTalkStatusRecord.format" should {

    "serialize and deserialize correctly" in {

      val now = LocalDateTime.now()

      val model = GovTalkStatusRecord(
        userIdentifier = "123",
        formResultID = "123456",
        correlationID = "CORR-123",
        formLock = "N",
        createDate = Some(now),
        endStateDate = None,
        lastMessageDate = now,
        numPolls = 0,
        pollInterval = 10,
        protocolStatus = "initial",
        gatewayURL = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
      )

      val json = Json.toJson(model)

      json.validate[GovTalkStatusRecord] mustBe JsSuccess(model)
    }
  }
}
