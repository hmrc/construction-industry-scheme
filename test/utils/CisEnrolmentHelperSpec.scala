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

package utils

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.constructionindustryscheme.utils.CisEnrolmentHelper

class CisEnrolmentHelperSpec extends AnyWordSpec with Matchers {

  private def enrolmentsWith(
    taxOfficeNumber: Option[String],
    taxOfficeReference: Option[String]
  ): Enrolments = {

    val identifiers =
      Seq(
        taxOfficeNumber.map(v => EnrolmentIdentifier("TaxOfficeNumber", v)),
        taxOfficeReference.map(v => EnrolmentIdentifier("TaxOfficeReference", v))
      ).flatten

    Enrolments(
      Set(
        Enrolment(
          key = "HMRC-CIS-ORG",
          identifiers = identifiers,
          state = "Activated",
          delegatedAuthRule = None
        )
      )
    )
  }

  "CisEnrolmentHelper.extractTaxOfficeIdentifiers" should {

    "return Some when both identifiers are present" in {
      val enrolments = enrolmentsWith(Some("123"), Some("ABC456"))

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe Some(("123", "ABC456"))
    }

    "return None when enrolment is missing" in {
      val enrolments = Enrolments(Set.empty)

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe None
    }

    "return None when TaxOfficeNumber is missing" in {
      val enrolments = enrolmentsWith(None, Some("ABC456"))

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe None
    }

    "return None when TaxOfficeReference is missing" in {
      val enrolments = enrolmentsWith(Some("123"), None)

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe None
    }

    "return None when both identifiers are missing" in {
      val enrolments = enrolmentsWith(None, None)

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe None
    }

    "ignore other enrolments and pick only HMRC-CIS-ORG" in {
      val otherEnrolment = Enrolment(
        key = "OTHER-SERVICE",
        identifiers = Seq(
          EnrolmentIdentifier("TaxOfficeNumber", "999"),
          EnrolmentIdentifier("TaxOfficeReference", "XXX")
        ),
        state = "Activated",
        delegatedAuthRule = None
      )

      val cisEnrolment = Enrolment(
        key = "HMRC-CIS-ORG",
        identifiers = Seq(
          EnrolmentIdentifier("TaxOfficeNumber", "123"),
          EnrolmentIdentifier("TaxOfficeReference", "ABC456")
        ),
        state = "Activated",
        delegatedAuthRule = None
      )

      val enrolments = Enrolments(Set(otherEnrolment, cisEnrolment))

      val result =
        CisEnrolmentHelper.extractTaxOfficeIdentifiers(enrolments)

      result mustBe Some(("123", "ABC456"))
    }
  }
}
