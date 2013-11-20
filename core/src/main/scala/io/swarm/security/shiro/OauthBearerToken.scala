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

package io.swarm
package security.shiro

import io.swarm.security.AuthPrincipalType._
import java.util.UUID
import io.swarm.Config
import java.nio.ByteBuffer
import io.swarm.security._
import io.swarm.security.AuthPrincipalType.AuthPrincipalType
import io.swarm.security.AuthPrincipalType
import io.swarm.security.TokenCategory.TokenCategory
import io.swarm.security.TokenCategory
import io.swarm.security.TokenInfo

/**
 * Created by Anil Chalil on 11/19/13.
 */
class OauthBearerToken private(val token: String, val authPrincipalType: AuthPrincipalType, val category: TokenCategory, val tokenID: UUID, val principalID: UUID, val expires: Long) extends PrincipalAuthenticationToken {
  def getPrincipal = token

  def getCredentials = token

  def canEqual(other: Any): Boolean = other.isInstanceOf[OauthBearerToken]

  override def equals(other: Any): Boolean = other match {
    case that: OauthBearerToken =>
      (that canEqual this) &&
        token == that.token &&
        authPrincipalType == that.authPrincipalType &&
        category == that.category &&
        tokenID == that.tokenID &&
        principalID == that.principalID
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(token, authPrincipalType, category, tokenID, principalID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object OauthBearerToken {

  private[this] def sha(tokenCategory: TokenCategory, principalType: AuthPrincipalType, expires: Long, uuid: UUID, principalUUID: UUID) = (tokenCategory.prefix + principalType.prefix + uuid + Config.tokenSecretSalt + expires + principalUUID).sha

  def apply(tokenInfo: TokenInfo) = {
    var l = 52
    if (tokenInfo.tokenCategory.expires) {
      l += 8
    }
    val bytes = ByteBuffer.allocate(l)
    bytes.put(tokenInfo.uuid.asByteArray)
    var expires = Long.MaxValue
    if (tokenInfo.tokenCategory.expires) {
      expires = tokenInfo.expiration
      bytes.putLong(expires)
    }
    bytes.put(tokenInfo.principal.uuid.asByteArray)
    bytes.put(sha(tokenInfo.tokenCategory, tokenInfo.principal.`type`, expires, tokenInfo.uuid, tokenInfo.principal.uuid))
    new OauthBearerToken(tokenInfo.tokenCategory.base64Prefix + tokenInfo.principal.`type`.base64Prefix + bytes.base64URLSafeString, tokenInfo.principal.`type`, tokenInfo.tokenCategory, tokenInfo.uuid, tokenInfo.principal.uuid, expires)
  }

  def apply(token: String) = {
    try {
      if (token.length < 8)
        throw NotTokenException(s"$token is not a token")
      val category = TokenCategory.fromBase64(token.substring(0, TokenCategory.TokenCategoryValue.base64prefixLength))
      val principleType = AuthPrincipalType.fromBase64(token.substring(TokenCategory.TokenCategoryValue.base64prefixLength, TokenCategory.TokenCategoryValue.base64prefixLength + AuthPrincipalType.AuthPrincipalTypeValue.base64prefixLength))
      val bytes = token.substring(TokenCategory.TokenCategoryValue.base64prefixLength + AuthPrincipalType.AuthPrincipalTypeValue.base64prefixLength).decodeBase64
      val bb = ByteBuffer.wrap(bytes)
      val tokenUUID = new UUID(bb.getLong, bb.getLong)
      val expires = bb.getLong
      val delta = System.currentTimeMillis() - expires
      if (delta > 0)
        throw ExpiredTokenException(delta)
      val principalID = new UUID(bb.getLong, bb.getLong)
      val shaExpected = sha(category, principleType, expires, tokenUUID, principalID)
      val shaActual = new Array[Byte](20)
      bb.get(shaActual)
      if (shaExpected.sameElements(shaActual)) {
        new OauthBearerToken(token, principleType, category, tokenUUID, principalID, expires)
      } else throw BadTokenException("Invalid Token Signature")
    } catch {
      case e: MatchError => throw NotTokenException(e)
      case e: java.nio.BufferUnderflowException => throw NotTokenException(e)
    }
  }
}
