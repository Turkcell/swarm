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
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.security.TokenCategory
import io.swarm.security.AuthPrincipalType
import io.swarm.domain.UserInfo
import io.swarm.domain.AdminUser
import io.swarm.UUIDGenerator
import org.apache.shiro.crypto.hash.Sha1Hash
import io.swarm.Config
import scala.collection.JavaConverters._
import io.swarm.domain.Client

class AdminUserRealmTests extends FlatSpec with ShouldMatchers with AdminUserRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = AdminUserRealm
  realm.setCredentialsMatcher(new UsernamePasswordBearerCredentialsMatcher(new Sha1CredentialsMatcher))
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val userPass = "test"
  clientRepository.saveAdminUser(TestData.user)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, TestData.user.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, TestData.user.id), 100, 0)

  def disable {
    clientRepository.upsertAdminUser(TestData.user.copy(disabled = true))
  }

  def passivate {
    clientRepository.upsertAdminUser(TestData.user.copy(activated = false))
  }

  def revert(user: Client) {
    clientRepository.upsertAdminUser(user.asInstanceOf[UserInfo])
  }

  "AdminUser" should behave like basic(new UsernamePasswordToken(TestData.user.username, userPass, AuthPrincipalType.Admin), new UsernamePasswordToken(TestData.user.username, "wrong pass", AuthPrincipalType.Admin), new UsernamePasswordToken("wrong", "wrong", AuthPrincipalType.Admin), validToken, expiredToken)
  "AdminUser" should behave like client(TestData.user, validToken)
  "AdminUser" should behave like user(TestData.user, userPass, AuthPrincipalType.Admin)
}