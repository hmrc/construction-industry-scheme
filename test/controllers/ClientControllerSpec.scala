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

package controllers

import base.SpecBase
import play.api.test.Helpers.contentAsJson
import play.api.test.Helpers.{contentAsJson, status}
import scala.concurrent.Future
import play.api.mvc.Result
import play.api.http.Status.OK
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.controllers.ClientController


class ClientControllerSpec extends SpecBase {

  "ClientController" - {

    "GET /cis/get-client-list-status" - {

      "return 200 with status Success" in {
        val auth: AuthAction = fakeAuthActionAgent()
        val controller = new ClientController(auth, cc)
        val result: Future[Result] = controller.getClientListDownloadStatus(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "status").as[String] must equal("Success")
      }

      "return 200 with status Failed" in {
        val auth: AuthAction = fakeAuthActionAgent("Failed")
        val controller = new ClientController(auth, cc)
        val result: Future[Result] = controller.getClientListDownloadStatus(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "status").as[String] must equal("Failed")
      }

      "return 200 with status InitiateDownload" in {
        val auth: AuthAction = fakeAuthActionAgent("InitiateDownload")
        val controller = new ClientController(auth, cc)
        val result: Future[Result] = controller.getClientListDownloadStatus(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "status").as[String] must equal("InitiateDownload")
      }

      "return 200 with status InProgress" in {
        val auth: AuthAction = fakeAuthActionAgent("InProgress")
        val controller = new ClientController(auth, cc)
        val result: Future[Result] = controller.getClientListDownloadStatus(fakeRequest)

        status(result) mustBe OK
        (contentAsJson(result) \ "status").as[String] must equal("InProgress")
      }
    }
  }
}
