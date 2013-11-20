package io.swarm.rest.shiro.session

import org.apache.shiro.session.mgt.{SessionContext, SessionKey, SessionManager}
import org.apache.shiro.session.Session
import org.apache.shiro.web.util.WebUtils
import javax.servlet.http.HttpServletRequest

/**
 * Created by capacman on 10/31/13.
 */
class HttpRequestSessionManager extends SessionManager {

  def getHost(context: SessionContext) = {
    val host = context.getHost
    if (host == null) {
      val request = WebUtils.getRequest(context)
      if (request != null) {
        request.getRemoteHost
      } else host
    } else host

  }

  def createSession(request: HttpServletRequest, host: String) = new HttpServletRequestSession(request, host)

  def start(context: SessionContext): Session = {
    if (!WebUtils.isHttp(context)) throw new IllegalArgumentException("SessionContext must be an HTTP compatible implementation.")

    val request = WebUtils.getHttpRequest(context)

    val session = createSession(request, getHost(context))

    request.setAttribute(HttpRequestSessionManager.REQUEST_ATTRIBUTE_KEY, session)
    session
  }

  def getSession(key: SessionKey): Session = {
    if (!WebUtils.isHttp(key)) throw new IllegalArgumentException("SessionKey must be an HTTP compatible implementation.")

    val request = WebUtils.getHttpRequest(key)

    request.getAttribute(HttpRequestSessionManager.REQUEST_ATTRIBUTE_KEY).asInstanceOf[Session]
  }
}

object HttpRequestSessionManager {
  val REQUEST_ATTRIBUTE_KEY = "__SHIRO_REQUEST_SESSION"
}
