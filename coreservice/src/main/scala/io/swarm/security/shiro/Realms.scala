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

package io.swarm.security.shiro

import org.apache.shiro.realm.{AuthorizingRealm, Realm}
import org.apache.shiro.authc._
import io.swarm.security._
import scala.concurrent.duration._
import org.apache.shiro.authc.credential.{HashedCredentialsMatcher, CredentialsMatcher}
import io.swarm.domain._
import io.swarm.Config
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.authz.{SimpleAuthorizationInfo, AuthorizationInfo}
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters.asJavaCollectionConverter
import io.swarm.management.Management._
import io.swarm.management.Management.Organization
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

class UsernamePasswordBearerCredentialsMatcher(val usernamePasswordMatcher: CredentialsMatcher) extends CredentialsMatcher {
  def doCredentialsMatch(token: AuthenticationToken, info: AuthenticationInfo): Boolean = token match {
    case t: OauthBearerToken => true
    case t: UsernamePasswordToken => usernamePasswordMatcher.doCredentialsMatch(token, info)
  }
}

trait BearerRealmBaseComponent {
  this: TokenRepositoryComponent =>

  trait BearerRealmBase extends Realm {
    protected def authorizeBearer[T <: IDEntity](idEntity: Future[Option[T]], bearerToken: OauthBearerToken, check: T => Unit = null) = {
      //TODO check tokenInfo since it can return none(means somebody know salt value or cassandra has problem)
      val tokenInfo = tokenRepository.getTokenInfo(bearerToken)
      if (bearerToken.authPrincipalType != tokenInfo.principal.`type` || bearerToken.principalID != tokenInfo.principal.uuid || bearerToken.expires != tokenInfo.expiration)
        throw BadTokenException("token is forged")
      Await.result(idEntity.map {
        case Some(entity) =>
          if (check != null)
            check(entity)
          new SimpleAuthenticationInfo(entity, tokenInfo, getName)
        case None => throw new UnknownAccountException(s"${idEntity.getClass().getName} not found for ${bearerToken.principalID}")
      }, Duration.Inf)
    }
  }

}

trait ClientIDSecretRealmBaseComponent {
  this: TokenRepositoryComponent =>

  trait ClientIDSecretRealmBase extends Realm {
    protected def authorizeClientIDSecret[T <: IDEntity](idEntity: Future[Option[T]], clientIDSecret: ClientIDSecretToken, check: T => Unit = null) = {
      val secret = tokenRepository.getClientSecret(clientIDSecret.getPrincipal)
      if (clientIDSecret.authPrincipalType != secret.authPrincipalType)
        throw BadTokenException("token is forged")
      Await.result(idEntity.map {
        case Some(entity) =>
          if (check != null)
            check(entity)
          new SimpleAuthenticationInfo(entity, secret, getName)
        case None => throw new UnknownAccountException(s"${idEntity.getClass().getName} not found for ${clientIDSecret.principalID}")
      }, Duration.Inf)
    }
  }

}

trait ClientIDSecretBearerRealmBaseComponent extends ClientIDSecretRealmBaseComponent with BearerRealmBaseComponent with SessionProvider {
  this: TokenRepositoryComponent with OrganizationRepositoryComponent =>

  trait ClientIDSecretBearerRealmBase extends AuthorizingRealm with BearerRealmBase with ClientIDSecretRealmBase {

    override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {

      case bearerToken: OauthBearerToken =>
        bearerToken.authPrincipalType match {
          case AuthPrincipalType.Device => authorizeBearer(future(withSession(implicit s => organizationRepository.getDevice(bearerToken.principalID))), bearerToken)
          case AuthPrincipalType.Domain => authorizeBearer(future(withSession(implicit s => organizationRepository.getDomain(bearerToken.principalID))), bearerToken)
          case AuthPrincipalType.Organization => authorizeBearer(future(withSession(implicit s => organizationRepository.getOrganization(bearerToken.principalID))), bearerToken)
        }

      case clientIDSecret: ClientIDSecretToken =>
        clientIDSecret.authPrincipalType match {
          case AuthPrincipalType.Device => authorizeClientIDSecret(future(withSession(implicit s => organizationRepository.getDevice(clientIDSecret.principalID))), clientIDSecret)
          case AuthPrincipalType.Domain => authorizeClientIDSecret(future(withSession(implicit s => organizationRepository.getDomain(clientIDSecret.principalID))), clientIDSecret)
          case AuthPrincipalType.Organization => authorizeClientIDSecret(future(withSession(implicit s => organizationRepository.getOrganization(clientIDSecret.principalID))), clientIDSecret)
        }
    }
  }

}

trait OrganizationRealmComponent extends ClientIDSecretBearerRealmBaseComponent {
  this: TokenRepositoryComponent with OrganizationRepositoryComponent =>

  object OrganizationRealm extends ClientIDSecretBearerRealmBase {
    //initialization block
    {
      setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
    }


    override def supports(token: AuthenticationToken) = token match {
      case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Organization
      case _ => false
    }

    override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
      val info = new SimpleAuthorizationInfo()
      val org = principals.byType(classOf[Organization]).asScala.head
      info.addObjectPermission(Permissions(org.organizationRef))
      info.addObjectPermission(Permissions.forDomains(org.domains))
      info.addRoles(List(Roles.DomainAdmin, Roles.OrganizationAdmin).asJavaCollection)
      info
    }
  }

}

trait DomainRealmComponent extends ClientIDSecretBearerRealmBaseComponent {
  this: TokenRepositoryComponent with OrganizationRepositoryComponent =>

  object DatabaseRealm extends ClientIDSecretBearerRealmBase {
    //initialization block
    {
      setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
    }

    override def supports(token: AuthenticationToken) = token match {
      case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Domain
      case _ => false
    }

    override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
      val info = new SimpleAuthorizationInfo()
      val domain = principals.byType(classOf[Domain]).asScala.head
      info.addObjectPermission(Permissions.forDomains(domain.domainRef))
      info.addRoles(List(Roles.DomainAdmin).asJavaCollection)
      info
    }
  }

}

trait DeviceRealmComponent extends ClientIDSecretRealmBaseComponent with BearerRealmBaseComponent with SessionProvider {
  this: TokenRepositoryComponent with OrganizationRepositoryComponent =>

  object DeviceRealm extends AuthorizingRealm with BearerRealmBase with ClientIDSecretRealmBase {
    //initialization block
    {
      setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
    }

    def checkClient(device: Device) {
      if (!device.activated)
        throw new LockedAccountException("client is not activated")
      if (device.disabled)
        throw new DisabledAccountException("client is disabled")
    }

    override def supports(token: AuthenticationToken) = token match {
      case t: PrincipalAuthenticationToken => t.authPrincipalType == AuthPrincipalType.Device
      case _ => false
    }

    override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {

      case bearerToken: OauthBearerToken =>
        bearerToken.authPrincipalType match {
          case AuthPrincipalType.Device => authorizeBearer(future(withSession(implicit s => organizationRepository.getDevice(bearerToken.principalID))), bearerToken, checkClient)
        }

      case clientIDSecret: ClientIDSecretToken =>
        clientIDSecret.authPrincipalType match {
          case AuthPrincipalType.Device => authorizeClientIDSecret(future(withSession(implicit s => organizationRepository.getDevice(bearerToken.principalID))), clientIDSecret, checkClient)
        }
    }

    override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
      val info = new SimpleAuthorizationInfo()
      val device = principals.byType(classOf[Device]).asScala.head
      info.addObjectPermissions(Permissions(device.permissions).asJavaCollection)
      info.addRoles(List(Roles.Device).asJavaCollection)
      info
    }
  }

}

trait UserInfoRealmBaseComponent extends BearerRealmBaseComponent with SessionProvider {
  this: TokenRepositoryComponent with UserRepositoryComponent with OrganizationRepositoryComponent =>

  trait UserInfoRealmBase extends AuthorizingRealm with BearerRealmBase {

    def getPrincipalByEmail(principal: String): Option[UserInfo]

    def getPrincipalByUsername(principal: String): Option[UserInfo]

    private def isEmail(value: String) = value.split("@").length == 2

    def checkClient(client: UserInfo) {
      if (!client.activated)
        throw new LockedAccountException("client is not activated")
      if (client.disabled)
        throw new DisabledAccountException("client is disabled")
    }

    override def doGetAuthenticationInfo(token: AuthenticationToken) = token match {
      case bearerToken: OauthBearerToken =>
        bearerToken.authPrincipalType match {
          case AuthPrincipalType.Admin => authorizeBearer(future(withSession(implicit s => organizationRepository.getAdminUser(bearerToken.principalID))), bearerToken, checkClient)
          case AuthPrincipalType.User => authorizeBearer(future(withSession(implicit s => userRepository.getUser(bearerToken.principalID))), bearerToken, checkClient)
        }

      case usernamePasswordToken: UsernamePasswordToken =>
        val userInfoF = if (isEmail(usernamePasswordToken.getPrincipal)) getPrincipalByEmail(usernamePasswordToken.getPrincipal) else getPrincipalByUsername(usernamePasswordToken.getPrincipal)
        userInfoF.map {
          userInfo =>
            checkClient(userInfo)
            new SimpleAuthenticationInfo(userInfo, userInfo.credential, Config.userInfoHash, getName)
        }.getOrElse(throw new UnknownAccountException(s"userinfo not found for ${usernamePasswordToken.getPrincipal}"))
    }
  }

}

trait AdminUserRealmComponent extends UserInfoRealmBaseComponent {
  this: TokenRepositoryComponent with UserRepositoryComponent with OrganizationRepositoryComponent =>

  object AdminUserRealm extends UserInfoRealmBase {
    //initialization block
    {
      setCredentialsMatcher(new UsernamePasswordBearerCredentialsMatcher(new HashedCredentialsMatcher(HashedAlgorithm.algorithmName)))
    }

    override def supports(token: AuthenticationToken) = token match {
      case t: UsernamePasswordToken => t.principalType == AuthPrincipalType.Admin
      case t: OauthBearerToken => t.authPrincipalType == AuthPrincipalType.Admin
      case _ => false
    }

    def getPrincipalByEmail(email: String): Option[UserInfo] = withSession(implicit s => organizationRepository.getAdminUserByEmail(email))

    def getPrincipalByUsername(username: String): Option[UserInfo] = withSession(implicit s => organizationRepository.getAdminUserByUsername(username))

    override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
      val info = new SimpleAuthorizationInfo()
      val user = principals.byType(classOf[AdminUser]).asScala.head
      info.addObjectPermission(Permissions(user.organizations))
      info.addObjectPermission(Permissions.forDomains(user.organizations.flatMap(_.databases)))
      info.addRoles(List(Roles.AdminUser, Roles.DomainAdmin, Roles.OrganizationAdmin).asJavaCollection)
      info
    }
  }

}

trait UserRealmComponent extends UserInfoRealmBaseComponent {
  this: TokenRepositoryComponent with UserRepositoryComponent with OrganizationRepositoryComponent =>

  object UserRealm extends UserInfoRealmBase {
    //initialization block
    {
      setCredentialsMatcher(new UsernamePasswordBearerCredentialsMatcher(new HashedCredentialsMatcher(HashedAlgorithm.algorithmName)))
    }

    override def supports(token: AuthenticationToken) = token match {
      case t: UsernamePasswordToken => t.principalType == AuthPrincipalType.User
      case t: OauthBearerToken => t.authPrincipalType == AuthPrincipalType.User
      case _ => false
    }

    def getPrincipalByEmail(email: String): Option[UserInfo] = withSession(implicit s => userRepository.getUserByEmail(email))

    def getPrincipalByUsername(username: String): Option[UserInfo] = withSession(implicit s => userRepository.getUserByUsername(username))

    override def doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo = {
      val info = new SimpleAuthorizationInfo()
      val user = principals.byType(classOf[User]).asScala.head
      info.addObjectPermissions(Permissions(user.permissions).asJavaCollection)
      info.addRoles(List(Roles.User).asJavaCollection)
      info
    }
  }

}

