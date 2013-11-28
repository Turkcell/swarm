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
import org.scalatest.matchers.ShouldMatchers
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import io.swarm.security.TokenCategory
import io.swarm.security.AuthPrincipalType
import io.swarm.domain.Client
import io.swarm.domain.Device
import io.swarm.domain.AdminUser
import io.swarm.UUIDGenerator
import org.apache.shiro.crypto.hash.Sha1Hash
import io.swarm.Config
import io.swarm.domain.Database
import io.swarm.domain.Organization
import io.swarm.domain.DatabaseMetadata
import io.swarm.domain.OrganizationInfo
import io.swarm.domain.DatabaseInfo
import scala.collection.JavaConverters._

class DeviceRealmTests extends FlatSpec with ShouldMatchers with DeviceRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = DeviceRealm
  realm.setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  clientRepository.saveAdminUser(TestData.user)
  resourceRepository.saveOrganization(TestData.org)
  resourceRepository.saveDatabase(TestData.database)
  val device = Device(UUIDGenerator.secretGenerator.generate(), "mydevice", DatabaseInfo(TestData.database.id, TestData.database.name), true, false,Set())
  val deviceNoneExistent = Device(UUIDGenerator.secretGenerator.generate(), "mydevice2", DatabaseInfo(TestData.database.id, TestData.database.name), true, false,Set())
  val userPass = "test"
  clientRepository.saveDevice(device)
  val secret = ClientSecret(AuthPrincipalType.Device)
  tokenRepository.saveClientSecret(ClientID(device), secret)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Device, device.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Device, device.id), 100, 0)

  def disable {
    clientRepository.upsertDevice(device.copy(disabled = true))
  }

  def passivate {
    clientRepository.upsertDevice(device.copy(activated = false))
  }

  def revert(device: Client) {
    clientRepository.upsertDevice(device.asInstanceOf[Device])
  }

  "Device" should behave like basic(ClientIDSecretToken(ClientID(device), secret), ClientIDSecretToken(ClientID(device), ClientSecret(AuthPrincipalType.Device)), ClientIDSecretToken(ClientID(deviceNoneExistent), ClientSecret(AuthPrincipalType.Device)), validToken, expiredToken)
  "Device" should behave like client(device, validToken)

}