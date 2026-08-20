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

/** Calls the backend's verification endpoints (port of call-backend.sh). */
object BackendClient {

  /** POST /cis/submissions/:submissionId/submit-verification-to-chris
    *
    * VerificationSubmissionContextBuilder joins verifications to subcontractors by verificationResourceRef ==
    * subbieResourceRef (as string); mismatched refs -> 400. The subcontractor identity (names/tradingName/utr) must
    * match the hardcoded subcontractor in the stub's submitCISVerifyMessage-success-response.xml, or
    * VerificationResultMapper fails the poll with "No matching requested verification" -> backend 500.
    */
  def submitVerification(
    submissionId: String,
    isAgent: Boolean,
    bodyTon: String,
    bodyTor: String,
    token: String,
    sessionId: String
  ): SimpleResponse = {
    val body = Json.obj(
      "instanceId"                   -> s"e2e-instance-$submissionId",
      "isAgent"                      -> isAgent,
      "clientTaxOfficeNumber"        -> bodyTon,
      "clientTaxOfficeRef"           -> bodyTor,
      "contractorUTR"                -> "1234567890",
      "contractorAORef"              -> "123PP87654321",
      "verificationBatchId"          -> s"batch-$submissionId",
      "verificationBatchResourceRef" -> "77",
      "emailRecipient"               -> "test@test.com",
      "subcontractors"               -> Json.arr(
        Json.obj(
          "subcontractorId"      -> 1,
          "subbieResourceRef"    -> 10,
          "firstName"            -> "Noel",
          "surname"              -> "Armstrong",
          "tradingName"          -> "DBB Construction",
          "utr"                  -> "8786438047",
          "nino"                 -> "AB623456C",
          "subcontractorType"    -> "soletrader",
          "addressLine1"         -> "1 Test Street",
          "postcode"             -> "NE1 1AA",
          "worksReferenceNumber" -> "WRN123"
        )
      ),
      "verifications"                -> Json.arr(
        Json.obj(
          "subcontractorName"       -> "Noel Armstrong",
          "verificationResourceRef" -> "10",
          "proceedVerification"     -> true
        )
      )
    )

    HttpSupport.post(
      s"${E2eConfig.backendHost}${E2eConfig.routePrefix}/submissions/$submissionId/submit-verification-to-chris",
      body.toString,
      authHeaders(token, sessionId)
    )
  }

  /** GET /cis/submissions/verification/poll?pollUrl=...&submissionId=... (use the responseEndPoint.url returned by a
    * 202 submit)
    */
  def poll(pollUrl: String, submissionId: String, token: String, sessionId: String): SimpleResponse =
    HttpSupport.get(
      s"${E2eConfig.backendHost}${E2eConfig.routePrefix}/submissions/verification/poll" +
        s"?pollUrl=${HttpSupport.encode(pollUrl)}&submissionId=${HttpSupport.encode(submissionId)}",
      authHeaders(token, sessionId)
    )

  private def authHeaders(token: String, sessionId: String): Map[String, String] =
    Map("Authorization" -> token, "X-Session-ID" -> sessionId)
}
