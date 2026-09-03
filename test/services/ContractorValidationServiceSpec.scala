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
import org.mockito.Mockito.{verify, when}
import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.ContractorScheme
import uk.gov.hmrc.constructionindustryscheme.services.ContractorValidationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class ContractorValidationServiceSpec extends SpecBase {

  private val instanceId = "inst-001"

  private val baseScheme = ContractorScheme(
    schemeId = 1,
    instanceId = instanceId,
    accountsOfficeReference = "123PX00123456",
    taxOfficeNumber = "123",
    taxOfficeReference = "AB456"
  )

  private def mkService(connector: FormpProxyConnector) =
    new ContractorValidationService(connector)

  private def stubScheme(connector: FormpProxyConnector, scheme: ContractorScheme): Unit =
    when(connector.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
      .thenReturn(Future.successful(Some(scheme)))

  "validateContractorDetails" - {

    "returns None when the connector returns None" in {
      val connector = mock[FormpProxyConnector]
      when(connector.getContractorScheme(eqTo(instanceId))(any[HeaderCarrier]))
        .thenReturn(Future.successful(None))

      mkService(connector).validateContractorDetails(instanceId).futureValue mustBe None

      verify(connector).getContractorScheme(eqTo(instanceId))(any[HeaderCarrier])
    }

    "returns Some with all fields valid when scheme has valid data" in {
      val connector = mock[FormpProxyConnector]
      val scheme    = baseScheme.copy(
        utr = Some("2234567890"),
        name = Some("ACME Ltd"),
        emailAddress = Some("test@example.com")
      )
      stubScheme(connector, scheme)

      val result = mkService(connector).validateContractorDetails(instanceId).futureValue

      result.value.utrValid mustBe true
      result.value.schemeNameValid mustBe true
      result.value.emailAddressValid mustBe true
      result.value.scheme mustBe scheme
    }

    "UTR validation" - {

      "utrValid is false when UTR is absent" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = None))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe false
      }

      "utrValid is false when UTR contains non-numeric characters" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = Some("12345abcde")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe false
      }

      "utrValid is false when UTR is fewer than 10 digits" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = Some("123")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe false
      }

      "utrValid is false when UTR is more than 10 digits" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = Some("12345678901")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe false
      }

      "utrValid is false when 10 digits fail the UTR algorithm" in {
        // 0234567890: sum=264, 264%11=0, lookup(0)=2, first digit 0 ≠ 2
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = Some("0234567890")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe false
      }

      "utrValid is true for a valid 10-digit UTR passing the algorithm" in {
        // 2234567890: sum=264, 264%11=0, lookup(0)=2, first digit 2 ✓
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(utr = Some("2234567890")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.utrValid mustBe true
      }
    }

    "scheme name validation" - {

      "schemeNameValid is true when name is absent" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(name = None))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.schemeNameValid mustBe true
      }

      "schemeNameValid is true for a standard valid name" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(name = Some("ACME Ltd")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.schemeNameValid mustBe true
      }

      "schemeNameValid is true for a name with allowed special characters" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(name = Some("A & B (Holdings) Ltd")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.schemeNameValid mustBe true
      }

      "schemeNameValid is false when name exceeds 56 characters" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(name = Some("A" * 57)))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.schemeNameValid mustBe false
      }

      "schemeNameValid is false when name contains a disallowed character" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(name = Some("Bad<Name")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.schemeNameValid mustBe false
      }
    }

    "email validation" - {

      "emailAddressValid is true when email is absent" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(emailAddress = None))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.emailAddressValid mustBe true
      }

      "emailAddressValid is true for a valid email" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(emailAddress = Some("test@example.com")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.emailAddressValid mustBe true
      }

      "emailAddressValid is false when email has no @ symbol" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(emailAddress = Some("notanemail")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.emailAddressValid mustBe false
      }

      "emailAddressValid is false when email exceeds 256 characters" in {
        val connector = mock[FormpProxyConnector]
        val longLocal = "a" * 250
        stubScheme(connector, baseScheme.copy(emailAddress = Some(s"$longLocal@example.com")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.emailAddressValid mustBe false
      }

      "emailAddressValid is false when email local part contains a space" in {
        val connector = mock[FormpProxyConnector]
        stubScheme(connector, baseScheme.copy(emailAddress = Some("bad space@example.com")))
        mkService(connector).validateContractorDetails(instanceId).futureValue.value.emailAddressValid mustBe false
      }
    }
  }
}
