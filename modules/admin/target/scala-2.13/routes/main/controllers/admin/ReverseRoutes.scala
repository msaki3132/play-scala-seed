// @GENERATOR:play-routes-compiler
// @SOURCE:modules/admin/conf/admin.routes

import play.api.mvc.Call


import _root_.controllers.Assets.Asset

// @LINE:10
package controllers.admin {

  // @LINE:10
  class ReverseAdminController(_prefix: => String) {
    def _defaultPrefix: String = {
      if (_prefix.endsWith("/")) "" else "/"
    }

  
    // @LINE:10
    def index(): Call = {
      
      Call("GET", _prefix + { _defaultPrefix } + "index")
    }
  
  }


}
