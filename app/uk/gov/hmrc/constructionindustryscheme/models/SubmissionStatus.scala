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

package uk.gov.hmrc.constructionindustryscheme.models

sealed trait SubmissionStatus
case object ACCEPTED extends SubmissionStatus with WithName("ACCEPTED")
case object FATAL_ERROR extends SubmissionStatus with WithName("FATAL_ERROR")
case object SUBMITTED extends SubmissionStatus with WithName("SUBMITTED")
case object SUBMITTED_NO_RECEIPT extends SubmissionStatus with WithName("SUBMITTED_NO_RECEIPT")
case object DEPARTMENTAL_ERROR extends SubmissionStatus with WithName("DEPARTMENTAL_ERROR")

final case class ResponseEndPoint(url: String, pollIntervalSeconds: Int)

final case class GovTalkError(
  errorNumber: String,
  errorType: String,
  errorText: String 
)

final case class GovTalkMeta(
  qualifier: String,
  function: String,
  className: String,
  correlationId: String,
  gatewayTimestamp: Option[String],
  responseEndPoint: ResponseEndPoint,
  error: Option[GovTalkError]
)

final case class SubmissionResult(
  status: SubmissionStatus,
  rawXml: String,
  meta: GovTalkMeta
)