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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{timeout => mockitoTimeout, verify, when}
import uk.gov.hmrc.constructionindustryscheme.jobs.SchedulingActor
import uk.gov.hmrc.constructionindustryscheme.services.BatchPollerScheduledService

import scala.concurrent.Future

class SchedulingActorSpec extends SpecBase {

  "SchedulingActor" - {

    "invoke the service when it receives a scheduled message" in {
      val mockService = mock[BatchPollerScheduledService]
      when(mockService.invoke(any())).thenReturn(Future.unit)

      val actorRef = system.actorOf(SchedulingActor.props)
      actorRef ! SchedulingActor.BatchPollerJobMessage(mockService)

      verify(mockService, mockitoTimeout(5000)).invoke(any())
    }

    "swallow failures from the service without crashing the actor" in {
      val mockService = mock[BatchPollerScheduledService]
      when(mockService.invoke(any())).thenReturn(Future.failed(new RuntimeException("boom")))

      val actorRef = system.actorOf(SchedulingActor.props)
      actorRef ! SchedulingActor.BatchPollerJobMessage(mockService)

      verify(mockService, mockitoTimeout(5000)).invoke(any())
    }
  }
}
