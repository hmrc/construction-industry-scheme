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

package uk.gov.hmrc.constructionindustryscheme.utils

import java.net.URI

object UriHelper {
  def replaceHostIgnoringUserInfoAndPort(
    uri: String,
    newHost: String
  ): Option[String] =
    try {
      val u = new URI(uri)
      for {
        scheme <- Option(u.getScheme)
        host   <- Option(u.getHost)
      } yield {
        val rebuilt = new URI(
          "https", // force scheme
          null, // drop userInfo
          newHost,
          -1, // drop port
          u.getPath,
          u.getQuery,
          u.getFragment
        )
        rebuilt.toString
      }
    } catch {
      case _: Exception => None
    }
}
