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
import io.swarm.domain.Device
import io.swarm.UUIDGenerator
import io.swarm.domain.DatabaseInfo
import scala.collection.JavaConverters._
import com.github.nscala_time.time.Imports._
import io.swarm.domain.persistence.slick.{ManagementRepositoryComponentJDBC, ClientRepositoryComponentSlick}
import io.swarm.infrastructure.persistence.slick.SlickPersistenceSessionComponent

class DeviceRealmTests extends FlatSpec with ShouldMatchers with DeviceRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors with HSQLInMemoryClientResourceDaoComponent with ClientRepositoryComponentSlick with ManagementRepositoryComponentJDBC with SlickPersistenceSessionComponent {
  val realm = DeviceRealm
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  clientRepository.saveAdminUser(TestData.user)
  resourceRepository.saveOrganization(TestData.org)
  val device = Device(UUIDGenerator.randomGenerator.generate(), "mydevice", DatabaseInfo(TestData.database.id, TestData.database.name, 0), true, false, Set(), 0)
  val deviceNoneExistent = Device(UUIDGenerator.randomGenerator.generate(), "mydevice2", DatabaseInfo(TestData.database.id, TestData.database.name, 0), true, false, Set(), 0)
  val userPass = "test"
  clientRepository.saveDevice(device)
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
    clientRepository.updateDevice(device.copy(disabled = true))
  }

  def passivate {
    clientRepository.updateDevice(device.copy(activated = false))
  }

  def revert(device: Client) {
    clientRepository.updateDevice(device.asInstanceOf[Device])
  }

  "Device" should behave like basic(ClientIDSecretToken(ClientID(device), secret), ClientIDSecretToken(ClientID(device), ClientSecret(AuthPrincipalType.Device)), ClientIDSecretToken(ClientID(deviceNoneExistent), ClientSecret(AuthPrincipalType.Device)), validToken, expiredToken)
  "Device" should behave like client(device, validToken)

}