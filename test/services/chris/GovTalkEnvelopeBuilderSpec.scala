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

package services.chris

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.services.chris.{EnvelopeProfile, GovTalkEnvelopeBuilder}

import scala.xml.Elem

class GovTalkEnvelopeBuilderSpec extends AnyWordSpec with Matchers {

  private val profile = EnvelopeProfile(
    messageDetailsClass = "TEST-CLASS",
    qualifier = "request",
    function = "submit",
    transformation = "XML",
    organisation = "ORG",
    channelUri = "0126",
    channelProduct = "TEST-PRODUCT",
    channelVersion = "1.0",
    defaultCurrency = "GBP",
    namespace = "http://test/namespace",
    schemaVersion = "1.1",
    topElementName = "CISrequest"
  )

  private val payload: Elem =
    <CISrequest>
      <Test>value</Test>
    </CISrequest>

  "GovTalkEnvelopeBuilder.build" should {

    "build a valid GovTalkMessage structure" in {

      val xml =
        GovTalkEnvelopeBuilder.build(
          profile = profile,
          taxOfficeNumber = "123",
          taxOfficeReference = "ABC456",
          correlationId = "CORR123",
          gatewayTimestamp = "2025-01-01T00:00:00",
          periodEnd = "2025-01-31",
          sender = "Company",
          payload = payload
        )

      xml.label mustBe "GovTalkMessage"

      (xml \\ "EnvelopeVersion").text.trim mustBe "2.0"
      (xml \\ "CorrelationID").text.trim mustBe "CORR123"
      (xml \\ "GatewayTimestamp").text.trim mustBe "2025-01-01T00:00:00"
    }

    "populate MessageDetails from profile" in {

      val xml = GovTalkEnvelopeBuilder.build(
        profile,
        "123",
        "ABC456",
        "CORR123",
        "2025-01-01T00:00:00",
        "2025-01-31",
        "Company",
        payload
      )

      (xml \\ "Class").text.trim mustBe "TEST-CLASS"
      (xml \\ "Qualifier").text.trim mustBe "request"
      (xml \\ "Function").text.trim mustBe "submit"
      (xml \\ "Transformation").text.trim mustBe "XML"
    }

    "populate GovTalkDetails correctly" in {

      val xml = GovTalkEnvelopeBuilder.build(
        profile,
        "123",
        "ABC456",
        "CORR123",
        "2025-01-01T00:00:00",
        "2025-01-31",
        "Company",
        payload
      )

      (xml \\ "GovTalkDetails" \\ "Key")
        .filter(_ \@ "Type" == "TaxOfficeNumber")
        .text
        .trim mustBe "123"

      (xml \\ "GovTalkDetails" \\ "Key")
        .filter(_ \@ "Type" == "TaxOfficeReference")
        .text
        .trim mustBe "ABC456"

      (xml \\ "Organisation").text.trim mustBe "ORG"
      (xml \\ "Product").text.trim mustBe "TEST-PRODUCT"
      (xml \\ "Version").text.trim mustBe "1.0"
    }

    "populate IRheader correctly" in {

      val xml = GovTalkEnvelopeBuilder.build(
        profile,
        "123",
        "ABC456",
        "CORR123",
        "2025-01-01T00:00:00",
        "2025-01-31",
        "Company",
        payload
      )

      (xml \\ "PeriodEnd").text.trim mustBe "2025-01-31"
      (xml \\ "DefaultCurrency").text.trim mustBe "GBP"
      (xml \\ "Sender").text.trim mustBe "Company"

      (xml \\ "IRmark").text.trim mustBe "TBC"
    }

    "include payload correctly inside IRenvelope" in {

      val xml = GovTalkEnvelopeBuilder.build(
        profile,
        "123",
        "ABC456",
        "CORR123",
        "2025-01-01T00:00:00",
        "2025-01-31",
        "Company",
        payload
      )

      (xml \\ "CISrequest").nonEmpty mustBe true
      (xml \\ "Test").text.trim mustBe "value"
    }

    "use namespace, schema version and top element from profile" in {

      val xml = GovTalkEnvelopeBuilder.build(
        profile,
        "123",
        "ABC456",
        "CORR123",
        "2025-01-01T00:00:00",
        "2025-01-31",
        "Company",
        payload
      )

      (xml \\ "Namespace").text.trim mustBe "http://test/namespace"
      (xml \\ "SchemaVersion").text.trim mustBe "1.1"
      (xml \\ "TopElementName").text.trim mustBe "CISrequest"
    }
  }
}
