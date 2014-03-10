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

import org.scalatest.{BeforeAndAfterAllConfigMap, FlatSpec, ShouldMatchers}
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.UUIDGenerator
import io.swarm.security._
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.management.Management.{DomainRef, ServiceProviderRegistryComponent}
import io.swarm.management.dao.OrganizationRepositoryDaoComponent


class DomainRealmTests extends FlatSpec with ShouldMatchers with BeforeAndAfterAllConfigMap with DomainRealmComponent with InMemoryComponents with OrganizationRepositoryDaoComponent with RealmTestsBase with BasicRealmBehaviors with HSQLInMemoryManagementDaoComponent with ServiceProviderRegistryComponent {
  val realm = DatabaseRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)

  val domainNonExist = DomainRef(UUIDGenerator.randomGenerator.generate(), "nonExist")
  val secret = ClientSecret(AuthPrincipalType.Domain)
  var validToken: OauthBearerToken = null
  var expiredToken: OauthBearerToken = null
  db.withSession {
    implicit s =>
      managementDao.create
      organizationRepository.saveAdminUser(TestData.admin)
      organizationRepository.saveOrganization(TestData.org)

      tokenRepository.saveClientSecret(ClientID(TestData.domain), secret)
      validToken = {
        val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Domain, TestData.domain.id), 0.toDuration, 0)
        tokenRepository.putTokenInfo(tokenInfo)
        OauthBearerToken(tokenInfo)
      }
      expiredToken = {
        val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Domain, TestData.domain.id), 100.toDuration, 0)
        tokenRepository.putTokenInfo(tokenInfo)
        OauthBearerToken(tokenInfo)
      }
  }

  "Database" should behave like basic(ClientIDSecretToken(ClientID(TestData.domain), secret), ClientIDSecretToken(ClientID(TestData.domain), ClientSecret(AuthPrincipalType.Domain)), ClientIDSecretToken(ClientID(domainNonExist), ClientSecret(AuthPrincipalType.Domain)), validToken, expiredToken)
}
