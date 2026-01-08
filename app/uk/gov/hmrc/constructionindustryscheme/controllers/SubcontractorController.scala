package uk.gov.hmrc.constructionindustryscheme.controllers

import play.api.Logging
import play.api.libs.json.{JsError, JsValue, Json}
import play.api.mvc.{Action, ControllerComponents}
import uk.gov.hmrc.constructionindustryscheme.actions.AuthAction
import uk.gov.hmrc.constructionindustryscheme.models.requests.SubcontractorCreateRequest
import uk.gov.hmrc.constructionindustryscheme.services.SubcontractorService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubcontractorController @Inject() (
                                          authorise: AuthAction,
                                          subcontractorService: SubcontractorService,
                                          cc: ControllerComponents
                                        )(implicit ec: ExecutionContext)
  extends BackendController(cc) with Logging {

  def createSubcontractor(): Action[JsValue] =
    authorise(parse.json).async { implicit request =>
      request.body.validate[SubcontractorCreateRequest].fold(
        errs => Future.successful(BadRequest(JsError.toJson(errs))),
        scr =>
          subcontractorService
            .createSubcontractor(scr)
            .map(resp => Created(Json.toJson(resp)))
            .recover { case ex =>
              logger.error("[create] formp-proxy create failed", ex)
              BadGateway(Json.obj("message" -> "create-subcontractor-failed"))
            }
      )
    }
  }
