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

package uk.gov.hmrc.constructionindustryscheme.config

import javax.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import uk.gov.hmrc.constructionindustryscheme.utils.SchemaLoader

import javax.xml.validation.Schema

@Singleton
class AppConfig @Inject() (val config: Configuration, val environment: Environment) {
  val appName: String = config.get[String]("appName")

  val chrisHost: Seq[String] = config.get[Seq[String]]("submissionPollUrlKnownHosts")

  val cisServiceName: String = config.get[String]("cis.serviceName")

  val cisGracePeriodSeconds: Int = config.get[Int]("cis.gracePeriodSeconds")

  val cisDefaultBrowserIntervalMs: Long = config.get[Long]("cis.defaultBrowserIntervalMs")

  val cisDefaultBusinessIntervalsMs: List[Long] = config.get[Seq[Long]]("cis.defaultBusinessIntervalsMs").toList

  lazy val useOverridePollResponseEndPoint: Boolean =
    config.getOptional[Boolean]("cis.useOverridePollResponseEndPoint").getOrElse(false)

  lazy val overridePollResponseEndPoint: String =
    config.get[String]("cis.overridePollResponseEndPoint")

  private val schemaNames: Seq[String] =
    config.get[Seq[String]]("xsd.schemaNames")

  lazy val schema: Schema = SchemaLoader.loadSchemas(schemaNames, environment)

  lazy val cacheTtl: Long          = config.get[Int]("mongodb.timeToLiveInSeconds")
  val agentClientCryptoKey: String = config.get[String]("agentClientCrypto.key")
  val cryptoToggle: Boolean        = config.get[Boolean]("encryptionToggle")

}
