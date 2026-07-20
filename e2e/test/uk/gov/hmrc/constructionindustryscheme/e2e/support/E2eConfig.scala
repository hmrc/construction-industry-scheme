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

package uk.gov.hmrc.constructionindustryscheme.e2e.support

/** Host configuration for the e2e suite, same env var names and defaults as the bash scripts in it/test/scripts.
  * sys.props is checked as a fallback so hosts can also be set via -D flags on the sbt command line.
  */
object E2eConfig {

  private def env(name: String, default: String): String =
    sys.env.get(name).orElse(sys.props.get(name)).getOrElse(default)

  val backendHost: String   = env("BACKEND_HOST", "http://localhost:6994")
  val stubHost: String      = env("STUB_HOST", "http://localhost:6997")
  val authHost: String      = env("AUTH_HOST", "http://localhost:8500")
  val authLoginHost: String = env("AUTH_LOGIN_API_HOST", "http://localhost:8585")

  /** When true, missing services fail the suites instead of cancelling them */
  val strict: Boolean = env("E2E_STRICT", "false").equalsIgnoreCase("true")

  // app.Routes is mounted under /cis in prod.routes
  val routePrefix: String = "/cis"
}
