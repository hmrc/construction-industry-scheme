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

package models.requests

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

import base.SpecBase
import play.api.libs.json.Json
import uk.gov.hmrc.constructionindustryscheme.models.requests.UpdateSubcontractorForEditRequest

class UpdateSubcontractorForEditRequestSpec extends SpecBase {

  "UpdateSubcontractorForEditRequest" - {

    "deserialize from valid JSON" in {

      val json = Json.parse(
        """
          |{
          |  "cisId": "123",
          |  "subbieResourceRef": 999,
          |  "utr": "1234567890",
          |  "pageVisited": 5,
          |  "partnerUtr": "2222222222",
          |  "crn": "CRN123",
          |  "firstName": "John",
          |  "nino": "AA123456A",
          |  "secondName": "Q",
          |  "surname": "Smith",
          |  "partnershipTradingName": "My Partnership",
          |  "tradingName": "ABC Ltd",
          |  "addressLine1": "1 Main Street",
          |  "addressLine2": "Line 2",
          |  "addressLine3": "Line 3",
          |  "addressLine4": "Line 4",
          |  "country": "United Kingdom",
          |  "postcode": "AA1 1AA",
          |  "emailAddress": "test@test.com",
          |  "phoneNumber": "01234567890",
          |  "mobilePhoneNumber": "07123456789",
          |  "worksReferenceNumber": "WRN123",
          |  "matched": "Y",
          |  "autoVerified": "Y",
          |  "version": 1
          |}
          |""".stripMargin
      )

      json.as[UpdateSubcontractorForEditRequest] mustBe
        UpdateSubcontractorForEditRequest(
          cisId = "123",
          subbieResourceRef = 999L,
          utr = Some("1234567890"),
          pageVisited = Some(5),
          partnerUtr = Some("2222222222"),
          crn = Some("CRN123"),
          firstName = Some("John"),
          nino = Some("AA123456A"),
          secondName = Some("Q"),
          surname = Some("Smith"),
          partnershipTradingName = Some("My Partnership"),
          tradingName = Some("ABC Ltd"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = Some("Line 2"),
          addressLine3 = Some("Line 3"),
          addressLine4 = Some("Line 4"),
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN123"),
          matched = Some("Y"),
          autoVerified = Some("Y"),
          version = Some(1)
        )
    }

    "serialize to JSON" in {

      val request =
        UpdateSubcontractorForEditRequest(
          cisId = "123",
          subbieResourceRef = 999L,
          utr = Some("1234567890"),
          pageVisited = Some(5),
          partnerUtr = None,
          crn = Some("CRN123"),
          firstName = Some("John"),
          nino = Some("AA123456A"),
          secondName = None,
          surname = Some("Smith"),
          partnershipTradingName = None,
          tradingName = Some("ABC Ltd"),
          addressLine1 = Some("1 Main Street"),
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          country = Some("United Kingdom"),
          postcode = Some("AA1 1AA"),
          emailAddress = Some("test@test.com"),
          phoneNumber = Some("01234567890"),
          mobilePhoneNumber = Some("07123456789"),
          worksReferenceNumber = Some("WRN123"),
          matched = Some("Y"),
          autoVerified = Some("Y"),
          version = Some(1)
        )

      val json = Json.toJson(request)

      (json \ "cisId").as[String] mustBe "123"
      (json \ "subbieResourceRef").as[Long] mustBe 999L
      (json \ "utr").as[String] mustBe "1234567890"
      (json \ "firstName").as[String] mustBe "John"
      (json \ "matched").as[String] mustBe "Y"
      (json \ "version").as[Int] mustBe 1
    }

    "deserialize when optional fields are missing" in {

      val json = Json.parse(
        """
          |{
          |  "cisId": "123",
          |  "subbieResourceRef": 999
          |}
          |""".stripMargin
      )

      json.as[UpdateSubcontractorForEditRequest] mustBe
        UpdateSubcontractorForEditRequest(
          cisId = "123",
          subbieResourceRef = 999L,
          utr = None,
          pageVisited = None,
          partnerUtr = None,
          crn = None,
          firstName = None,
          nino = None,
          secondName = None,
          surname = None,
          partnershipTradingName = None,
          tradingName = None,
          addressLine1 = None,
          addressLine2 = None,
          addressLine3 = None,
          addressLine4 = None,
          country = None,
          postcode = None,
          emailAddress = None,
          phoneNumber = None,
          mobilePhoneNumber = None,
          worksReferenceNumber = None,
          matched = None,
          autoVerified = None,
          version = None
        )
    }
  }
}
