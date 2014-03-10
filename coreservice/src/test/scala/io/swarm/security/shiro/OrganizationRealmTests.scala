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
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.management.dao.OrganizationRepositoryDaoComponent
import io.swarm.management.Management.{ServiceProviderRegistryComponent, Organization}

/**
 * Created by Anil Chalil on 11/19/13.
 */
class OrganizationRealmTests extends FlatSpec with ShouldMatchers with OrganizationRealmComponent with InMemoryComponents with RealmTestsBase with BasicRealmBehaviors with HSQLInMemoryManagementDaoComponent with OrganizationRepositoryDaoComponent with ServiceProviderRegistryComponent {
  val realm = OrganizationRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val secret = ClientSecret(AuthPrincipalType.Organization)
  val orgNonExistent = Organization(UUIDGenerator.randomGenerator.generate(), "testorgNonExist", false, Set(), Set())
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
  db.withSession {
    implicit s =>
      managementDao.create
      organizationRepository.saveAdminUser(TestData.admin)
      organizationRepository.saveOrganization(TestData.org)
      tokenRepository.saveClientSecret(ClientID(TestData.org.organizationRef), secret)

  }

  "Organization" should behave like basic(ClientIDSecretToken(ClientID(TestData.org.organizationRef), secret), ClientIDSecretToken(ClientID(TestData.org.organizationRef), ClientSecret(AuthPrincipalType.Organization)), ClientIDSecretToken(ClientID(orgNonExistent.organizationRef), ClientSecret(AuthPrincipalType.Organization)), validToken, expiredToken)
}
