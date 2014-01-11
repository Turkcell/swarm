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

package io.swarm
package security

import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.github.nscala_time.time.Imports._
import java.nio.BufferUnderflowException
import java.lang.IllegalArgumentException
import io.swarm.security.shiro._
import io.swarm.management.Management._
import io.swarm.management.Management.UserRef
import io.swarm.management.Management.AdminUserRef
import io.swarm.security.shiro.AuthPrincipalInfo
import io.swarm.management.Management.OrganizationRef
import scala.Some
import io.swarm.management.Management.DeviceRef

/**
 * Created by Anil Chalil on 11/12/13.
 */
class TokenTests extends FlatSpec with ShouldMatchers {
  val org = OrganizationRef(UUIDGenerator.randomGenerator.generate(), "testorg", false)
  val tmpAdminUser = AdminUserRef(UUIDGenerator.randomGenerator.generate(), Some("anil"), Some("halil"), "user1", "user@user.com", HashedAlgorithm.toHex("mypass"), activated = true, confirmed = true, disabled = false)
  val userRef = UserRef(UUIDGenerator.randomGenerator.generate(), Some("anil"), Some("halil"), "user1", "user@user.com", HashedAlgorithm.toHex("mypass"), activated = true, confirmed = true, disabled = false)
  "token " should " construct an OauthBearerToken" in {
    val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.randomGenerator.generate()), 0.toDuration, 0)
    val direct = OauthBearerToken(tokenInfo)
    val fromStr = OauthBearerToken(direct.token)
    fromStr shouldBe direct
  }

  it should "throw NotTokenException" in {
    intercept[NotTokenException] {
      OauthBearerToken("invalid")
    }
  }

  it should "throw ExpiredTokenException" in {
    intercept[ExpiredTokenException] {
      val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.randomGenerator.generate()), 1.toDuration, 0)
      val token = OauthBearerToken(tokenInfo)
      Thread.sleep(10)
      OauthBearerToken(token.token)
    }
  }

  it should "throw NotTokenException with match error" in {
    val cause = intercept[NotTokenException] {
      OauthBearerToken("invalida")
    }.getCause
    assert(cause.isInstanceOf[MatchError], s"Execution end with cause ${cause.getClass.getName} instead of ${classOf[MatchError].getName}")
  }

  it should "throw NotTokenException with buffer overflow " in {
    val cause = intercept[NotTokenException] {
      OauthBearerToken(TokenCategory.Access.base64Prefix + AuthPrincipalType.Admin.base64Prefix)
    }.getCause
    assert(cause.isInstanceOf[BufferUnderflowException], s"Execution end with cause ${cause.getClass.getName} instead of ${classOf[BufferUnderflowException].getName}")
  }

  it should "throw BadTokenException when signature not match" in {
    intercept[BadTokenException] {
      val tokenInfo = TokenInfo(TokenCategory.Access, TokenType.Access, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.randomGenerator.generate()), 0.toDuration, 0)
      val a = OauthBearerToken(tokenInfo)
      val t = a.token
      if (t.last.isDigit) {
        OauthBearerToken(t.take(t.length - 1) + (t.last.asDigit - 1).toString)
      } else if (t.last.isLetter) {
        OauthBearerToken(t.take(t.length - 1) + (if (t.last.isUpper) t.last.toLower else t.last.toUpper))
      } else
        OauthBearerToken(t.take(t.length - 1) + (if (t.last == '-') "_" else "-"))
    }
  }

  "client id " should " should construct a ClientID" in {
    val org = ClientID(OrganizationRef(UUIDGenerator.randomGenerator.generate(), "org", false))
    val domain = Domain(UUIDGenerator.randomGenerator.generate(), "db")
    val db = ClientID(domain)
    val dev = ClientID(DeviceRef(UUIDGenerator.randomGenerator.generate(), "device", true, false))

    org shouldBe ClientID(org.id)
    db shouldBe ClientID(db.id)
    dev shouldBe ClientID(dev.id)
  }

  it should "throw not NotClientIDException with matchError" in {
    val cause = intercept[NotClientIDException] {
      ClientID("invalid")
    }.getCause
    assert(cause.isInstanceOf[MatchError], s"Execution end with cause ${cause.getClass.getName} instead of ${classOf[MatchError].getName}")
  }

  it should "throw not NotClientIDException with BufferUnderflowException" in {
    val cause = intercept[NotClientIDException] {
      ClientID(AuthPrincipalType.Domain.base64Prefix + "test")
    }.getCause
    val cause2 = intercept[NotClientIDException] {
      ClientID(AuthPrincipalType.Domain.base64Prefix)
    }.getCause
    assert(cause.isInstanceOf[BufferUnderflowException], s"Execution end with cause ${cause.getClass.getName} instead of ${classOf[BufferUnderflowException].getName}")
    assert(cause2.isInstanceOf[BufferUnderflowException], s"Execution end with cause ${cause.getClass.getName} instead of ${classOf[BufferUnderflowException].getName}")
  }

  it should "throw NotClientIDException " in {
    intercept[NotClientIDException] {
      ClientID("i")
    }
  }

  it should "throw NotClientIDException when prefix is admin or databaseuser" in {
    intercept[NotClientIDException] {
      ClientID(AuthPrincipalType.Admin.base64Prefix)
    }
    intercept[NotClientIDException] {
      ClientID(AuthPrincipalType.User.base64Prefix)
    }
  }

  "client secret " should "construct a ClientSecret" in {
    val orgSecret = ClientSecret(AuthPrincipalType.Organization)
    val dbSecret = ClientSecret(AuthPrincipalType.Domain)
    val devSecret = ClientSecret(AuthPrincipalType.Device)

    orgSecret shouldBe ClientSecret(orgSecret.secret)
    dbSecret shouldBe ClientSecret(dbSecret.secret)
    devSecret shouldBe ClientSecret(devSecret.secret)
  }

  it should " throw NotClientSecretException " in {
    intercept[NotClientSecretException] {
      ClientSecret("invalid")
    }
  }

  it should " throw NotClientSecretException for Admin prefix" in {
    intercept[NotClientSecretException] {
      ClientSecret(AuthPrincipalType.Admin.base64Prefix)
    }
    intercept[NotClientSecretException] {
      ClientSecret(AuthPrincipalType.User.base64Prefix)
    }
  }

  it should " throw IllegalArgumentException" in {
    intercept[IllegalArgumentException] {
      ClientSecret(AuthPrincipalType.Admin)
    }
    intercept[IllegalArgumentException] {
      ClientSecret(AuthPrincipalType.User)
    }
  }
}
