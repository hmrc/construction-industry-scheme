/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.constructionindustryscheme.controllers

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.must.Matchers
import org.scalatest.matchers.must.Matchers.mustBe
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.OptionValues
import org.scalatest.EitherValues.*
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, CREATED, NO_CONTENT, UNAUTHORIZED}
import play.api.libs.json.{JsNull, JsObject, JsValue, Json}
import uk.gov.hmrc.constructionindustryscheme.itutil.{ApplicationWithWiremock, AuthStub}
import uk.gov.hmrc.constructionindustryscheme.repositories.{ChrisSubmissionSessionData, ChrisSubmissionSessionRepository, StoredVerificationContext}

import java.time.{Instant, LocalDateTime}

class SubmissionControllerIntegrationSpec
    extends ApplicationWithWiremock
    with Matchers
    with ScalaFutures
    with IntegrationPatience
    with OptionValues {

  private val submissionId              = "sub-123"
  private val validRequestJson: JsValue = Json.obj(
    "utr"                   -> "1234567890",
    "aoReference"           -> "754PT00002240",
    "informationCorrect"    -> "yes",
    "inactivity"            -> "yes",
    "monthYear"             -> "2025-09",
    "email"                 -> "test@test.com",
    "isAgent"               -> false,
    "clientTaxOfficeNumber" -> "",
    "clientTaxOfficeRef"    -> "",
    "returnType"            -> "MonthlyNilReturn"
  )

  private val validVerificationRequestJson: JsObject = Json.obj(
    "instanceId"                   -> "id-1",
    "isAgent"                      -> true,
    "clientTaxOfficeNumber"        -> "999",
    "clientTaxOfficeRef"           -> "XYZ123",
    "contractorUTR"                -> "1234567890",
    "contractorAORef"              -> "123/AB456",
    "verificationBatchId"          -> "batch-1",
    "verificationBatchResourceRef" -> "5",
    "emailRecipient"               -> "test@test.com",
    "subcontractors"               -> Json.arr(
      Json.obj(
        "subcontractorId"        -> 1L,
        "subbieResourceRef"      -> 10L,
        "firstName"              -> "John",
        "secondName"             -> "Q",
        "surname"                -> "Smith",
        "tradingName"            -> "ACME",
        "utr"                    -> "1111111111",
        "nino"                   -> "AA123456A",
        "crn"                    -> "AC012345",
        "partnerUtr"             -> "5860920998",
        "partnershipTradingName" -> "ACME trading",
        "subcontractorType"      -> "soletrader",
        "addressLine1"           -> "Line 1",
        "addressLine2"           -> "Line 2",
        "addressLine3"           -> "Line 3",
        "addressLine4"           -> "Line 4",
        "country"                -> "UK",
        "postcode"               -> "NE1 1AA",
        "worksReferenceNumber"   -> "WRN123"
      )
    ),
    "verifications"                -> Json.arr(
      Json.obj(
        "subcontractorName"       -> "John Smith",
        "verificationResourceRef" -> "10",
        "proceedVerification"     -> true
      )
    )
  )

  private val createUrl                                = s"$base/submissions/create"
  private def submitToChrisUrl(id: String)             = s"$base/submissions/$id/submit-to-chris"
  private def updateUrl(id: String)                    = s"$base/submissions/$id/update"
  private def submitVerificationToChrisUrl(id: String) =
    s"$base/submissions/$id/submit-verification-to-chris"

  private def pollVerificationUrl(id: String, pollUrl: String) =
    s"$base/submissions/verification/poll?submissionId=$id&pollUrl=$pollUrl"

  private lazy val sessionRepository = app.injector.instanceOf[ChrisSubmissionSessionRepository]

  private def verificationSession(id: String, correlationId: String, pollUrl: String): ChrisSubmissionSessionData =
    ChrisSubmissionSessionData(
      submissionId = id,
      instanceId = "instance-123",
      correlationId = correlationId,
      lastMessageDate = Instant.parse("2026-06-19T10:00:00Z"),
      numPolls = 0,
      pollInterval = 10,
      pollUrl = pollUrl,
      govTalkStatus = None,
      verificationContext = Some(
        StoredVerificationContext(
          verificationBatchResourceRef = 5L,
          hmrcMarkGenerated = "hmrc-mark",
          submissionRequestDate = LocalDateTime.parse("2026-06-19T10:00:00"),
          actionIndicators = Seq.empty,
          requestedVerifications = Seq.empty
        )
      )
    )

  private def stubGovTalkStatus(id: String, correlationId: String, pollUrl: String): Unit =
    stubFor(
      post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/get"))
        .withQueryParam("stage", equalTo("polling"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              Json
                .obj(
                  "govtalk_status" -> Json.arr(
                    Json.obj(
                      "userIdentifier"  -> "instance-123",
                      "formResultID"     -> id,
                      "correlationID"    -> correlationId,
                      "formLock"         -> "N",
                      "createDate"       -> "2026-06-19T10:00:00",
                      "endStateDate"     -> JsNull,
                      "lastMessageDate"  -> "2026-06-19T10:00:00",
                      "numPolls"         -> 0,
                      "pollInterval"     -> 10,
                      "protocolStatus"   -> "dataRequest",
                      "gatewayURL"       -> pollUrl
                    )
                  )
                )
                .toString()
            )
        )
    )

  private def stubGovTalkUpdateSteps(): Unit = {
    stubFor(post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-correlationID")).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-statistics")).willReturn(aResponse().withStatus(204)))
    stubFor(post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/update-status")).willReturn(aResponse().withStatus(204)))
  }

  private def stubInitialGovTalkStatusNotFound(): Unit =
    stubFor(
      post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/get"))
        .withQueryParam("stage", equalTo("initial"))
        .willReturn(aResponse().withStatus(404))
    )

  private def stubCreateGovTalkStatus(): Unit =
    stubFor(
      post(urlPathEqualTo("/formp-proxy/cis/govtalkstatus/create"))
        .willReturn(aResponse().withStatus(201))
    )

  private def stubCisTaxpayer(): Unit =
    stubFor(
      post(urlPathEqualTo("/rds-datacache-proxy/cis-taxpayer"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              """{
                |  "uniqueId": "instance-123",
                |  "taxOfficeNumber": "999",
                |  "taxOfficeRef": "XYZ123",
                |  "employerName1": "TEST LTD"
                |}""".stripMargin
            )
        )
    )

  private def stubVerificationSubmissionUpdate(
    expectedStatus: String,
    expectedCode: String,
    expectedType: String,
    expectedMessage: String
  ): Unit =
    stubFor(
      post(urlPathEqualTo("/formp-proxy/cis/verification/submission/update"))
        .withRequestBody(matchingJsonPath("$.instanceId", equalTo("instance-123")))
        .withRequestBody(matchingJsonPath("$.verificationBatchResourceRef", equalTo("5")))
        .withRequestBody(matchingJsonPath("$.submittableStatus", equalTo(expectedStatus)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorCode", equalTo(expectedCode)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorType", equalTo(expectedType)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorMessage", equalTo(expectedMessage)))
        .willReturn(aResponse().withStatus(204))
    )

  private def verifyVerificationSubmissionUpdate(
    expectedStatus: String,
    expectedCode: String,
    expectedType: String,
    expectedMessage: String
  ): Unit =
    verify(
      postRequestedFor(urlPathEqualTo("/formp-proxy/cis/verification/submission/update"))
        .withRequestBody(matchingJsonPath("$.submittableStatus", equalTo(expectedStatus)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorCode", equalTo(expectedCode)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorType", equalTo(expectedType)))
        .withRequestBody(matchingJsonPath("$.govtalkErrorMessage", equalTo(expectedMessage)))
    )

  private def verificationGovTalkErrorXml(
    correlationId: String,
    raisedBy: String,
    code: String,
    errorType: String,
    text: String
  ): String =
    s"""<GovTalkMessage>
       |  <Header>
       |    <MessageDetails>
       |      <Qualifier>error</Qualifier>
       |      <CorrelationID>$correlationId</CorrelationID>
       |      <GatewayTimestamp>2026-06-19T10:01:00</GatewayTimestamp>
       |    </MessageDetails>
       |  </Header>
       |  <GovTalkDetails>
       |    <GovTalkErrors>
       |      <Error>
       |        <RaisedBy>$raisedBy</RaisedBy>
       |        <Number>$code</Number>
       |        <Type>$errorType</Type>
       |        <Text>$text</Text>
       |      </Error>
       |    </GovTalkErrors>
       |  </GovTalkDetails>
       |</GovTalkMessage>
       |""".stripMargin

  private def runVerificationPollUpdateTest(
    id: String,
    path: String,
    chrisResponse: com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder,
    expectedStatus: String,
    expectedCode: String,
    expectedType: String,
    expectedMessage: String
  ): Unit = {
    AuthStub.authorisedWithoutCisEnrolment()

    val correlationId = s"corr-$id"
    val pollUrl       = s"http://localhost:11111$path"

    sessionRepository.upsert(verificationSession(id, correlationId, pollUrl)).futureValue
    stubGovTalkStatus(id, correlationId, pollUrl)
    stubGovTalkUpdateSteps()
    stubVerificationSubmissionUpdate(expectedStatus, expectedCode, expectedType, expectedMessage)

    stubFor(
      post(urlPathEqualTo(path))
        .willReturn(chrisResponse)
    )

    val response = getJson(
      pollVerificationUrl(id, pollUrl),
      "X-Session-Id"  -> "Session-123",
      "Authorization" -> "Bearer it-token"
    )

    response.status mustBe play.api.http.Status.OK
    verifyVerificationSubmissionUpdate(expectedStatus, expectedCode, expectedType, expectedMessage)
  }

  private def runInitialVerificationFailureTest(
    id: String,
    chrisResponse: com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder,
    expectedCode: String,
    expectedMessage: String
  ): Unit = {
    AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "999", taxOfficeReference = "XYZ123")
    stubCisTaxpayer()
    stubInitialGovTalkStatusNotFound()
    stubCreateGovTalkStatus()
    stubGovTalkUpdateSteps()
    stubVerificationSubmissionUpdate("FATAL_ERROR", expectedCode, "timeOut", expectedMessage)

    stubFor(
      post(urlPathEqualTo("/submission/ChRIS/CISR/Filing/sync/CISVERIFY"))
        .willReturn(chrisResponse)
    )

    val response = postJson(
      submitVerificationToChrisUrl(id),
      validVerificationRequestJson,
      "X-Session-Id"  -> "Session-123",
      "Authorization" -> "Bearer it-token"
    )

    response.status mustBe play.api.http.Status.OK
    (response.json \ "status").as[String] mustBe "FATAL_ERROR"
    verifyVerificationSubmissionUpdate("FATAL_ERROR", expectedCode, "timeOut", expectedMessage)
  }

  "POST /cis/submissions/create" should {

    "returns 201 with submissionId when authorised and JSON is valid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val body = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "amendment"         -> "N",
        "hmrcMarkGenerated" -> "Y"
      )

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/create"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(matchingJsonPath("$.instanceId"))
          .withRequestBody(matchingJsonPath("$.taxYear"))
          .withRequestBody(matchingJsonPath("$.taxMonth"))
          .willReturn(
            aResponse()
              .withStatus(201)
              .withHeader("Content-Type", "application/json")
              .withBody("""{ "submissionId": "sub-123" }""")
          )
      )

      val res = postJson(createUrl, body, "X-Session-Id" -> "Session-123", "Authorization" -> "Bearer it-token")

      res.status mustBe CREATED
      (res.json \ "submissionId").asOpt[String].value.length must be > 0
    }

    "returns 400 when JSON is invalid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val response = postJsonEither(
        createUrl,
        Json.obj("taxYear" -> 2024),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      val err = response.left.value
      err.statusCode mustBe BAD_REQUEST
    }

    "returns 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        createUrl,
        Json.obj("instanceId" -> "123", "taxYear" -> 2024, "taxMonth" -> 4)
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }
  }

  "POST /cis/chris (submitNilMonthlyReturn)" should {

    "return 400 when request JSON is invalid" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      val invalidJson = Json.obj(
        "utr"       -> 123,
        "monthYear" -> "2025-09"
      )

      val response = postJsonEither(
        submitToChrisUrl(submissionId),
        invalidJson,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        submitToChrisUrl(submissionId),
        validRequestJson
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }
  }

  "POST /cis/submissions/:id/update" should {

    "return 204 when authorised and JSON is valid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withRequestBody(matchingJsonPath("$.instanceId"))
          .withRequestBody(matchingJsonPath("$.taxYear"))
          .withRequestBody(matchingJsonPath("$.taxMonth"))
          .withRequestBody(matchingJsonPath("$.submittableStatus"))
          .willReturn(aResponse().withStatus(204))
      )

      val json = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "amendment"         -> "N",
        "submittableStatus" -> "ACCEPTED"
      )

      val res = postJson(
        updateUrl(submissionId),
        json,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      res.status mustBe NO_CONTENT
    }

    "return 400 when JSON is invalid" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val response = postJsonEither(
        updateUrl(submissionId),
        Json.obj("taxYear" -> 2024),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        updateUrl(submissionId),
        Json.obj(
          "instanceId"        -> "123",
          "taxYear"           -> 2024,
          "taxMonth"          -> 4,
          "submittableStatus" -> "ACCEPTED"
        )
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }

    "return 502 when downstream formp-proxy is non-2xx (e.g. 502)" in {
      AuthStub.authorisedWithoutCisEnrolment()

      stubFor(
        post(urlPathEqualTo("/formp-proxy/submissions/update"))
          .willReturn(aResponse().withStatus(502).withBody("bad gateway"))
      )

      val json = Json.obj(
        "instanceId"        -> "123",
        "taxYear"           -> 2024,
        "taxMonth"          -> 4,
        "amendment"         -> "N",
        "submittableStatus" -> "REJECTED"
      )

      val response = postJsonEither(
        updateUrl(submissionId),
        json,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_GATEWAY
    }
  }

  "POST /cis/submissions/:id/submit-verification-to-chris" should {

    "return 400 when request JSON is invalid" in {
      AuthStub.authorisedWithCisEnrolment(taxOfficeNumber = "123", taxOfficeReference = "AB456")

      val invalidJson = Json.obj(
        "instanceId" -> "id-1",
        "isAgent"    -> true
      )

      val response = postJsonEither(
        submitVerificationToChrisUrl(submissionId),
        invalidJson,
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = postJsonEither(
        submitVerificationToChrisUrl(submissionId),
        validVerificationRequestJson
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }

    "AC1 update FormP with FATAL_ERROR timeout details when initial verification request returns 500" in {
      runInitialVerificationFailureTest(
        id = "sub-initial-500",
        chrisResponse = aResponse().withStatus(500).withBody("Internal Server Error"),
        expectedCode = "500",
        expectedMessage = "timeOut"
      )
    }

    "AC2 update FormP with FATAL_ERROR timeout details when initial verification request has a connection failure" in {
      runInitialVerificationFailureTest(
        id = "sub-initial-conn",
        chrisResponse = aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER),
        expectedCode = "xxxx",
        expectedMessage = "timed out"
      )
    }
  }

  "GET /cis/submissions/verification/poll" should {

    "return 401 when unauthorised" in {
      AuthStub.noActiveSession()

      val response = getEither(
        pollVerificationUrl(submissionId, "http://chris.test/poll")
      )

      response.swap.value.statusCode mustBe UNAUTHORIZED
    }

    "return 400 when pollUrl host is not allowed" in {
      AuthStub.authorisedWithoutCisEnrolment()

      val response = getEither(
        pollVerificationUrl(submissionId, "http://bad.host/poll"),
        "X-Session-Id"  -> "Session-123",
        "Authorization" -> "Bearer it-token"
      )

      response.swap.value.statusCode mustBe BAD_REQUEST
    }

    "AC3 update FormP with ACCEPTED timeout details when verification poll returns 500" in {
      runVerificationPollUpdateTest(
        id = "sub-poll-500",
        path = "/poll/verification-500",
        chrisResponse = aResponse().withStatus(500).withBody("Internal Server Error"),
        expectedStatus = "ACCEPTED",
        expectedCode = "500",
        expectedType = "timeOut",
        expectedMessage = "timeOut"
      )
    }

    "AC4 update FormP with ACCEPTED timeout details when verification poll has a connection failure" in {
      runVerificationPollUpdateTest(
        id = "sub-poll-conn",
        path = "/poll/verification-conn",
        chrisResponse = aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER),
        expectedStatus = "ACCEPTED",
        expectedCode = "xxxx",
        expectedType = "timeOut",
        expectedMessage = "timeOut"
      )
    }

    "AC5 update FormP with departmentalError details when verification poll returns code 3001" in {
      val id        = "sub-poll-3001"
      val path      = "/poll/verification-3001"
      val errorText = "Your submission failed due to business validation errors."

      runVerificationPollUpdateTest(
        id = id,
        path = path,
        chrisResponse = aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(verificationGovTalkErrorXml(s"corr-$id", "Gateway", "3001", "fatal", errorText)),
        expectedStatus = "DEPARTMENTAL_ERROR",
        expectedCode = "3001",
        expectedType = "departmentalError",
        expectedMessage = errorText
      )
    }

    "AC6 update FormP with departmentalError details when verification poll returns business type" in {
      val id        = "sub-poll-business"
      val errorText = "Verification failed due to ChRIS business validation."

      runVerificationPollUpdateTest(
        id = id,
        path = "/poll/verification-business",
        chrisResponse = aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(verificationGovTalkErrorXml(s"corr-$id", "Gateway", "3999", "business", errorText)),
        expectedStatus = "DEPARTMENTAL_ERROR",
        expectedCode = "3999",
        expectedType = "departmentalError",
        expectedMessage = errorText
      )
    }

    "AC7 update FormP with departmentalError details when verification poll returns RaisedBy Department" in {
      val id        = "sub-poll-department"
      val errorText = "Verification failed due to a departmental response."

      runVerificationPollUpdateTest(
        id = id,
        path = "/poll/verification-department",
        chrisResponse = aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(verificationGovTalkErrorXml(s"corr-$id", "Department", "3998", "warning", errorText)),
        expectedStatus = "DEPARTMENTAL_ERROR",
        expectedCode = "3998",
        expectedType = "departmentalError",
        expectedMessage = errorText
      )
    }

    Seq("3000", "1000", "2005").foreach { code =>
      s"AC8 update FormP with FATAL_ERROR systemError details when verification poll returns fatal code $code" in {
        val id        = s"sub-poll-fatal-$code"
        val errorText = s"Verification failed with a fatal $code error."

        runVerificationPollUpdateTest(
          id = id,
          path = s"/poll/verification-fatal-$code",
          chrisResponse = aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/xml")
            .withBody(verificationGovTalkErrorXml(s"corr-$id", "Department", code, "fatal", errorText)),
          expectedStatus = "FATAL_ERROR",
          expectedCode = code,
          expectedType = "systemError",
          expectedMessage = errorText
        )
      }
    }

    "AC9 update FormP with FATAL_ERROR systemError details when verification poll returns any other fatal code" in {
      val id        = "sub-poll-fatal-1020"
      val errorText = "Verification failed with a fatal 1020 error."

      runVerificationPollUpdateTest(
        id = id,
        path = "/poll/verification-fatal-1020",
        chrisResponse = aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(verificationGovTalkErrorXml(s"corr-$id", "Gateway", "1020", "fatal", errorText)),
        expectedStatus = "FATAL_ERROR",
        expectedCode = "1020",
        expectedType = "systemError",
        expectedMessage = errorText
      )
    }

    "AC10 update FormP with FATAL_ERROR systemError details for unmapped verification poll error" in {
      val id        = "sub-poll-unknown"
      val errorText = "Verification failed with an unmapped ChRIS error."

      runVerificationPollUpdateTest(
        id = id,
        path = "/poll/verification-unknown",
        chrisResponse = aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/xml")
          .withBody(verificationGovTalkErrorXml(s"corr-$id", "Gateway", "7000", "technical", errorText)),
        expectedStatus = "FATAL_ERROR",
        expectedCode = "7000",
        expectedType = "systemError",
        expectedMessage = errorText
      )
    }
  }

}
