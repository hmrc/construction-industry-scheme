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

package services

import base.SpecBase
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.matchers.should.Matchers.shouldBe
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.audit.{AuditResponseReceivedModel, ClientListRetrievalFailedEvent, ClientListRetrievalInProgressEvent, MonthlyNilReturnRequestEvent, MonthlyNilReturnResponseEvent}
import uk.gov.hmrc.constructionindustryscheme.services.AuditService
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent
import scala.concurrent.Future


class AuditServiceSpec extends SpecBase {
  "AuditService" - {
    "call AuditConnector.sendExtendedEvent for MonthlyNilReturnRequestEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val jsonData = Json.obj("period" -> "2025-10", "submittedBy" -> "user123")
      val expectedEvent = MonthlyNilReturnRequestEvent(jsonData).extendedDataEvent
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service = new AuditService(mockAuditConnector)
      val resultF = service.monthlyNilReturnRequestEvent(jsonData)
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail shouldBe expectedEvent.detail
      }
    }
    "call AuditConnector.sendExtendedEvent for MonthlyNilReturnResponseEvent" in {
      val mockAuditConnector = mock[AuditConnector]
      val responseData = Json.obj("message" -> "No return required")
      val responseModel = AuditResponseReceivedModel("OK", responseData)
      val expectedEvent = MonthlyNilReturnResponseEvent(responseModel).extendedDataEvent
      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))
      val service = new AuditService(mockAuditConnector)
      val resultF = service.monthlyNilReturnResponseEvent(responseModel)
      resultF.map { result =>
        result shouldBe AuditResult.Success
        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())
        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail shouldBe expectedEvent.detail
      }
    }


    "call AuditConnector.sendExtendedEvent for ClientListRetrievalFailedEvent" in {
      val mockAuditConnector = mock[AuditConnector]

      val credentialId = "cred-123"
      val phase        = "business#1"
      val reason       = Some("no-business-intervals")

      val expectedEvent =
        ClientListRetrievalFailedEvent(credentialId, phase, reason).extendedDataEvent

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val service = new AuditService(mockAuditConnector)
      val resultF = service.clientListRetrievalFailed(credentialId, phase, reason)

      resultF.map { result =>
        result shouldBe AuditResult.Success

        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail    shouldBe expectedEvent.detail
      }
    }

    "call AuditConnector.sendExtendedEvent for ClientListRetrievalInProgressEvent" in {
      val mockAuditConnector = mock[AuditConnector]

      val credentialId = "cred-456"
      val phase        = "browser"

      val expectedEvent =
        ClientListRetrievalInProgressEvent(credentialId, phase).extendedDataEvent

      when(mockAuditConnector.sendExtendedEvent(any[ExtendedDataEvent])(any(), any()))
        .thenReturn(Future.successful(AuditResult.Success))

      val service = new AuditService(mockAuditConnector)
      val resultF = service.clientListRetrievalInProgress(credentialId, phase)

      resultF.map { result =>
        result shouldBe AuditResult.Success

        val captor = ArgumentCaptor.forClass(classOf[ExtendedDataEvent])
        verify(mockAuditConnector, times(1)).sendExtendedEvent(captor.capture())(any(), any())

        val capturedEvent = captor.getValue
        capturedEvent.auditType shouldBe expectedEvent.auditType
        capturedEvent.detail    shouldBe expectedEvent.detail
      }
    }
  }
}