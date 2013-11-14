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

package com.turkcellteknoloji.iotdb
package security

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.turkcellteknoloji.iotdb.UUIDGenerator
import org.joda.time.DateTime
import java.nio.BufferUnderflowException
import com.turkcellteknoloji.iotdb.domain._
import java.lang.IllegalArgumentException
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.AdminUser
import com.turkcellteknoloji.iotdb.domain.Device

/**
 * Created by Anil Chalil on 11/12/13.
 */
@RunWith(classOf[JUnitRunner])
class TokenTests extends FlatSpec with ShouldMatchers {
  val tmpAdminUser = AdminUser(UUIDGenerator.secretGenerator.generate(), "anil", "halil", "user1", "user@user.com", activated = true, confirmed = true, disabled = false)
  val tmpDBUser = DatabaseUser(UUIDGenerator.secretGenerator.generate(), "anil", "halil", "user1", "user@user.com", activated = true, confirmed = true, disabled = false)
  "token " should " construct an OauthBearerToken" in {
    val tokenInfo = TokenInfo(UUIDGenerator.secretGenerator.generate(), TokenType.Access, DateTime.now(), DateTime.now(), DateTime.now(), 0, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.secretGenerator.generate()))
    val direct = OauthBearerToken(tokenInfo, TokenCategory.Access, UUIDGenerator.secretGenerator.generate())
    val fromStr = OauthBearerToken(direct.token)
    fromStr shouldBe direct
  }

  it should "throw NotTokenException" in {
    intercept[NotTokenException] {
      OauthBearerToken("invalid")
    }
  }

  it should "throw ExpiredTokenException" in {
    intercept[ExpiredTokenException]{
      val tokenInfo = TokenInfo(UUIDGenerator.secretGenerator.generate(), TokenType.Access, DateTime.now(), DateTime.now(), DateTime.now(), 1, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.secretGenerator.generate()))
      val token = OauthBearerToken(tokenInfo, TokenCategory.Access, UUIDGenerator.secretGenerator.generate())
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
      val tokenInfo = TokenInfo(UUIDGenerator.secretGenerator.generate(), TokenType.Access, DateTime.now(), DateTime.now(), DateTime.now(), 0, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.secretGenerator.generate()))
      val a = OauthBearerToken(tokenInfo, TokenCategory.Access, UUIDGenerator.secretGenerator.generate())
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
    val org = ClientID(OrganizationInfo(UUIDGenerator.secretGenerator.generate(), "org"))
    val db = ClientID(DatabaseInfo(UUIDGenerator.secretGenerator.generate(), "db"))
    val dev = ClientID(Device(UUIDGenerator.secretGenerator.generate(), "device"))

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
      ClientID(AuthPrincipalType.Database.base64Prefix + "test")
    }.getCause
    val cause2 = intercept[NotClientIDException] {
      ClientID(AuthPrincipalType.Database.base64Prefix)
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
      ClientID(AuthPrincipalType.DatabaseUser.base64Prefix)
    }
  }


  "client secret " should "construct a ClientSecret" in {
    val orgSecret = ClientSecret(AuthPrincipalType.Organization)
    val dbSecret = ClientSecret(AuthPrincipalType.Database)
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
      ClientSecret(AuthPrincipalType.DatabaseUser.base64Prefix)
    }
  }

  it should " throw IllegalArgumentException" in {
    intercept[IllegalArgumentException] {
      ClientSecret(AuthPrincipalType.Admin)
    }
    intercept[IllegalArgumentException] {
      ClientSecret(AuthPrincipalType.DatabaseUser)
    }
  }
}
