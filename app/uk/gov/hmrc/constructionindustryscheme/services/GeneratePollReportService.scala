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

package uk.gov.hmrc.constructionindustryscheme.services

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class GeneratePollReportService @Inject() ()(implicit ec: ExecutionContext) extends Logging {

  def generatePollReport()(implicit hc: HeaderCarrier): Future[Unit] = {
    logger.info("[GeneratePollReportService][generatePollReport] Calling F8 - Generate Poll Report")

    /*
     * TODO:
     * Implement F8 - Generate Poll Report here.
     *
     * This should be called only when both:
     * - verification submissions list is empty
     * - monthly return submissions list is empty
     */

    Future.unit
  }
}
