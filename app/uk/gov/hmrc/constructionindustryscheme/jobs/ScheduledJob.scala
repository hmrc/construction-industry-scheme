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
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.extension.quartz.QuartzSchedulerExtension
import org.quartz.CronExpression

import scala.concurrent.ExecutionContext

trait ScheduledJob extends Logging {

  implicit val executionContext: ExecutionContext
  val scheduledMessage: SchedulingActor.ScheduledMessage[_]
  val actorSystem: ActorSystem
  val jobName: String
  val enabled: Boolean
  val description: Option[String]
  val expression: String

  lazy val scheduler = QuartzSchedulerExtension(actorSystem)

  private lazy val schedulingActorRef = actorSystem.actorOf(SchedulingActor.props)

  private lazy val expressionValid: Boolean = CronExpression.isValidExpression(expression)

  protected def schedule(): Unit =
    if (enabled && expressionValid) {
      scheduler.createSchedule(jobName, description, expression)
      scheduler.schedule(jobName, schedulingActorRef, scheduledMessage)
      logger.info(s"Started scheduler: $jobName")
    } else {
      logger.warn(s"Scheduler not started: $jobName")
    }
}
