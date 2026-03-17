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

package services.chris.xml

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.constructionindustryscheme.services.chris.xml.ChrisEnvelopeXmlBuilder

import scala.xml.{Elem, Node}

class ChrisEnvelopeXmlBuilderSpec extends AnyWordSpec with Matchers {

  "ChrisEnvelopeXmlBuilder.buildEnvelope" should {
    "build envelope with correlationId, keys, periodEnd, sender and include cisReturn" in {
      val cisReturn: Node = <CISreturn><Test>ok</Test></CISreturn>

      val xml: Elem = ChrisEnvelopeXmlBuilder.buildEnvelope(
        taxOfficeNumber = "123",
        taxOfficeReference = "ABC456",
        correlationId = "corr-id",
        gatewayTimestamp = "2025-05-01T12:00:00",
        periodEnd = "2025-05-05",
        sender = "Company",
        cisReturn = cisReturn
      )

      (xml \\ "CorrelationID").text mustBe "corr-id"

      val keys = xml \\ "Key"
      keys.find(_ \@ "Type" == "TaxOfficeNumber").map(_.text).getOrElse("") mustBe "123"
      keys.find(_ \@ "Type" == "TaxOfficeReference").map(_.text).getOrElse("") mustBe "ABC456"

      (xml \\ "PeriodEnd").text mustBe "2025-05-05"
      (xml \\ "Sender").text mustBe "Company"

      (xml \\ "CISreturn" \\ "Test").text mustBe "ok"
    }
  }
}
