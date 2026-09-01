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

import uk.gov.hmrc.constructionindustryscheme.connectors.FormpProxyConnector
import uk.gov.hmrc.constructionindustryscheme.models.response.ContractorValidationResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ContractorValidationService @Inject() (
  formpProxyConnector: FormpProxyConnector
)(implicit ec: ExecutionContext) {

  private val schemeNameRegex =
    """[A-Za-z0-9"~!@#$%*+:;=?\s,.\[\]_{}()/&'\-^\\£€]*""".r

  private val emailRegex =
    """^[A-Za-z0-9!#$%&*+,\-./=?^_`{|}~]+@[A-Za-z0-9!#$%&*+,\-./=?^_`{|}~]+$""".r

  def validateContractorDetails(
    instanceId: String
  )(implicit hc: HeaderCarrier): Future[Option[ContractorValidationResponse]] =
    formpProxyConnector.getContractorScheme(instanceId).map {
      case None         => None
      case Some(scheme) =>
        Some(ContractorValidationResponse(
          utrValid          = validateUtr(scheme.utr),
          schemeNameValid   = validateSchemeName(scheme.name),
          emailAddressValid = validateEmail(scheme.emailAddress),
          scheme            = scheme
        ))
    }

  private def validateUtr(utr: Option[String]): Boolean =
    utr match {
      case None        => false
      case Some(value) =>
        value.matches("[0-9]+") &&
          value.length == 10 &&
          passesUtrAlgorithm(value)
    }

  private def passesUtrAlgorithm(utr: String): Boolean = {
    val digits  = utr.map(_.asDigit)
    val weights = Seq(6, 7, 8, 9, 10, 5, 4, 3, 2)
    val total   = digits.tail.zip(weights).foldLeft(0) { case (acc, (d, w)) => acc + d * w }
    val lookup  = Seq(2, 1, 9, 8, 7, 6, 5, 4, 3, 2, 1)
    lookup(total % 11) == digits.head
  }

  private def validateSchemeName(name: Option[String]): Boolean =
    name match {
      case None        => true
      case Some(value) => value.length <= 56 && schemeNameRegex.matches(value)
    }

  private def validateEmail(email: Option[String]): Boolean =
    email match {
      case None        => true
      case Some(value) => value.length <= 256 && emailRegex.matches(value)
    }
}
