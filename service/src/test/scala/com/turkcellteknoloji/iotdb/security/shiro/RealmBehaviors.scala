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
import com.turkcellteknoloji.iotdb.domain.Client
import org.apache.shiro.authc.AuthenticationToken

trait ClientRealmBehaviors {
  this: FlatSpec with ShouldMatchers with InMemoryComponents =>

  def disable
  def passivate
  def revert(user: Client)

  def client(client: Client, userPass: String, principalType: AuthPrincipalType, validAuthToken: AuthenticationToken, invalidAuthToken: AuthenticationToken, validToken: OauthBearerToken, expiredToken: OauthBearerToken) {
    def withDisabledUser(testCode: => Any) {
      try {
        disable
        testCode
      } finally revert(client)
    }

    def withNotActivatedUser(testCode: => Any) {
      try {
        passivate
        testCode
      } finally revert(client)
    }
    it should "authenticate with valid token" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject.login(validAuthToken)
      SecurityUtils.getSubject().getPrincipal() should be(validAuthToken.getPrincipal())
    }

    it should "throw authentication exception with wrong credentials" in {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject.login(invalidAuthToken)
      }
    }
    it should "authenticate with bearer token" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      SecurityUtils.getSubject().getPrincipal().asInstanceOf[Client].id shouldBe validToken.principalID
    }
    it should "fail with expired bearer token" in {
      intercept[ExpiredTokenException] {
        Thread.sleep(110)
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(expiredToken.token))
      }
    }

    it should "fail with disabled client" in withDisabledUser {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      }
    }

    it should "fail with not active client" in withNotActivatedUser {
      intercept[AuthenticationException] {
        SecurityUtils.getSubject().isAuthenticated() should be(false)
        SecurityUtils.getSubject().login(OauthBearerToken(validToken.token))
      }
    }
  }
}
trait UserRealmBehaviors extends ClientRealmBehaviors {
  this: FlatSpec with ShouldMatchers with InMemoryComponents =>
  def user(user: UserInfo, userPass: String, principalType: AuthPrincipalType) {
    it should "authenticate with email and password" in {
      SecurityUtils.getSubject().isAuthenticated() should be(false)
      SecurityUtils.getSubject.login(new UsernamePasswordToken(user.email, userPass, principalType))
      SecurityUtils.getSubject().getPrincipal() should be(user.email)
    }
  }
}
