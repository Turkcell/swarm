/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.turkcellteknoloji.iotdb.security.shiro

import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.authc.{SimpleAuthenticationInfo, AuthenticationException, AuthenticationToken}
import com.turkcellteknoloji.iotdb.security.{ClientID, BadTokenException, ClientIDSecretToken, OauthBearerToken}
import com.turkcellteknoloji.iotdb.security.tokens.TokenServiceComponent
import com.turkcellteknoloji.iotdb.management.ManagementServiceComponent
import scala.concurrent.Await
import scala.concurrent.duration._
import org.apache.shiro.subject.PrincipalCollection
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo

/**
 * Created by Anil Chalil on 11/14/13.
 */
trait OrganizationRealm extends AuthorizingRealm {
  this: TokenServiceComponent with ManagementServiceComponent =>

  override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {

    case bearerToken: OauthBearerToken =>
      val orgResponse = managementService.getOrganizationInfoAsync(bearerToken.principalID)
      val tokenInfo = tokenService.getTokenInfo(bearerToken)
      if (bearerToken.authPrincipalType != tokenInfo.principal.`type` || bearerToken.principalID != tokenInfo.principal.uuid)
        throw BadTokenException("token is forged")
      Await.result(orgResponse.map {
        case Some(org) => new SimpleAuthenticationInfo(org, tokenInfo, getName)
        case None => throw new AuthenticationException(s"organization not found for ${bearerToken.principalID}")
      }, 0 nanos)


    case clientIDSecret: ClientIDSecretToken =>
      val orgResponse = managementService.getOrganizationInfoAsync(clientIDSecret.principalID)
      val secret = tokenService.getClientSecret(clientIDSecret.principalID.asInstanceOf[ClientID])
      if (clientIDSecret.authPrincipalType != secret.authPrincipalType)
        throw BadTokenException("token is forged")
      if (secret != clientIDSecret.getCredentials)
        throw new AuthenticationException("authentication failed!")
      Await.result(orgResponse.map {
        case Some(org) => new SimpleAuthenticationInfo(org, secret, getName)
        case None => throw new AuthenticationException(s"organization not found for ${clientIDSecret.principalID}")
      }, 0 nanos)
  }

}
