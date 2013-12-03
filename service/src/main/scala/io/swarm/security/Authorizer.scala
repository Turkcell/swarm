package io.swarm.security

import org.apache.shiro.SecurityUtils
import org.apache.shiro.authz.AuthorizationException

/**
 * Created by Anil Chalil on 12/3/13.
 */
trait AnonymousAuthorizer {
  def isAnonymous[T](body: => T): T = {
    if (!SecurityUtils.getSubject.isAuthenticated) {
      body
    } else {
      throw new AuthorizationException("user is already authenticated!")
    }
  }
}
