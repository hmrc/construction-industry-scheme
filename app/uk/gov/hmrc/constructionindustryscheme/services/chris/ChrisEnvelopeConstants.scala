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

package uk.gov.hmrc.constructionindustryscheme.services.chris

object ChrisEnvelopeConstants {
  val MessageDetailsClass = "IR-CIS-CIS300MR"
  val Qualifier    = "request"
  val Function     = "submit"
  val Transformation = "XML"
  val Organisation = "IR"
  val ChannelUri = "0126"
  val ChannelProduct = "EzGov IR-CIS-CIS300MR"
  val ChannelVersion = "3.4"
  val DefaultCurrency = "GBP"
  val Sender = "Company"
  val Namespace = "http://www.govtalk.gov.uk/taxation/CISreturn"
  val SchemaVersion = "2005-v1.1"
  val TopElementName = "CISreturn"
  val NilReturn = "yes"
}
