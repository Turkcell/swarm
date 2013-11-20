/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.swarm.security.shiro

import org.scalatest._
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.{Config, UUIDGenerator}
import org.apache.shiro.crypto.hash.Sha1Hash
import io.swarm.security._
import io.swarm.domain.Organization
import io.swarm.domain.AdminUser
import scala.collection.JavaConverters._

/**
 * Created by Anil Chalil on 11/19/13.
 */
class OrganizationRealmTests extends FlatSpec with ShouldMatchers with OrganizationRealmComponent with InMemoryComponents with RealmTestsBase with BasicRealmBehaviors {
  val realm = new OrganizationRealm {
    def doGetAuthorizationInfo(principals: PrincipalCollection) = null
  }
  realm.setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val user = AdminUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", new Sha1Hash("test", Config.userInfoHash).toHex(), true, true, false)
  clientRepository.saveAdminUser(user)
  val org = Organization(UUIDGenerator.secretGenerator.generate(), "testorg", Set(user))
  val orgNonExistent = Organization(UUIDGenerator.secretGenerator.generate(), "testorgnonexist", Set(user))
  resourceRepository.saveOrganization(org)
  val secret = ClientSecret(AuthPrincipalType.Organization)
  tokenRepository.saveClientSecret(ClientID(org), secret)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Organization, org.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Organization, org.id), 100, 0)
  "Organization" should behave like basic(ClientIDSecretToken(ClientID(org), secret), ClientIDSecretToken(ClientID(org), ClientSecret(AuthPrincipalType.Organization)), ClientIDSecretToken(ClientID(orgNonExistent), ClientSecret(AuthPrincipalType.Organization)), validToken, expiredToken)
}
