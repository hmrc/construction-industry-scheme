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

import base.SpecBase
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.*
import uk.gov.hmrc.crypto.{Decrypter, Encrypter, SymmetricCryptoFactory}

import java.time.Instant
import java.time.temporal.ChronoUnit

class AgentClientDataFormatsSpec extends SpecBase {
  import AgentClientData.given

  private val cryptoKey                        = "Bk/WzqlUJk4/M279rO+BJYVtLkRq4lxH9Wn2A0k9lqo="
  private val crypto: Encrypter with Decrypter = SymmetricCryptoFactory.aesGcmCrypto(cryptoKey)

  "plainFormat" - {

    "serializes AgentClientData to JSON with raw JsValue data" in {
      val now  = Instant.parse("2024-10-01T12:34:56.789Z")
      val data = AgentClientData("123", Json.obj("a" -> 1), now)

      val json = Json.toJson(data)
      (json \ "id").as[String]           shouldBe "123"
      (json \ "data").as[JsObject]       shouldBe Json.obj("a" -> 1)
      (json \ "lastUpdated").as[Instant] shouldBe now
    }

    "deserializes AgentClientData from JSON" in {
      val now  = Instant.parse("2024-11-02T13:14:15.921Z")
      val json = Json.obj(
        "id"          -> "id42",
        "data"        -> Json.obj("x" -> 99),
        "lastUpdated" -> now
      )

      val result = json.validate[AgentClientData]
      result.isSuccess shouldBe true
      result.get       shouldBe AgentClientData("id42", Json.obj("x" -> 99), now)
    }

    "round-trips symmetrically" in {
      val now  = Instant.now().truncatedTo(ChronoUnit.MILLIS)
      val data = AgentClientData("roundtrip", Json.obj("k" -> "v"), now)
      Json.fromJson[AgentClientData](Json.toJson(data)).get shouldBe data
    }
  }

  "encryptedFormat" - {

    "encrypts the data field on write and decrypts on read" in {
      implicit val fmt: OFormat[AgentClientData] = AgentClientData.encryptedFormat(crypto)

      val now  = Instant.now().truncatedTo(ChronoUnit.MILLIS)
      val data = AgentClientData("enc-id", Json.obj("secret" -> "value"), now)

      val json = Json.toJson(data)
      ((json \ "data").as[String] == Json.stringify(data.data)) shouldBe false

      val parsed = Json.fromJson[AgentClientData](json).get
      parsed shouldBe data
    }
  }
}
