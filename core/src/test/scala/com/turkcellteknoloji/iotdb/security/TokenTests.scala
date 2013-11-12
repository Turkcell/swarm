package com.turkcellteknoloji.iotdb.security

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.ShouldMatchers
import com.turkcellteknoloji.iotdb.{UUIDGenerator, Config}
import org.joda.time.DateTime
import com.turkcellteknoloji.iotdb.domain.{UUID2RichUUID, String2RichString}

/**
 * Created by capacman on 11/12/13.
 */
@RunWith(classOf[JUnitRunner])
class TokenTests extends FlatSpec with ShouldMatchers {

  "Object " should " construct an oath token" in {
    val tokenInfo = TokenInfo(UUIDGenerator.secretGenerator.generate(), TokenType.Access, DateTime.now(), DateTime.now(), DateTime.now(), 0, AuthPrincipalInfo(AuthPrincipalType.Admin, UUIDGenerator.secretGenerator.generate()))
    val a = OauthBearerToken(tokenInfo, TokenCategory.Access, UUIDGenerator.secretGenerator.generate())
    println(a.token)
  }
}
