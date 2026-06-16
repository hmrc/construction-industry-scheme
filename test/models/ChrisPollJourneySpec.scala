package models

import base.SpecBase
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney
import uk.gov.hmrc.constructionindustryscheme.models.ChrisPollJourney.*

class ChrisPollJourneySpec extends SpecBase {

  "ChrisPollJourney" - {

    "must define monthly return values" in {
      MonthlyReturn.logName mustBe "monthlyReturn"
      MonthlyReturn.govTalkClass mustBe "IR-CIS-CIS300MR"
    }

    "must define verification values" in {
      Verification.logName mustBe "verification"
      Verification.govTalkClass mustBe "IR-CIS-VERIFY"
    }

    "must be usable as ChrisPollJourney" in {
      val journey: ChrisPollJourney = Verification

      journey.logName mustBe "verification"
      journey.govTalkClass mustBe "IR-CIS-VERIFY"
    }
  }
}
