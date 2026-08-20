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

package uk.gov.hmrc.constructionindustryscheme.e2e.support

import play.api.libs.json.Json

/** Mints a local bearer token via auth-login-api (port of bearer-token.sh).
  *
  * The TaxOfficeNumber placed in the enrolment is what drives the stub error scenarios for non-agent submissions (see
  * Scenarios).
  */
object AuthLoginApi {

  /** @param enrolment
    *   Some((taxOfficeNumber, taxOfficeReference)) mints an Activated HMRC-CIS-ORG enrolment; None logs in without it
    *   (negative testing of the enrolment-driven flows, the bash -n flag)
    */
  def bearerToken(enrolment: Option[(String, String)]): String = {
    val enrolments = enrolment.fold(Json.arr()) { case (ton, tor) =>
      Json.arr(
        Json.obj(
          "key"         -> "HMRC-CIS-ORG",
          "identifiers" -> Json.arr(
            Json.obj("key" -> "TaxOfficeNumber", "value"    -> ton),
            Json.obj("key" -> "TaxOfficeReference", "value" -> tor)
          ),
          "state"       -> "Activated"
        )
      )
    }

    val payload = Json.obj(
      "credId"             -> "test-cred-id",
      "affinityGroup"      -> "Organisation",
      "confidenceLevel"    -> 200,
      "credentialStrength" -> "strong",
      "enrolments"         -> enrolments
    )

    val response = HttpSupport.post(s"${E2eConfig.authLoginHost}/government-gateway/session/login", payload.toString)

    response.headers
      .firstValue("Authorization")
      .orElseThrow(() =>
        new IllegalStateException(
          s"Failed to obtain bearer token from auth-login-api (HTTP ${response.status}): ${response.body}"
        )
      )
  }
}
