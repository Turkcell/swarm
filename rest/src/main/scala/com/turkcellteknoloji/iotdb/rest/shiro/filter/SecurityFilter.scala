package com.turkcellteknoloji.iotdb.server.shiro.filter

import javax.servlet.http.HttpServletRequest
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest
import org.apache.oltu.oauth2.common.message.types.TokenType
import com.turkcellteknoloji.iotdb.security.tokens.TokenService
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType._
import com.turkcellteknoloji.iotdb.management.ManagementService
import com.turkcellteknoloji.iotdb.security.shiro.PrincipalCredentialsToken
import scala.util.Try
import org.apache.shiro.SecurityUtils


/**
 * Created by capacman on 10/31/13.
 */
trait OauthTokenFilter {

  implicit val tokenService: TokenService
  implicit val managementService: ManagementService

  protected def oauth()(implicit request: HttpServletRequest) {
    val acc_token = {
      if (request.getParameter("access_token") != null)
        Some(request.getParameter("access_token"))
      else {
        val rr = new OAuthAccessResourceRequest(request, TokenType.BEARER)
        if (rr.getAccessToken == null) None else Some(rr.getAccessToken)
      }
    }
    acc_token.map {
      token =>
        val tokenInfo = tokenService.getTokenInfo(token)
        val pct = tokenInfo.principal.`type` match {
          case Admin | DatabaseUser => PrincipalCredentialsToken(managementService.userInfoFromAccessToken(token, tokenInfo.principal.`type`), token, tokenInfo.principal.`type`)
          case Organization => PrincipalCredentialsToken(managementService.organizationInfoFromAccessToken(token), token)
          case Database => PrincipalCredentialsToken(managementService.databaseInfoFromAccessToken(token), token)
          case Device => PrincipalCredentialsToken(managementService.deviceFromAccessToken(token), token)
        }
        SecurityUtils.getSubject.login(pct)
    }
  }
}
