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

package uk.gov.hmrc.constructionindustryscheme.itutil

import scala.io.Source

object ItResources {
  def read(path: String): String = {
    val stream = getClass.getClassLoader.getResourceAsStream(path)
    require(stream != null, s"Resource not found on classpath: $path")
    val src    = Source.fromInputStream(stream, "UTF-8")
    try src.mkString
    finally src.close()
  }
}
