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

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.UUIDGenerator
import io.swarm.security._
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.domain
import io.swarm.domain.DatabaseMetadata
import io.swarm.domain.persistence.slick.{ResourceRepositoryComponentJDBC, ClientRepositoryComponentSlick}
import io.swarm.infrastructure.persistence.slick.SlickPersistenceSessionComponent


class DatabaseRealmTests extends FlatSpec with ShouldMatchers with DatabaseRealmComponent with InMemoryComponents with ClientRepositoryComponentSlick with ResourceRepositoryComponentJDBC with RealmTestsBase with BasicRealmBehaviors with HSQLInMemoryClientResourceDaoComponent with SlickPersistenceSessionComponent {
  val realm = DatabaseRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  db.withDynSession {
    clientRepository.saveAdminUser(TestData.user)
    resourceRepository.saveOrganization(TestData.org)
  }
  val databaseNonExist = domain.Database(UUIDGenerator.randomGenerator.generate(), "nonExist", DatabaseMetadata(3600 * 1000 * 24), 0)
  val secret = ClientSecret(AuthPrincipalType.Database)
  tokenRepository.saveClientSecret(ClientID(TestData.database), secret)
  val validToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Database, TestData.database.id), 0.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  val expiredToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Database, TestData.database.id), 100.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  "Database" should behave like basic(ClientIDSecretToken(ClientID(TestData.database), secret), ClientIDSecretToken(ClientID(TestData.database), ClientSecret(AuthPrincipalType.Database)), ClientIDSecretToken(ClientID(databaseNonExist), ClientSecret(AuthPrincipalType.Database)), validToken, expiredToken)
}
