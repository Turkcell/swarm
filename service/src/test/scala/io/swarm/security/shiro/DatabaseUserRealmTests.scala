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

import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import scala.collection.JavaConverters._
import org.apache.shiro.SecurityUtils
import io.swarm.security.{TokenInfo, HashedAlgorithm, AuthPrincipalType, TokenCategory}
import io.swarm.UUIDGenerator
import io.swarm.domain.DatabaseUser
import io.swarm.domain.UserInfo
import io.swarm.domain.Client
import com.github.nscala_time.time.Imports._

/**
 * Created by Anil Chalil on 11/15/13.
 */
@RunWith(classOf[JUnitRunner])
class DatabaseUserRealmTests extends FlatSpec with ShouldMatchers with DatabaseUserRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = DatabaseUserRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val user = DatabaseUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set())
  val userPass = "test"
  clientRepository.saveDatabaseUser(user)
  val validToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.DatabaseUser, user.id), 0.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  val expiredToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.DatabaseUser, user.id), 100.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }

  def disable {
    clientRepository.upsertDatabaseUser(user.copy(disabled = true))
  }

  def passivate {
    clientRepository.upsertDatabaseUser(user.copy(activated = false))
  }

  def revert(user: Client) {
    clientRepository.upsertDatabaseUser(user.asInstanceOf[UserInfo])
  }

  "DatabaseUser" should behave like basic(new UsernamePasswordToken(user.username, "test", AuthPrincipalType.DatabaseUser), new UsernamePasswordToken(user.username, "wrong", AuthPrincipalType.DatabaseUser), new UsernamePasswordToken("wrıng", "wrong", AuthPrincipalType.DatabaseUser), validToken, expiredToken)
  "DatabaseUser" should behave like client(user, validToken)
  "DatabaseUser" should behave like user(user, userPass, AuthPrincipalType.DatabaseUser)

}
