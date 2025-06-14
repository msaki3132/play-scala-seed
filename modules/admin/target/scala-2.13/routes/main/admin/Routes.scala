// @GENERATOR:play-routes-compiler
// @SOURCE:modules/admin/conf/admin.routes

package admin

import play.core.routing._
import play.core.routing.HandlerInvokerFactory._

import play.api.mvc._

import _root_.controllers.Assets.Asset

class Routes(
  override val errorHandler: play.api.http.HttpErrorHandler, 
  // @LINE:10
  AdminController_0: controllers.admin.AdminController,
  val prefix: String
) extends GeneratedRouter {

  @javax.inject.Inject()
  def this(errorHandler: play.api.http.HttpErrorHandler,
    // @LINE:10
    AdminController_0: controllers.admin.AdminController
  ) = this(errorHandler, AdminController_0, "/")

  def withPrefix(addPrefix: String): Routes = {
    val prefix = play.api.routing.Router.concatPrefix(addPrefix, this.prefix)
    admin.RoutesPrefix.setPrefix(prefix)
    new Routes(errorHandler, AdminController_0, prefix)
  }

  private val defaultPrefix: String = {
    if (this.prefix.endsWith("/")) "" else "/"
  }

  def documentation = List(
    ("""GET""", this.prefix + (if(this.prefix.endsWith("/")) "" else "/") + """index""", """controllers.admin.AdminController.index()"""),
    Nil
  ).foldLeft(Seq.empty[(String, String, String)]) { (s,e) => e.asInstanceOf[Any] match {
    case r @ (_,_,_) => s :+ r.asInstanceOf[(String, String, String)]
    case l => s ++ l.asInstanceOf[List[(String, String, String)]]
  }}


  // @LINE:10
  private lazy val controllers_admin_AdminController_index0_route = Route("GET",
    PathPattern(List(StaticPart(this.prefix), StaticPart(this.defaultPrefix), StaticPart("index")))
  )
  private lazy val controllers_admin_AdminController_index0_invoker = createInvoker(
    AdminController_0.index(),
    play.api.routing.HandlerDef(this.getClass.getClassLoader,
      "admin",
      "controllers.admin.AdminController",
      "index",
      Nil,
      "GET",
      this.prefix + """index""",
      """##
  summary: admin index
  tags:
    - admin
  responses:
    200:
      description: success
      schema:
##""",
      Seq()
    )
  )


  def routes: PartialFunction[RequestHeader, Handler] = {
  
    // @LINE:10
    case controllers_admin_AdminController_index0_route(params@_) =>
      call { 
        controllers_admin_AdminController_index0_invoker.call(AdminController_0.index())
      }
  }
}
