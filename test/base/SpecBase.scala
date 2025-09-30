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

package base

import actions.FakeAuthAction
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContentAsEmpty, ControllerComponents, PlayBodyParsers}
import play.api.test.{DefaultAwaitTimeout, FakeRequest}
import play.api.test.Helpers.stubControllerComponents
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.CisTaxpayer
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with DefaultAwaitTimeout
    with ScalaFutures
    with FakeApplicationFactory
    with BaseOneAppPerSuite
    with MockitoSugar 
    with BeforeAndAfterEach{

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host" -> "localhost",
        "microservice.services.auth.port" -> 11111,
        "microservice.services.rds-datacache-proxy.host" -> "localhost",
        "microservice.services.rds-datacache-proxy.port" -> 11111
      )
      .build()

  val cc: ControllerComponents = stubControllerComponents()
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  val bodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  implicit val hc: HeaderCarrier = HeaderCarrier()
  implicit val ec: ExecutionContext = cc.executionContext

  def fakeAuthAction(ton: String = "123", tor: String = "AB456"): AuthAction =
    FakeAuthAction.withCisIdentifiers(ton, tor, bodyParsers)

  def noEnrolmentReferenceAuthAction: AuthAction =
    FakeAuthAction.empty(bodyParsers)

  def mkTaxpayer(
    id: String = "CIS-123",
    ton: String = "123",
    tor: String = "AB456",
    employerName1: Option[String] = Some("TEST LTD")
                ): CisTaxpayer =
    CisTaxpayer(
      uniqueId         = id,
      taxOfficeNumber  = ton,
      taxOfficeRef     = tor,
      aoDistrict       = None,
      aoPayType        = None,
      aoCheckCode      = None,
      aoReference      = None,
      validBusinessAddr= None,
      correlation      = None,
      ggAgentId        = None,
      employerName1    = employerName1,
      employerName2    = None,
      agentOwnRef      = None,
      schemeName       = None,
      utr              = None,
      enrolledSig      = None
    )
}