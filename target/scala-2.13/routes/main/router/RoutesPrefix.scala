// @GENERATOR:play-routes-compiler
// @SOURCE:/Users/jocelynsickler/Documents/ITSD-DT2025-26-Template/conf/routes
// @DATE:Thu Feb 05 12:42:51 GMT 2026


package router {
  object RoutesPrefix {
    private var _prefix: String = "/"
    def setPrefix(p: String): Unit = {
      _prefix = p
    }
    def prefix: String = _prefix
    val byNamePrefix: Function0[String] = { () => prefix }
  }
}
