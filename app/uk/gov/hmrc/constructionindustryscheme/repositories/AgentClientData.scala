/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.crypto.Sensitive.SensitiveString
import uk.gov.hmrc.crypto.json.JsonEncryption
import uk.gov.hmrc.crypto.{Decrypter, Encrypter}
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class AgentClientData(id: String, data: JsValue, lastUpdated: Instant)

object AgentClientData {
  given dateFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  given plainFormat: OFormat[AgentClientData] = Json.format[AgentClientData]

  def encryptedFormat(implicit crypto: Encrypter with Decrypter): OFormat[AgentClientData] = {
    implicit val sensitiveFormat: Format[SensitiveString] =
      JsonEncryption.sensitiveEncrypterDecrypter(SensitiveString.apply)

    val reads: Reads[AgentClientData] = (
      (__ \ "id").read[String] and
        (__ \ "data").read[SensitiveString] and
        (__ \ "lastUpdated").read[Instant]
    )((id, data, lastUpdated) => AgentClientData(id, Json.parse(data.decryptedValue), lastUpdated))

    val writes: OWrites[AgentClientData] = (
      (__ \ "id").write[String] and
        (__ \ "data").write[SensitiveString] and
        (__ \ "lastUpdated").write[Instant]
    )(ua => (ua.id, SensitiveString(Json.stringify(ua.data)), ua.lastUpdated))

    OFormat(reads, writes)
  }
}

object AgentClientDataKeys {
  val dataKey: String        = "data"
  val idField: String        = "id"
  val lastUpdatedKey: String = "lastUpdated"
}
