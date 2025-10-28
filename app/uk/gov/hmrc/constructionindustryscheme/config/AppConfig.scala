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
import play.api.Configuration

@Singleton
class AppConfig @Inject()(config: Configuration) {
  val appName: String = config.get[String]("appName")

  val chrisEnableMissingMandatory: Boolean =
    config.getOptional[Boolean]("chrisTest.enable-missing-mandatory").getOrElse(false)

  val chrisEnableNon2xx: Boolean =
    config.getOptional[Boolean]("chrisTest.enable-non-2xx").getOrElse(false)

  val chrisEnableIrmarkBad: Boolean =
    config.getOptional[Boolean]("chrisTest.enable-irmark-bad").getOrElse(false)

  val chrisNon2xxOverrideUrl: Option[String] =
    config.getOptional[String]("chrisTest.non2xx.override-url")  
}

