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

package uk.gov.hmrc.constructionindustryscheme.models

sealed trait UnsubmittedMonthlyReturnStatus {
  def asText: String
}

object UnsubmittedMonthlyReturnStatus {
  case object InProgress extends UnsubmittedMonthlyReturnStatus {
    override def asText: String = "In Progress"
  }

  case object AwaitingConfirmation extends UnsubmittedMonthlyReturnStatus {
    override def asText: String = "Awaiting confirmation"
  }

  case object Failed extends UnsubmittedMonthlyReturnStatus {
    override def asText: String = "Failed"
  }

  def fromRaw(raw: Option[String]): UnsubmittedMonthlyReturnStatus =
    raw.map(_.trim.toUpperCase) match {
      case Some("STARTED") | Some("VALIDATED") => InProgress
      case Some("PENDING")                     => AwaitingConfirmation
      case Some("REJECTED")                    => Failed
      case _                                   => InProgress
    }
}
