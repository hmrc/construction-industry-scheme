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

package uk.gov.hmrc.constructionindustryscheme.models.requests

import scala.xml.Elem

case class ChrisPollRequest(correlationId: String) {

  def paylaod: Elem = <GovTalkMessage xmlns="http://www.govtalk.gov.uk/CM/envelope">
    <EnvelopeVersion>2.0</EnvelopeVersion>
    <Header>
      <MessageDetails>
        <Class>IR-CIS-CIS300MR</Class>
        <Qualifier>poll</Qualifier>
        <Function>submit</Function>
        <CorrelationID>{correlationId}</CorrelationID>
        <Transformation>XML</Transformation>
      </MessageDetails>
      <SenderDetails/>
    </Header>
    <GovTalkDetails>
      <Keys/>
    </GovTalkDetails>
  </GovTalkMessage>
}
