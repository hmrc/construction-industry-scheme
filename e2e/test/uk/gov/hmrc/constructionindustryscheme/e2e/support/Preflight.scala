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

import java.time.Duration
import scala.util.Try

/** Pings the required local services once per test JVM. */
object Preflight {

  private val services: Seq[(String, String)] = Seq(
    "backend"        -> s"${E2eConfig.backendHost}/ping/ping",
    "stub"           -> s"${E2eConfig.stubHost}/ping/ping",
    "auth"           -> s"${E2eConfig.authHost}/ping/ping",
    "auth-login-api" -> s"${E2eConfig.authLoginHost}/ping/ping"
  )

  /** None when all services answer /ping/ping with 200; otherwise a message listing what is unreachable and how to
    * start it. Lazy so the checks run once per (single, sequential) test JVM.
    */
  lazy val failure: Option[String] = {
    val unreachable = services.flatMap { case (name, url) =>
      // 60s like the bash preflight (curl -m 60): a fresh `sbt run` binds the
      // port immediately but compiles the app on the first request
      Try(HttpSupport.get(url, timeout = Duration.ofSeconds(60)).status).toOption match {
        case Some(200)  => None
        case Some(code) => Some(s"$name answered HTTP $code at $url")
        case None       => Some(s"$name not reachable at $url")
      }
    }
    if (unreachable.isEmpty) None
    else
      Some(
        (unreachable.map(u => s"  - $u") ++ Seq(
          "",
          "Start the missing services first, e.g.:",
          "  sm2 --start CIS_ALL",
          "  sm2 --stop CONSTRUCTION_INDUSTRY_SCHEME     # then run this branch: sbt run",
          "  (cd ../construction-industry-scheme-external-stub && sbt run)   # stub on 6997"
        )).mkString("e2e preflight failed:\n", "\n", "")
      )
  }
}
