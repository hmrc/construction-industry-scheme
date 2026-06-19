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

package models

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.models.CisResponseSubcontractor

class CisResponseSubcontractorSpec extends SpecBase {

  "CisResponseSubcontractor" - {
    "create an instance with the expected values" in {
      val subcontractor = CisResponseSubcontractor(
        utr = Some("1234567890"),
        partnershipUtr = Some("0987654321"),
        tradingName = Some("Test Trading"),
        foreName = Some("John"),
        middleName = Some("A"),
        surname = Some("Smith"),
        nino = Some("AB123456C"),
        matched = Some("Y"),
        taxTreatment = Some("net"),
        verificationNumber = Some("V1000000007")
      )

      subcontractor.utr mustBe Some("1234567890")
      subcontractor.partnershipUtr mustBe Some("0987654321")
      subcontractor.tradingName mustBe Some("Test Trading")
      subcontractor.foreName mustBe Some("John")
      subcontractor.middleName mustBe Some("A")
      subcontractor.surname mustBe Some("Smith")
      subcontractor.nino mustBe Some("AB123456C")
      subcontractor.matched mustBe Some("Y")
      subcontractor.taxTreatment mustBe Some("net")
      subcontractor.verificationNumber mustBe Some("V1000000007")
    }
  }
}
