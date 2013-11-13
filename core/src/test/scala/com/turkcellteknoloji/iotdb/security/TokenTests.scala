package com.turkcellteknoloji.iotdb
package security

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.turkcellteknoloji.iotdb.UUIDGenerator
import org.joda.time.DateTime
import java.nio.{BufferUnderflowException, BufferOverflowException}

/**
 * Created by capacman on 11/12/13.
 */
@RunWith(classOf[JUnitRunner])
class TokenTests extends FlatSpec with ShouldMatchers {

  "token " should " construct an OauthBearerToken" in {
    val tokenInfo = TokenInfo(UUIDGenerator.secretGenerator.generate(), TokenType.Access, DateTime.now(), DateTime.now(), DateTime.now(), 0, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.secretGenerator.generate()))
    val a = OauthBearerToken(tokenInfo, TokenCategory.Access, UUIDGenerator.secretGenerator.generate())
  }

  it should "throw NotTokenException" in {
    intercept[NotTokenException] {
      OauthBearerToken("invalid")
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
}
