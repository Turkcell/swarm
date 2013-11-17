package com.turkcellteknoloji.iotdb.security.shiro

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import org.apache.shiro.subject.PrincipalCollection
import org.apache.shiro.authc.credential.Sha1CredentialsMatcher
import org.apache.shiro.mgt.DefaultSecurityManager
import org.apache.shiro.realm.Realm
import org.apache.shiro.SecurityUtils
import com.turkcellteknoloji.iotdb.security.TokenCategory
import com.turkcellteknoloji.iotdb.security.TokenType
import com.turkcellteknoloji.iotdb.security.AuthPrincipalInfo
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain.UserInfo
import com.turkcellteknoloji.iotdb.domain.AdminUser
import com.turkcellteknoloji.iotdb.UUIDGenerator
import org.apache.shiro.crypto.hash.Sha1Hash
import com.turkcellteknoloji.iotdb.Config
import scala.collection.JavaConverters._
import com.turkcellteknoloji.iotdb.security.UsernamePasswordToken
import com.turkcellteknoloji.iotdb.domain.Client

class AdminUserRealmTests extends FlatSpec with ShouldMatchers with AdminUserRealmComponent with InMemoryComponents with RealmTestsBase with UserRealmBehaviors {
  val realm = new AdminUserRealm {
    def doGetAuthorizationInfo(principals: PrincipalCollection) = null
  }
  realm.setCredentialsMatcher(new UsernamePasswordBearerCredentialsMatcher(new Sha1CredentialsMatcher))
  val sec = new DefaultSecurityManager()
  sec.setAuthenticator(new ExclusiveRealmAuthenticator)
  sec.setRealms(List(realm.asInstanceOf[Realm]).asJava)
  SecurityUtils.setSecurityManager(sec)
  val user = AdminUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", new Sha1Hash("test", Config.userInfoHash).toHex(), true, true, false)
  val userPass = "test"
  clientRepository.saveAdminUser(user)
  val validToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, user.id), 0, 0)
  val expiredToken = tokenRepository.createOauthToken(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, user.id), 100, 0)
  def disable {
    clientRepository.upsertAdminUser(user.copy(disabled = true))
  }
  def passivate {
    clientRepository.upsertAdminUser(user.copy(activated = false))
  }

  def revert(user: Client) {
    clientRepository.upsertAdminUser(user.asInstanceOf[UserInfo])
  }
  "AdminUser" should behave like client(user, userPass, AuthPrincipalType.Admin, new UsernamePasswordToken(user.username, userPass, AuthPrincipalType.Admin), new UsernamePasswordToken(user.username, "wrong pass", AuthPrincipalType.Admin), validToken, expiredToken)
  "AdminUser" should behave like user(user, userPass, AuthPrincipalType.Admin)
}