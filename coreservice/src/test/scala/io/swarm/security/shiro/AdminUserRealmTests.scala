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
import io.swarm.security.{TokenInfo, TokenCategory, AuthPrincipalType}
import io.swarm.domain.Client
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.management.dao.{UserRepositoryDaoComponent, OrganizationRepositoryDaoComponent}
import io.swarm.management.Management.{ServiceProviderRegistryComponent, AdminUserRef}

class AdminUserRealmTests extends FlatSpec with ShouldMatchers with AdminUserRealmComponent with RealmTestsBase with UserRealmBehaviors with HSQLInMemoryManagementDaoComponent with OrganizationRepositoryDaoComponent with UserRepositoryDaoComponent with ServiceProviderRegistryComponent with InMemoryComponents {
  val realm = AdminUserRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val userPass = "test"
  db.withSession {
    implicit session =>
      managementDao.create
      organizationRepository.saveAdminUser(TestData.admin)
      organizationRepository.saveOrganization(TestData.org)
  }
  val validToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, TestData.admin.id), 0.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  val expiredToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, TestData.admin.id), 100.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }

  def disable {
    val u = TestData.admin
    val newAdmin = new AdminUserRef(u.id, u.name, u.surname, u.username, u.email, u.credential, u.activated, u.confirmed, true)
    db.withSession(implicit s=>organizationRepository.updateAdminUser(newAdmin))
  }

  def passivate {
    val u = TestData.admin
    val newAdmin = new AdminUserRef(u.id, u.name, u.surname, u.username, u.email, u.credential, false, u.confirmed, u.disabled)
    db.withSession(implicit s=>organizationRepository.updateAdminUser(newAdmin))
  }

  def revert(user: Client) {
    db.withSession(implicit s=>organizationRepository.updateAdminUser(user.asInstanceOf[AdminUserRef]))
  }

  "AdminUser" should behave like basic(new UsernamePasswordToken(TestData.admin.username, userPass, AuthPrincipalType.Admin), new UsernamePasswordToken(TestData.admin.username, "wrong pass", AuthPrincipalType.Admin), new UsernamePasswordToken("wrong", "wrong", AuthPrincipalType.Admin), validToken, expiredToken)
  "AdminUser" should behave like client(TestData.admin, validToken)
  "AdminUser" should behave like user(TestData.admin, userPass, AuthPrincipalType.Admin)
}