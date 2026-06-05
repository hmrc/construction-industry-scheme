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

package uk.gov.hmrc.constructionindustryscheme.jobs

import play.api.Logging
import org.apache.pekko.actor.{Actor, Props}
import uk.gov.hmrc.constructionindustryscheme.services.BatchPollerScheduledService

import scala.concurrent.ExecutionContext

class SchedulingActor(implicit ec: ExecutionContext) extends Actor with Logging {

  override def receive: Receive = { case message: SchedulingActor.ScheduledMessage[_] =>
    message.service.invoke.onComplete {
      case scala.util.Success(_)  =>
        logger.info(s"Successfully executed ${message.service}")
      case scala.util.Failure(ex) =>
        logger.error(s"Failed to execute ${message.service}", ex)
    }
  }
}

object SchedulingActor {

  def props(implicit ec: ExecutionContext): Props =
    Props(new SchedulingActor())

  sealed trait ScheduledMessage[A] {
    val service: ScheduledService[A]
  }

  case class BatchPollerJobMessage(
    service: BatchPollerScheduledService
  ) extends ScheduledMessage[Unit]
}
