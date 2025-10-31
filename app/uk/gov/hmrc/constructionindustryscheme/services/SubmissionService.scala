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

package uk.gov.hmrc.constructionindustryscheme.services

import play.api.Logging
import uk.gov.hmrc.constructionindustryscheme.connectors.{ChrisConnector, EmailConnector, FormpProxyConnector}
import uk.gov.hmrc.constructionindustryscheme.models.{BuiltSubmissionPayload, SUBMITTED, SubmissionResult, SuccessEmailParams}
import uk.gov.hmrc.constructionindustryscheme.models.requests.{CreateSubmissionRequest, NilMonthlyReturnOrgSuccessEmail, UpdateSubmissionRequest}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class SubmissionService @Inject()(
  chrisConnector: ChrisConnector,
  formpProxyConnector: FormpProxyConnector,
  emailConnector: EmailConnector
)(implicit ec: ExecutionContext) extends Logging {
  def createSubmission(request: CreateSubmissionRequest)(implicit hc: HeaderCarrier): Future[String] =
    formpProxyConnector.createSubmission(request)

  def updateSubmission(req: UpdateSubmissionRequest)(implicit hc: HeaderCarrier): Future[Unit] =
    formpProxyConnector.updateSubmission(req)

  def submitToChris(payload: BuiltSubmissionPayload,
                    successEmail: Option[SuccessEmailParams]=None)
                   (implicit hc: HeaderCarrier): Future[SubmissionResult] = {
    chrisConnector.submitEnvelope(payload.envelope, payload.correlationId).flatMap{ res =>
      res.status match {
        case SUBMITTED =>
          successEmail match {
            case Some(emailParams) =>
              logger.info(s"[email] SUBMITTED → sending success email to=${emailParams.to}")
              val ym = parseYearMonthFlexible(emailParams.monthYear)
              val month = ym.format(monthFmt)
              val year = ym.format(yearFmt)
              emailConnector
                .send(NilMonthlyReturnOrgSuccessEmail(emailParams.to, month, year))
                .map(_ => res)
                .recover {case e =>
                    logger.warn(s"[email] failed to send success email to=${emailParams.to}", e); res
                }
            case None =>
              logger.warn("[email] SUBMITTED but no email params supplied; skipping send")
              Future.successful(res)
          }
        case _ =>
          Future.successful(res)
      }
    }
  }

  private def parseYearMonthFlexible(s: String): YearMonth =
    Try(YearMonth.parse(s))
      .orElse(Try(YearMonth.parse(s.replace('/', '-'))))
      .getOrElse(throw new IllegalArgumentException(s"Invalid monthYear: $s (expected YYYY-MM or YYYY/MM)"))

  private val monthFmt = DateTimeFormatter.ofPattern("MMMM", Locale.UK)
  private val yearFmt = DateTimeFormatter.ofPattern("uuuu", Locale.UK)
}
