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

package uk.gov.hmrc.constructionindustryscheme.repositories

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import play.api.libs.json.JsObject
import uk.gov.hmrc.constructionindustryscheme.repositories.JourneyHandoffDataKeys.*
import java.time.Instant

case class JourneyHandoffData(
  id: String,
  userId: String,
  journeyType: String,
  data: JsObject,
  lastUpdated: Instant
)

object JourneyHandoffData {
  given dataFormat: Format[Instant]        = MongoJavatimeFormats.instantFormat
  given format: Format[JourneyHandoffData] = Json.format[JourneyHandoffData]
}

object JourneyHandoffDataKeys {
  val idField: String          = "id"
  val userIdField: String      = "userId"
  val journeyTypeField: String = "journeyType"
  val dataField: String        = "data"
  val lastUpdatedField: String = "lastUpdated"
}
