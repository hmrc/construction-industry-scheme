package uk.gov.hmrc.constructionindustryscheme.repositories

import base.SpecBase
import play.api.libs.json.Json

import java.time.Instant

class JourneyHandoffDataSpec extends SpecBase {

  "JourneyHandoffData" - {

    "must serialise and deserialise" in {
      val instant = Instant.parse("2026-05-06T15:49:35.000Z")

      val model = JourneyHandoffData(
        id = "handoff-123",
        userId = "cred-123",
        journeyType = "amend-monthly-return",
        data = Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        ),
        lastUpdated = instant
      )

      val json = Json.toJson(model)

      json mustBe Json.obj(
        "id"          -> "handoff-123",
        "userId"      -> "cred-123",
        "journeyType" -> "amend-monthly-return",
        "data" -> Json.obj(
          "instanceId" -> "1",
          "taxYear"    -> 2026,
          "taxMonth"   -> 4
        ),
        "lastUpdated" -> Json.obj(
          "$date" -> Json.obj(
            "$numberLong" -> instant.toEpochMilli.toString
          )
        )
      )

      json.as[JourneyHandoffData] mustBe model
    }
  }

  "JourneyHandoffDataKeys" - {

    "must contain expected field names" in {
      JourneyHandoffDataKeys.idField mustBe "id"
      JourneyHandoffDataKeys.userIdField mustBe "userId"
      JourneyHandoffDataKeys.journeyTypeField mustBe "journeyType"
      JourneyHandoffDataKeys.dataField mustBe "data"
      JourneyHandoffDataKeys.lastUpdatedField mustBe "lastUpdated"
    }
  }
}
