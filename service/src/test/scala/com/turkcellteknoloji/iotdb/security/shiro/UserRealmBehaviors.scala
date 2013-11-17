package com.turkcellteknoloji.iotdb.security.shiro

import org.scalatest.FlatSpec
import org.apache.shiro.SecurityUtils
import org.scalatest.matchers.ShouldMatchers
import com.turkcellteknoloji.iotdb.security.UsernamePasswordToken
import com.turkcellteknoloji.iotdb.domain.UserInfo
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType
import org.apache.shiro.authc.AuthenticationException
import com.turkcellteknoloji.iotdb.security.ExpiredTokenException
import com.turkcellteknoloji.iotdb.security.OauthBearerToken

trait UserRealmBehaviors {
  this: FlatSpec with ShouldMatchers with InMemoryComponents =>

  def disable
  def passivate
  def revert(user: UserInfo)

  def realm(user: UserInfo, userPass: String, principalType: AuthPrincipalType, validToken: OauthBearerToken, expiredToken: OauthBearerToken) {
    def withDisabledUser(testCode: => Any) {
      try {
        disable
        testCode
      } finally revert(user)
    }

    def withNotActivatedUser(testCode: => Any) {
      try {
        passivate
        testCode
      } finally revert(user)
    }
    it should "authenticate with username password" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject.login(new UsernamePasswordToken(user.username, userPass, principalType))
      SecurityUtils.getSubject().getPrincipal() should be(user.username)
    }

    it should "authenticate with email and password" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject.login(new UsernamePasswordToken(user.email, userPass, principalType))
      SecurityUtils.getSubject().getPrincipal() should be(user.email)
    }
    it should "throw authentication exception with wrong password" in {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject.login(new UsernamePasswordToken(user.username, "wrongpass", principalType))
      }
    }
    it should "authenticate with bearer token" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      SecurityUtils.getSubject().getPrincipal().asInstanceOf[UserInfo].id shouldBe user.id
    }
    it should "fail with expired bearer token" in {
      intercept[ExpiredTokenException] {
        Thread.sleep(110)
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(expiredToken.token))
      }
    }

    it should "fail with disabled user" in withDisabledUser {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      }
    }

    it should "fail with not active user" in withNotActivatedUser {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      }
    }
  }
}