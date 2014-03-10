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
import io.swarm.UUIDGenerator
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.management.dao.OrganizationRepositoryDaoComponent
import io.swarm.management.Management.{ServiceProviderRegistryComponent, DeviceRef}

class DeviceRealmTests extends FlatSpec with ShouldMatchers with DeviceRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors with HSQLInMemoryManagementDaoComponent with OrganizationRepositoryDaoComponent with ServiceProviderRegistryComponent {
  val realm = DeviceRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val device = DeviceRef(UUIDGenerator.randomGenerator.generate(), "mydevice", true, false)
  val deviceNoneExistent = DeviceRef(UUIDGenerator.randomGenerator.generate(), "mydevice2", true, false)
  val userPass = "test"

  db.withSession{
    implicit s=>
      managementDao.create
      organizationRepository.saveAdminUser(TestData.admin)
      organizationRepository.saveOrganization(TestData.org)
      organizationRepository.saveDeviceRef(device)
  }

  val secret = ClientSecret(AuthPrincipalType.Device)
  tokenRepository.saveClientSecret(ClientID(device), secret)
  val validToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Device, device.id), 0.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }
  val expiredToken = {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Device, device.id), 100.toDuration, 0)
    tokenRepository.putTokenInfo(tokenInfo)
    OauthBearerToken(tokenInfo)
  }

  def disable {
    db.withSession(implicit s=>organizationRepository.updateDeviceRef(device.copy(disabled = true)))
  }

  def passivate {
    db.withSession(implicit s=>organizationRepository.updateDeviceRef(device.copy(activated = false)))
  }

  def revert(device: Client) {
    db.withSession(implicit s=>organizationRepository.updateDeviceRef(device.asInstanceOf[DeviceRef]))
  }

  "Device" should behave like basic(ClientIDSecretToken(ClientID(device), secret), ClientIDSecretToken(ClientID(device), ClientSecret(AuthPrincipalType.Device)), ClientIDSecretToken(ClientID(deviceNoneExistent), ClientSecret(AuthPrincipalType.Device)), validToken, expiredToken)
  "Device" should behave like client(device, validToken)

}