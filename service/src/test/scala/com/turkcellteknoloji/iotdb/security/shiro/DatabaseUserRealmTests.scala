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

import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import java.util.ArrayList
import scala.collection.JavaConverters._
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.SecurityUtils
import org.apache.shiro.crypto.hash.Sha1Hash
import com.turkcellteknoloji.iotdb.security.UsernamePasswordToken
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain.DatabaseUser
import com.turkcellteknoloji.iotdb.{ UUIDGenerator, Config }
import org.apache.shiro.crypto.hash.Sha1Hash
import org.apache.shiro.authc.AuthenticationException
import com.turkcellteknoloji.iotdb.security.TokenCategory
import com.turkcellteknoloji.iotdb.security.TokenType
import com.turkcellteknoloji.iotdb.security.AuthPrincipalInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseUser
import com.turkcellteknoloji.iotdb.security.OauthBearerToken
import com.turkcellteknoloji.iotdb.security.ExpiredTokenException
import com.turkcellteknoloji.iotdb.domain.UserInfo
/**
 * Created by Anil Chalil on 11/15/13.
 */
@RunWith(classOf[JUnitRunner])
class DatabaseUserRealmTests extends FlatSpec with ShouldMatchers with DatabaseUserRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = new DatabaseUserRealm {
    def doGetAuthorizationInfo(principals: PrincipalCollection) = null
  }
  realm.setCredentialsMatcher(new UsernamePasswordBearerCredentialsMatcher(new Sha1CredentialsMatcher))
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val user = DatabaseUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", new Sha1Hash("test", Config.userInfoHash).toHex(), true, true, false)
  val userPass = "test"
  clientRepository.saveDatabaseUser(user)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.DatabaseUser, user.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.DatabaseUser, user.id), 100, 0)
  def disable {
    clientRepository.upsertDatabaseUser(user.copy(disabled = true))
  }
  def passivate {
    clientRepository.upsertDatabaseUser(user.copy(activated = false))
  }

  def revert(user: UserInfo) {
    clientRepository.upsertDatabaseUser(user)
  }
  "DatabaseUser" should behave like realm(user, userPass, AuthPrincipalType.DatabaseUser, validToken, expiredToken)
}
