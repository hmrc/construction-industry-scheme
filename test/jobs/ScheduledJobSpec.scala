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

package jobs

import base.SpecBase
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.constructionindustryscheme.jobs.{ScheduledJob, SchedulingActor}
import uk.gov.hmrc.constructionindustryscheme.services.BatchPollerScheduledService

import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

class ScheduledJobSpec extends SpecBase {

  // A valid quartz cron that will not actually fire during the test run.
  private val validCron   = "0 0/30 * ? * *"
  private val invalidCron = "not-a-cron-expression"

  private class TestScheduledJob(isEnabled: Boolean, cron: String, name: String, actorSys: ActorSystem)
      extends ScheduledJob {
    override implicit val executionContext: ExecutionContext           = ec
    override val actorSystem: ActorSystem                              = actorSys
    override val scheduledMessage: SchedulingActor.ScheduledMessage[_] =
      SchedulingActor.BatchPollerJobMessage(mock[BatchPollerScheduledService])
    override val jobName: String                                       = name
    override val enabled: Boolean                                      = isEnabled
    override val description: Option[String]                           = Some("test job")
    override val expression: String                                    = cron

    def run(): Unit = schedule()
  }

  // QuartzSchedulerExtension registers its scheduler in a JVM-global repository keyed by the
  // actor system name, so each job runs on its own uniquely-named, short-lived ActorSystem to
  // avoid colliding with other suites' "application" system. The system is terminated afterwards.
  private def withJob(isEnabled: Boolean, cron: String, name: String)(assertion: TestScheduledJob => Unit): Unit = {
    val actorSys = ActorSystem(s"scheduled-job-spec-${UUID.randomUUID()}")
    try assertion(new TestScheduledJob(isEnabled, cron, name, actorSys))
    finally Await.result(actorSys.terminate(), 10.seconds)
  }

  "ScheduledJob.schedule" - {

    "register a quartz schedule when the job is enabled and the cron expression is valid" in
      withJob(isEnabled = true, cron = validCron, name = "enabled-valid-job") { job =>
        job.run()
        // cancelJob returns true only if a job with this name was actually scheduled
        job.scheduler.cancelJob(job.jobName) mustBe true
      }

    "not register a quartz schedule when the job is disabled" in
      withJob(isEnabled = false, cron = validCron, name = "disabled-job") { job =>
        job.run()
        job.scheduler.cancelJob(job.jobName) mustBe false
      }

    "not register a quartz schedule when the cron expression is invalid" in
      withJob(isEnabled = true, cron = invalidCron, name = "invalid-expression-job") { job =>
        job.run()
        job.scheduler.cancelJob(job.jobName) mustBe false
      }
  }
}
