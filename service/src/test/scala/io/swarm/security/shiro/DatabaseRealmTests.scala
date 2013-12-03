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
import io.swarm.domain.Database
import io.swarm.domain.DatabaseMetadata

/**
 * Created by Anil Chalil on 11/19/13.
 */
class DatabaseRealmTests extends FlatSpec with ShouldMatchers with DatabaseRealmComponent with InMemoryComponents with RealmTestsBase with BasicRealmBehaviors {
  val realm = DatabaseRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  clientRepository.saveAdminUser(TestData.user)
  resourceRepository.saveOrganization(TestData.org)
  val databaseNonExist = Database(UUIDGenerator.secretGenerator.generate(), "nonExist", DatabaseMetadata(3600 * 1000 * 24))
  val secret = ClientSecret(AuthPrincipalType.Database)
  tokenRepository.saveClientSecret(ClientID(TestData.database), secret)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Database, TestData.database.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Database, TestData.database.id), 100, 0)
  "Database" should behave like basic(ClientIDSecretToken(ClientID(TestData.database), secret), ClientIDSecretToken(ClientID(TestData.database), ClientSecret(AuthPrincipalType.Database)), ClientIDSecretToken(ClientID(databaseNonExist), ClientSecret(AuthPrincipalType.Database)), validToken, expiredToken)
}
