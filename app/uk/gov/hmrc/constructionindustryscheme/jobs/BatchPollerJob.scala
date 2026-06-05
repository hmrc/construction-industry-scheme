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

import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.constructionindustryscheme.services.BatchPollerScheduledService
import uk.gov.hmrc.constructionindustryscheme.config.AppConfig

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class BatchPollerJob @Inject() (
  batchPollerScheduledService: BatchPollerScheduledService,
  appConfig: AppConfig,
  val actorSystem: ActorSystem
)(implicit val executionContext: ExecutionContext)
    extends ScheduledJob {

  override val jobName = "batch-poller-job"

  override val enabled: Boolean = appConfig.batchPollerJobEnabled

  override val description = Some(appConfig.batchPollerJobDescription)

  override val expression: String = appConfig.batchPollerJobExpression

  override val scheduledMessage = SchedulingActor.BatchPollerJobMessage(batchPollerScheduledService)

  schedule()
}
