package com.turkcellteknoloji.iotdb.rest.shiro.filter

import org.apache.oltu.oauth2.common.message.types.TokenType
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest
import org.apache.shiro.SecurityUtils

import com.turkcellteknoloji.iotdb.security.AuthPrincipalType._

import javax.servlet.http.HttpServletRequest
import com.turkcellteknoloji.iotdb.security.shiro.OauthBearerToken

/**
 * Created by capacman on 10/31/13.
 */
trait OauthTokenFilter {

  protected def oauth()(implicit request: HttpServletRequest) {
    val acc_token = {
      if (request.getParameter("access_token") != null)
        Some(OauthBearerToken(request.getParameter("access_token")))
      else {
        val rr = new OAuthAccessResourceRequest(request, TokenType.BEARER)
        if (rr.getAccessToken == null) None else Some(OauthBearerToken(rr.getAccessToken))
      }
    }
    acc_token.map {
      token =>
        SecurityUtils.getSubject.login(token)
    }
  }
}
