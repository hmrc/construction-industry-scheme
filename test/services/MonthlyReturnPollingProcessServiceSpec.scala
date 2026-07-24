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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, times, verify, verifyNoInteractions, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.constructionindustryscheme.models.*
import uk.gov.hmrc.constructionindustryscheme.models.requests.{GetMonthlyReturnForEditRequest, SendSuccessEmailRequest, UpdateSubmissionRequest}
import uk.gov.hmrc.constructionindustryscheme.models.response.{ChrisPollResponse, GetMonthlyReturnForEditResponse, MonthlyReturnSubmissionToPoll}
import uk.gov.hmrc.constructionindustryscheme.services.{MonthlyReturnPollingProcessService, MonthlyReturnService, SubmissionService}

import java.time.{LocalDateTime, ZoneId}
import scala.concurrent.Future

class MonthlyReturnPollingProcessServiceSpec extends SpecBase with BeforeAndAfterEach {

  private val monthlyReturnService =
    mock[MonthlyReturnService]

  private val submissionService =
    mock[SubmissionService]

  private val service =
    new MonthlyReturnPollingProcessService(
      monthlyReturnService,
      submissionService
    )

  private val startTime = System.currentTimeMillis()

  override def beforeEach(): Unit = {
    super.beforeEach()

    org.mockito.Mockito.reset(
      monthlyReturnService,
      submissionService
    )
  }

  private val gatewayUrl       = "http://localhost:6997/submission/ChRIS/CISR/Filing/sync/CIS300MR"
  private val testSubmissionId = 100L
  private val testInstanceId   = "inst-1"
  private val testTaxYear      = 2026
  private val testTaxMonth     = 4

  private def makeSubmission(
    instanceId: String = testInstanceId,
    taxYear: Int = testTaxYear,
    taxMonth: Int = testTaxMonth
  ): MonthlyReturnSubmissionToPoll =
    MonthlyReturnSubmissionToPoll(
      submissionId = testSubmissionId,
      submissionType = "MONTHLY_RETURN",
      status = "STARTED",
      taxOfficeNumber = "123",
      taxOfficeReference = "AZ123",
      taxYear = taxYear,
      taxMonth = taxMonth,
      instanceId = instanceId,
      agentId = None
    )

  private def makeMonthlyReturn(
    taxYear: Int = testTaxYear,
    taxMonth: Int = testTaxMonth,
    amendment: Option[String] = None,
    status: Option[String] = Some("STARTED")
  ): MonthlyReturn =
    MonthlyReturn(
      monthlyReturnId = 1L,
      taxYear = taxYear,
      taxMonth = taxMonth,
      amendment = amendment,
      status = status
    )

  private def makeDbSubmission(
    hmrcMarkGenerated: Option[String] = Some("irmark-gen"),
    emailRecipient: Option[String] = Some("user@example.com"),
    agentId: Option[String] = None,
    status: Option[String] = Some("STARTED"),
    submissionRequestDate: Option[LocalDateTime] = None
  ): Submission =
    Submission(
      submissionId = testSubmissionId,
      submissionType = "MONTHLY_RETURN",
      activeObjectId = None,
      status = status,
      hmrcMarkGenerated = hmrcMarkGenerated,
      hmrcMarkGgis = None,
      emailRecipient = emailRecipient,
      acceptedTime = None,
      createDate = None,
      lastUpdate = None,
      schemeId = 1L,
      agentId = agentId,
      l_Migrated = None,
      submissionRequestDate = submissionRequestDate,
      govTalkErrorCode = None,
      govTalkErrorType = None,
      govTalkErrorMessage = None
    )

  private def makeDetails(
    monthlyReturn: MonthlyReturn = makeMonthlyReturn(),
    dbSubmission: Submission = makeDbSubmission()
  ): GetMonthlyReturnForEditResponse =
    GetMonthlyReturnForEditResponse(
      scheme = Seq.empty,
      monthlyReturn = Seq(monthlyReturn),
      subcontractors = Seq.empty,
      monthlyReturnItems = Seq.empty,
      submission = Seq(dbSubmission)
    )

  private def makePollResponse(
    status: SubmissionStatus,
    irMarkReceived: Option[String] = None,
    acceptedTime: Option[String] = None
  ): ChrisPollResponse =
    ChrisPollResponse(
      status = status,
      correlationId = "corr-123",
      pollUrl = None,
      pollInterval = None,
      error = None,
      irMarkReceived = irMarkReceived,
      lastMessageDate = None,
      acceptedTime = acceptedTime,
      govTalkErrorStatus = None
    )

  private def setupHappyPath(
    details: GetMonthlyReturnForEditResponse = makeDetails(),
    pollResponse: ChrisPollResponse = makePollResponse(ACCEPTED)
  ): Unit = {
    when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
      .thenReturn(Future.successful(details))
    when(submissionService.processMonthlyReturnGovTalkStatusCheck(any(), any(), any())(any()))
      .thenReturn(Future.successful(gatewayUrl))
    when(submissionService.pollSubmissionAndUpdateGovTalkStatus(any(), any(), any())(any()))
      .thenReturn(Future.successful(pollResponse))
    when(submissionService.updateSubmission(any())(any()))
      .thenReturn(Future.unit)
    when(submissionService.sendSuccessfulEmail(any(), any())(any()))
      .thenReturn(Future.unit)
  }

  "MonthlyReturnPollingProcessService" - {

    "process" - {

      "must not interact with any service when there are no submissions" in {
        service.process(Seq.empty, startTime).futureValue mustBe empty

        verifyNoInteractions(monthlyReturnService)
        verifyNoInteractions(submissionService)
      }

      "must continue processing remaining submissions when one fails" in {
        val sub1 = makeSubmission(instanceId = "inst-1", taxMonth = 1)
        val sub2 = makeSubmission(instanceId = "inst-2", taxMonth = 2)

        when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
          .thenReturn(
            Future.failed(new RuntimeException("getMonthlyReturnForEdit failed")),
            Future.successful(makeDetails())
          )
        when(submissionService.processMonthlyReturnGovTalkStatusCheck(any(), any(), any())(any()))
          .thenReturn(Future.successful(gatewayUrl))
        when(submissionService.pollSubmissionAndUpdateGovTalkStatus(any(), any(), any())(any()))
          .thenReturn(Future.successful(makePollResponse(ACCEPTED)))
        when(submissionService.updateSubmission(any())(any()))
          .thenReturn(Future.unit)
        when(submissionService.sendSuccessfulEmail(any(), any())(any()))
          .thenReturn(Future.unit)

        val result = service.process(Seq(sub1, sub2), startTime).futureValue

        result must have size 2

        result.head.submissionId mustBe sub1.submissionId.toString
        result.head.currentReturnStatus mustBe "-"
        result.head.correlationId mustBe "(not polled)"
        result.head.agentId mustBe "-"

        result(1).submissionId mustBe sub2.submissionId.toString
        result(1).currentReturnStatus mustBe "ACCEPTED"

        verify(monthlyReturnService, times(2)).getMonthlyReturnForEdit(any())(any())

        // The first submission fails at getMonthlyReturnForEdit, so only the second
        // submission continues and completes the downstream steps.
        verify(submissionService, times(1))
          .processMonthlyReturnGovTalkStatusCheck(
            eqTo("inst-2"),
            any(),
            any()
          )(any())

        verify(submissionService, times(1))
          .pollSubmissionAndUpdateGovTalkStatus(any(), any(), any())(any())
        verify(submissionService, times(1)).updateSubmission(any())(any())
      }

      "must return PollReportContent after successfully polling and updating monthly return" in {
        val submission =
          makeSubmission()

        setupHappyPath(
          pollResponse = makePollResponse(
            ACCEPTED,
            irMarkReceived = Some("irmark-recv"),
            acceptedTime = Some("2026-06-29T12:00:00Z")
          )
        )

        val result =
          service
            .process(Seq(submission), startTime)
            .futureValue

        result mustBe Seq(
          PollReportContent(
            user = testInstanceId,
            submissionType = submission.submissionType,
            submissionId = submission.submissionId.toString,
            govTalkRequestStatus = submission.status,
            currentReturnStatus = "ACCEPTED",
            employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
            correlationId = "corr-123",
            agentId = "-"
          )
        )
      }
    }

    "processSubmission" - {

      "must call getMonthlyReturnForEdit with isAmendment = false" in {
        setupHappyPath()
        val sub = makeSubmission(instanceId = "inst-99", taxYear = 2025, taxMonth = 6)

        service.process(Seq(sub), startTime).futureValue must have size 1

        verify(monthlyReturnService).getMonthlyReturnForEdit(
          eqTo(GetMonthlyReturnForEditRequest("inst-99", taxYear = 2025, taxMonth = 6, isAmendment = Some(false)))
        )(any())
      }

      "must call processMonthlyReturnGovTalkStatusCheck with instanceId and submissionId" in {
        setupHappyPath()

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService).processMonthlyReturnGovTalkStatusCheck(
          eqTo(testInstanceId),
          eqTo(testSubmissionId.toString),
          any()
        )(any())
      }

      "must call pollSubmissionAndUpdateGovTalkStatus with gatewayUrl and MonthlyReturn journey" in {
        setupHappyPath()

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService).pollSubmissionAndUpdateGovTalkStatus(
          eqTo(testSubmissionId.toString),
          eqTo(gatewayUrl),
          eqTo(ChrisPollJourney.MonthlyReturn)
        )(any())
      }

      "must call updateSubmission with correctly mapped fields from F3 details and poll response" in {
        val mr       = makeMonthlyReturn(taxYear = 2026, taxMonth = 4, amendment = Some("Y"))
        val dbSub    = makeDbSubmission(
          hmrcMarkGenerated = Some("irmark-gen"),
          emailRecipient = Some("user@example.com"),
          agentId = Some("agent-1")
        )
        val pollResp = makePollResponse(SUBMITTED, irMarkReceived = Some("irmark-recv"))

        setupHappyPath(
          makeDetails(mr, dbSub),
          pollResp
        )

        service
          .process(
            Seq(
              makeSubmission(
                taxYear = 2026,
                taxMonth = 4
              )
            ),
            startTime
          )
          .futureValue must have size 1

        verify(submissionService).updateSubmission(
          eqTo(
            UpdateSubmissionRequest(
              instanceId = testInstanceId,
              taxYear = 2026,
              taxMonth = 4,
              hmrcMarkGenerated = Some("irmark-gen"),
              submittableStatus = "SUBMITTED",
              amendment = "Y",
              hmrcMarkGgis = Some("irmark-recv"),
              submissionRequestDate = None,
              acceptedTime = None,
              emailRecipient = Some("user@example.com"),
              agentId = Some("agent-1"),
              govTalkResponse = None
            )
          )
        )(any())
      }

      "must default amendment to 'N' when it is None in monthly return" in {
        val mr = makeMonthlyReturn(amendment = None)
        setupHappyPath(makeDetails(mr))

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService).updateSubmission(
          eqTo(
            UpdateSubmissionRequest(
              instanceId = testInstanceId,
              taxYear = testTaxYear,
              taxMonth = testTaxMonth,
              hmrcMarkGenerated = Some("irmark-gen"),
              submittableStatus = "ACCEPTED",
              amendment = "N",
              emailRecipient = Some("user@example.com")
            )
          )
        )(any())
      }

      "must fail and skip remaining steps when details contain no monthly return" in {
        when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
          .thenReturn(Future.successful(makeDetails().copy(monthlyReturn = Seq.empty)))

        val result = service.process(Seq(makeSubmission()), startTime).futureValue

        result must have size 1
        result.head.currentReturnStatus mustBe "-"
        result.head.correlationId mustBe "(not polled)"
        result.head.agentId mustBe "-"

        verify(submissionService, never()).updateSubmission(any())(any())
      }

      "must fail and skip remaining steps when details contain no submission" in {
        when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
          .thenReturn(Future.successful(makeDetails().copy(submission = Seq.empty)))

        val result = service.process(Seq(makeSubmission()), startTime).futureValue

        result must have size 1
        result.head.currentReturnStatus mustBe "-"
        result.head.correlationId mustBe "(not polled)"
        result.head.agentId mustBe "-"

        verify(submissionService, never()).updateSubmission(any())(any())
      }

      "must not poll ChRIS when monthly return status does not match DB submission status" in {
        val submission =
          makeSubmission()

        val details =
          makeDetails(
            monthlyReturn = makeMonthlyReturn(
              status = Some("ACCEPTED")
            ),
            dbSubmission = makeDbSubmission(
              status = Some("STARTED")
            )
          )

        when(monthlyReturnService.getMonthlyReturnForEdit(any())(any()))
          .thenReturn(Future.successful(details))

        val result =
          service
            .process(
              Seq(submission),
              startTime
            )
            .futureValue

        result mustBe Seq(
          PollReportContent(
            user = submission.instanceId,
            submissionType = submission.submissionType,
            submissionId = submission.submissionId.toString,
            govTalkRequestStatus = submission.status,
            currentReturnStatus = "-",
            employerReference = s"${submission.taxOfficeNumber}/${submission.taxOfficeReference}",
            correlationId = "(not polled)",
            agentId = "-"
          )
        )

        verify(submissionService, never())
          .processMonthlyReturnGovTalkStatusCheck(any(), any(), any())(any())

        verify(submissionService, never())
          .pollSubmissionAndUpdateGovTalkStatus(any(), any(), any())(any())

        verify(submissionService, never())
          .updateSubmission(any())(any())

        verify(submissionService, never())
          .sendSuccessfulEmail(any(), any())(any())
      }

      "must return not polled report content when ChRIS polling fails" in {
        setupHappyPath()
        when(submissionService.pollSubmissionAndUpdateGovTalkStatus(any(), any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("ChRIS polling failed")))

        val result = service.process(Seq(makeSubmission()), startTime).futureValue

        result.head.currentReturnStatus mustBe "-"
        result.head.correlationId mustBe "(not polled)"
        verify(submissionService, never()).updateSubmission(any())(any())
        verify(submissionService, never()).sendSuccessfulEmail(any(), any())(any())
      }

      "must preserve poll status and correlation ID when update submission fails" in {
        setupHappyPath(pollResponse = makePollResponse(ACCEPTED))
        when(submissionService.updateSubmission(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("update submission failed")))

        val result = service.process(Seq(makeSubmission()), startTime).futureValue

        result.head.currentReturnStatus mustBe "ACCEPTED"
        result.head.correlationId mustBe "corr-123"
      }

      "must preserve poll status and correlation ID when sending email fails" in {
        setupHappyPath(pollResponse = makePollResponse(SUBMITTED))
        when(submissionService.sendSuccessfulEmail(any(), any())(any()))
          .thenReturn(Future.failed(new RuntimeException("send email failed")))

        val result = service.process(Seq(makeSubmission()), startTime).futureValue

        result.head.currentReturnStatus mustBe "SUBMITTED"
        result.head.correlationId mustBe "corr-123"
      }

      "must set currentReturnStatus to '-' when ChRIS poll response status is null" in {
        val pollResponse =
          ChrisPollResponse(
            status = null.asInstanceOf[SubmissionStatus],
            correlationId = "corr-123",
            pollUrl = None,
            pollInterval = None,
            error = None,
            irMarkReceived = None,
            lastMessageDate = None,
            acceptedTime = None,
            govTalkErrorStatus = None
          )

        setupHappyPath(
          pollResponse = pollResponse
        )

        val result =
          service
            .process(
              Seq(makeSubmission()),
              startTime
            )
            .futureValue

        result.head.currentReturnStatus mustBe "-"
      }
    }

    "must log warning when submission has been polling for more than 24 hours" in {
      val submission =
        makeSubmission()

      val oldSubmissionRequestDate = LocalDateTime.now(ZoneId.of("Europe/London")).minusHours(25)
      val dbSubmission             =
        makeDbSubmission(
          status = Some(submission.status),
          submissionRequestDate = Some(oldSubmissionRequestDate)
        )

      setupHappyPath(
        details = makeDetails(
          dbSubmission = dbSubmission
        )
      )

      service
        .process(
          Seq(submission),
          startTime
        )
        .futureValue must have size 1

      verify(submissionService).processMonthlyReturnGovTalkStatusCheck(
        any(),
        any(),
        any()
      )(any())

      verify(submissionService)
        .pollSubmissionAndUpdateGovTalkStatus(
          any(),
          any(),
          any()
        )(any())

      verify(submissionService)
        .updateSubmission(any())(any())
    }

    "email behaviour" - {

      "must send email when status is SUBMITTED and emailRecipient is present (AC5)" in {
        val dbSub = makeDbSubmission(emailRecipient = Some("user@example.com"))
        val mr    = makeMonthlyReturn(taxYear = 2026, taxMonth = 4)

        setupHappyPath(
          makeDetails(mr, dbSub),
          makePollResponse(SUBMITTED)
        )

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService).sendSuccessfulEmail(
          eqTo(testSubmissionId.toString),
          eqTo(SendSuccessEmailRequest("user@example.com", "4", "2026"))
        )(any())
      }

      "must send email when status is SUBMITTED_NO_RECEIPT and emailRecipient is present (AC6)" in {
        val dbSub = makeDbSubmission(emailRecipient = Some("user@example.com"))
        val mr    = makeMonthlyReturn(taxYear = 2026, taxMonth = 7)

        setupHappyPath(
          makeDetails(mr, dbSub),
          makePollResponse(SUBMITTED_NO_RECEIPT)
        )

        service.process(Seq(makeSubmission(taxMonth = 7)), startTime).futureValue must have size 1

        verify(submissionService).sendSuccessfulEmail(
          eqTo(testSubmissionId.toString),
          eqTo(SendSuccessEmailRequest("user@example.com", "7", "2026"))
        )(any())
      }

      "must send email when status is DEPARTMENTAL_ERROR and emailRecipient is present (AC7)" in {
        val dbSub = makeDbSubmission(emailRecipient = Some("user@example.com"))
        val mr    = makeMonthlyReturn(taxYear = 2026, taxMonth = 10)

        setupHappyPath(
          makeDetails(mr, dbSub),
          makePollResponse(DEPARTMENTAL_ERROR)
        )

        service
          .process(
            Seq(
              makeSubmission(
                taxMonth = 10
              )
            ),
            startTime
          )
          .futureValue must have size 1

        verify(submissionService).sendSuccessfulEmail(
          eqTo(testSubmissionId.toString),
          eqTo(SendSuccessEmailRequest("user@example.com", "10", "2026"))
        )(any())
      }

      "must not send email when status is ACCEPTED (AC2)" in {
        setupHappyPath(pollResponse = makePollResponse(ACCEPTED))

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService, never()).sendSuccessfulEmail(any(), any())(any())
      }

      "must not send email when status is STARTED (AC8)" in {
        setupHappyPath(pollResponse = makePollResponse(STARTED))

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService, never()).sendSuccessfulEmail(any(), any())(any())
      }

      "must not send email when status is FATAL_ERROR (AC9)" in {
        setupHappyPath(pollResponse = makePollResponse(FATAL_ERROR))

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService, never()).sendSuccessfulEmail(any(), any())(any())
      }

      "must not send email when emailRecipient is None even for SUBMITTED status" in {
        val dbSub = makeDbSubmission(emailRecipient = None)

        setupHappyPath(
          makeDetails(dbSubmission = dbSub),
          makePollResponse(SUBMITTED)
        )

        service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

        verify(submissionService, never()).sendSuccessfulEmail(any(), any())(any())
      }
    }

    "status mapping (AC1)" - {

      Seq(
        ACCEPTED             -> "ACCEPTED",
        SUBMITTED            -> "SUBMITTED",
        SUBMITTED_NO_RECEIPT -> "SUBMITTED_NO_RECEIPT",
        DEPARTMENTAL_ERROR   -> "DEPARTMENTAL_ERROR",
        STARTED              -> "STARTED",
        FATAL_ERROR          -> "FATAL_ERROR"
      ).foreach { case (status, expectedStatusString) =>
        s"must set submittableStatus to '$expectedStatusString' in UpdateSubmissionRequest" in {
          setupHappyPath(pollResponse = makePollResponse(status))

          service.process(Seq(makeSubmission()), startTime).futureValue must have size 1

          verify(submissionService).updateSubmission(
            eqTo(
              UpdateSubmissionRequest(
                instanceId = testInstanceId,
                taxYear = testTaxYear,
                taxMonth = testTaxMonth,
                hmrcMarkGenerated = Some("irmark-gen"),
                submittableStatus = expectedStatusString,
                amendment = "N",
                emailRecipient = Some("user@example.com")
              )
            )
          )(any())
        }
      }
    }
  }
}
