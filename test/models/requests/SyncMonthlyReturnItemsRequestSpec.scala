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
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.SyncMonthlyReturnItemsRequest

class SyncMonthlyReturnItemsRequestSpec extends AnyWordSpec with Matchers {

  "SyncMonthlyReturnItemsRequest JSON format" should {
    "serialize and deserialize" in {
      val model = SyncMonthlyReturnItemsRequest(
        instanceId = "instance-1",
        taxYear = 2026,
        taxMonth = 2,
        amendment = "amendment-1",
        createResourceReferences = Seq(10L, 20L),
        deleteResourceReferences = Seq(30L)
      )

      val json = Json.toJson(model)
      json.validate[SyncMonthlyReturnItemsRequest].asOpt shouldBe Some(model)
    }
  }
}
