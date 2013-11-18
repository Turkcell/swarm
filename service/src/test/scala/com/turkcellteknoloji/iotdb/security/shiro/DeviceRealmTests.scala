package com.turkcellteknoloji.iotdb.security.shiro

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import com.turkcellteknoloji.iotdb.security.TokenCategory
import com.turkcellteknoloji.iotdb.security.TokenType
import com.turkcellteknoloji.iotdb.security.AuthPrincipalInfo
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain.Client
import com.turkcellteknoloji.iotdb.domain.Device
import com.turkcellteknoloji.iotdb.domain.Database
import com.turkcellteknoloji.iotdb.domain.AdminUser
import com.turkcellteknoloji.iotdb.UUIDGenerator
import org.apache.shiro.crypto.hash.Sha1Hash
import com.turkcellteknoloji.iotdb.Config
import com.turkcellteknoloji.iotdb.domain.Database
import com.turkcellteknoloji.iotdb.domain.Organization
import com.turkcellteknoloji.iotdb.domain.ResourceRepository
import com.turkcellteknoloji.iotdb.domain.DatabaseMetadata
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import scala.collection.JavaConverters._
import com.turkcellteknoloji.iotdb.security.ClientSecret
import com.turkcellteknoloji.iotdb.security.ClientID
import com.turkcellteknoloji.iotdb.security.ClientIDSecretToken

class DeviceRealmTests extends FlatSpec with ShouldMatchers with DeviceRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = new DeviceRealm {
    def doGetAuthorizationInfo(principals: PrincipalCollection) = null
  }
  realm.setCredentialsMatcher(new ClientIDSecretBearerCredentialsMatcher)
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val user = AdminUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", new Sha1Hash("test", Config.userInfoHash).toHex(), true, true, false)
  clientRepository.saveAdminUser(user)
  val org = Organization(UUIDGenerator.secretGenerator.generate(), "testorg", Set(user))
  resourceRepository.saveOrganization(org)
  val database = Database(UUIDGenerator.secretGenerator.generate(), "testdb", DatabaseMetadata(3600 * 1000 * 24), OrganizationInfo(org.id, org.name))
  resourceRepository.saveDatabase(database)
  val device = Device(UUIDGenerator.secretGenerator.generate(), "mydevice", DatabaseInfo(database.id, database.name), true, false)
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

  "Device" should behave like client(device, userPass, AuthPrincipalType.DatabaseUser, ClientIDSecretToken(ClientID(device), secret), ClientIDSecretToken(ClientID(device), secret), validToken, expiredToken)
}