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

import org.apache.shiro.realm.{AuthorizingRealm, Realm}
import org.apache.shiro.authc.{AuthenticationToken, AuthenticationInfo, SimpleAuthenticationInfo, AuthenticationException}
import com.turkcellteknoloji.iotdb.security._
import scala.concurrent.duration._
import org.apache.shiro.authc.credential.CredentialsMatcher
import com.turkcellteknoloji.iotdb.domain._
import com.turkcellteknoloji.iotdb.Config
import scala.concurrent._
import scala.Some

/**
 * Created by Anil Chalil on 11/14/13.
 */
class ClientIDSecretBearerCredentialsMatcher extends CredentialsMatcher {
  def doCredentialsMatch(token: AuthenticationToken, info: AuthenticationInfo): Boolean = token match {
    case t: OauthBearerToken => true
    case t: ClientIDSecretToken => t.getCredentials == info.getCredentials
  }
}

trait BearerRealmBase {
  this: TokenRepositoryComponent with Realm =>

  protected def authorizeBearer[T <: IDEntity](idEntity: Future[Option[T]], bearerToken: OauthBearerToken, check: T => Unit = null) = {
    //TODO check tokenInfo since it can return none(means somebody know salt value or cassandra has problem)
    val tokenInfo = tokenRepository.getTokenInfo(bearerToken)
    if (bearerToken.authPrincipalType != tokenInfo.principal.`type` || bearerToken.principalID != tokenInfo.principal.uuid)
      throw BadTokenException("token is forged")
    Await.result(idEntity.map {
      case Some(entity) =>
        if (check != null)
          check(entity)
        new SimpleAuthenticationInfo(entity, tokenInfo, getName)
      case None => throw new AuthenticationException(s"${idEntity.getClass().getName} not found for ${bearerToken.principalID}")
    }, 0 nanos)
  }
}

trait ClientIDSecretRealmBase {
  this: TokenRepositoryComponent with Realm =>

  protected def authorizeClientIDSecret(idEntity: Future[Option[IDEntity]], clientIDSecret: ClientIDSecretToken) = {
    val secret = tokenRepository.getClientSecret(clientIDSecret.principalID.asInstanceOf[ClientID])
    if (clientIDSecret.authPrincipalType != secret.authPrincipalType)
      throw BadTokenException("token is forged")
    Await.result(idEntity.map {
      case Some(entity) => new SimpleAuthenticationInfo(entity, secret, getName)
      case None => throw new AuthenticationException(s"${idEntity.getClass().getName} not found for ${clientIDSecret.principalID}")
    }, 0 nanos)
  }
}

trait ClientIDSecretBearerRealmBase extends AuthorizingRealm with ClientIDSecretRealmBase with BearerRealmBase {
  this: ClientRepositoryComponent with ResourceRepositoryComponent with TokenRepositoryComponent =>


  override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {

    case bearerToken: OauthBearerToken =>
      bearerToken.authPrincipalType match {
        case AuthPrincipalType.Device => authorizeBearer(clientRepository.getDeviceAsync(bearerToken.principalID), bearerToken)
        case AuthPrincipalType.Database => authorizeBearer(resourceRepository.getDatabaseInfoAsync(bearerToken.principalID), bearerToken)
        case AuthPrincipalType.Organization => authorizeBearer(resourceRepository.getOrganizationInfoAsync(bearerToken.principalID), bearerToken)
      }


    case clientIDSecret: ClientIDSecretToken =>
      clientIDSecret.authPrincipalType match {
        case AuthPrincipalType.Device => authorizeClientIDSecret(clientRepository.getDeviceAsync(clientIDSecret.principalID), clientIDSecret)
        case AuthPrincipalType.Database => authorizeClientIDSecret(resourceRepository.getDatabaseInfoAsync(clientIDSecret.principalID), clientIDSecret)
        case AuthPrincipalType.Organization => authorizeClientIDSecret(resourceRepository.getOrganizationInfoAsync(clientIDSecret.principalID), clientIDSecret)
      }
  }
}

trait OrganizationRealmBase extends ClientIDSecretBearerRealmBase {
  this: ClientRepositoryComponent with ResourceRepositoryComponent with TokenRepositoryComponent =>
  override def supports(token: AuthenticationToken) = token match {
    case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Organization
    case _ => false
  }
}

trait DatabaseRealmBase extends ClientIDSecretBearerRealmBase {
  this: ClientRepositoryComponent with ResourceRepositoryComponent with TokenRepositoryComponent =>
  override def supports(token: AuthenticationToken) = token match {
    case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Database
    case _ => false
  }
}

trait DeviceRealmBase extends ClientIDSecretBearerRealmBase {
  this: ClientRepositoryComponent with ResourceRepositoryComponent with TokenRepositoryComponent =>
  override def supports(token: AuthenticationToken) = token match {
    case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Device
    case _ => false
  }
}

trait UserInfoRealmBase extends AuthorizingRealm with BearerRealmBase {
  this: TokenRepositoryComponent with ClientRepositoryComponent =>

  def getPrincipal(principal: String): Future[Option[UserInfo]]

  def checkClient(client: Client) {
    if (!client.activated)
      throw new AuthenticationException("client is not activated")
    if (client.disabled)
      throw new AuthenticationException("client is disabled")
  }

  override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {
    case bearerToken: OauthBearerToken =>
      bearerToken.authPrincipalType match {
        case AuthPrincipalType.Admin => authorizeBearer(clientRepository.getAdminUserAsync(bearerToken.principalID), bearerToken, checkClient)
        case AuthPrincipalType.DatabaseUser => authorizeBearer(clientRepository.getDatabaseUserAsync(bearerToken.principalID), bearerToken, checkClient)
      }

    case usernamePasswordToken: UsernamePasswordToken =>
      val userInfoF = getPrincipal(usernamePasswordToken.getPrincipal)
      Await.result(userInfoF.map {
        case Some(userInfo) =>
          checkClient(userInfo)
          new SimpleAuthenticationInfo(if (userInfo.username == usernamePasswordToken.getPrincipal) userInfo.username else userInfo.email, userInfo.credential, Config.userInfoHash, getName)
        case None => throw new AuthenticationException(s"userinfo not found for ${usernamePasswordToken.getPrincipal}")
      }, 0 nanos)
  }
}

trait AdminUserRealm extends UserInfoRealmBase {
  this: TokenRepositoryComponent with ClientRepositoryComponent =>
  override def supports(token: AuthenticationToken) = token match {
    case t: UsernamePasswordToken => t.principalType == AuthPrincipalType.Admin
    case _ => false
  }
}