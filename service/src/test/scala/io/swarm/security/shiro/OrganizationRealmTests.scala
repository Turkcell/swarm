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
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.UUIDGenerator
import io.swarm.security._
import io.swarm.domain.Organization
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.domain.persistence.slick.{ResourceRepositoryComponentJDBC, ClientRepositoryComponentSlick}
import io.swarm.infrastructure.persistence.slick.SlickPersistenceSessionComponent

/**
 * Created by Anil Chalil on 11/19/13.
 */
class OrganizationRealmTests extends FlatSpec with ShouldMatchers with OrganizationRealmComponent with InMemoryComponents with RealmTestsBase with BasicRealmBehaviors with HSQLInMemoryClientResourceDaoComponent with ClientRepositoryComponentSlick with ResourceRepositoryComponentJDBC with SlickPersistenceSessionComponent {
  val realm = OrganizationRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  clientRepository.saveAdminUser(TestData.user)
  val orgNonExistent = Organization(UUIDGenerator.randomGenerator.generate(), "testorgnonexist", Set(), 0)
  resourceRepository.saveOrganization(TestData.org)
  val secret = ClientSecret(AuthPrincipalType.Organization)
  tokenRepository.saveClientSecret(ClientID(TestData.org), secret)
  val validToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Organization, TestData.org.id), 0.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  val expiredToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Organization, TestData.org.id), 100.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  "Organization" should behave like basic(ClientIDSecretToken(ClientID(TestData.org), secret), ClientIDSecretToken(ClientID(TestData.org), ClientSecret(AuthPrincipalType.Organization)), ClientIDSecretToken(ClientID(orgNonExistent), ClientSecret(AuthPrincipalType.Organization)), validToken, expiredToken)
}
