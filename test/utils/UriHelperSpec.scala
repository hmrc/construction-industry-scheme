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

package utils

import base.SpecBase
import org.scalatest.matchers.should.Matchers
import org.scalatest.matchers.should.Matchers.shouldBe
import uk.gov.hmrc.constructionindustryscheme.utils.UriHelper.replaceHostIgnoringUserInfoAndPort

class UriHelperSpec extends SpecBase {
  "replace host and drop userInfo and port" in {
    replaceHostIgnoringUserInfoAndPort(
      "http://sa.chris.hmrc.gov.uk:9102/ChRIS/CISR/Filing/action/CISR",
      "chris.ws.ibt.hmrc.gov.uk"
    ) shouldBe
      Some("https://chris.ws.ibt.hmrc.gov.uk/ChRIS/CISR/Filing/action/CISR")
  }
  "replace host and keep path, query and fragment" in {
    replaceHostIgnoringUserInfoAndPort(
      "http://example.com/api?x=1#frag",
      "new.example.com"
    ) shouldBe
      Some("https://new.example.com/api?x=1#frag")
  }
  "replace IPv6 host and drop port" in {
    replaceHostIgnoringUserInfoAndPort(
      "http://[2001:db8::1]:8080/api",
      "example.com"
    ) shouldBe
      Some("https://example.com/api")
  }
  "return None for invalid URI" in {
    replaceHostIgnoringUserInfoAndPort(
      "not a uri",
      "example.com"
    ) shouldBe None
  }
  "return None when URI has no host" in {
    replaceHostIgnoringUserInfoAndPort(
      "mailto:user@example.com",
      "example.com"
    ) shouldBe None
  }
}
