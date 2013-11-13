package com.turkcellteknoloji.iotdb
package security

import org.apache.shiro.authc.AuthenticationToken
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType
import java.util.UUID
import org.joda.time.DateTime
import java.nio.ByteBuffer
import com.turkcellteknoloji.iotdb.Config
import com.turkcellteknoloji.iotdb.security.TokenCategory.TokenCategory

/**
 * Created by capacman on 11/11/13.
 */

case class AuthPrincipalInfo(`type`: AuthPrincipalType, uuid: UUID)

object TokenType extends Enumeration {
  type TokenType = Value
  val Access, Activate, ResetPW, Confirm = Value
}

case class TokenInfo(uuid: UUID, `type`: TokenType.TokenType, created: DateTime, accessed: DateTime, inactive: DateTime, duration: Long, principal: AuthPrincipalInfo)


trait PrincipalAuthenticationToken extends AuthenticationToken {
  def authPrincipalType: AuthPrincipalType
}

class OauthBearerToken private(val token: String, val authPrincipalType: AuthPrincipalType) extends PrincipalAuthenticationToken {
  def getPrincipal = token

  def getCredentials = token
}

class ClientID private(val id: String, val uuid: UUID, val authPrincipalType: AuthPrincipalType)

object ClientID {
  def apply(id: String) = {
    id.substring(0, 4)
  }
}

class ClientIDSecretToken private(clienID: String, clientSecret: String, val authPrincipalType: AuthPrincipalType) extends PrincipalAuthenticationToken {
  def getPrincipal = clienID

  def getCredentials = clientSecret
}

object OauthBearerToken {

  private[this] def sha(tokenCategory: TokenCategory, principalType: AuthPrincipalType, expires: Long, uuid: UUID, principalUUID: UUID) = (tokenCategory.prefix + principalType.prefix + uuid + Config.tokenSecretSalt + expires + principalUUID).sha

  def apply(tokenInfo: TokenInfo, tokenCategory: TokenCategory, uuid: UUID) = {
    var l = 52
    if (tokenCategory.expires) {
      l += 8
    }
    val bytes = ByteBuffer.allocate(l)
    bytes.put(uuid.asByteArray)
    var expires = Long.MaxValue
    if (tokenCategory.expires) {
      expires = if (tokenInfo.duration > 0) uuid.timeStampInMillis + tokenInfo.duration else uuid.timeStampInMillis + tokenCategory.expiration
      bytes.putLong(expires)
    }
    bytes.put(tokenInfo.principal.uuid.asByteArray)
    bytes.put(sha(tokenCategory, tokenInfo.principal.`type`, expires, uuid, tokenInfo.principal.uuid))
    new OauthBearerToken(tokenCategory.base64Prefix + tokenInfo.principal.`type`.base64Prefix + bytes.base64URLSafeString, tokenInfo.principal.`type`)
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
      val principalID = new UUID(bb.getLong, bb.getLong)
      val shaExpected = sha(category, principleType, expires, tokenUUID, principalID)
      val shaActual = new Array[Byte](20)
      bb.get(shaActual)
      if (shaExpected.sameElements(shaActual)) {
        new OauthBearerToken(token, principleType)
        //TODO change exception type
      } else throw BadTokenException("Invalid Token Signature")
    } catch {
      case e: MatchError => throw NotTokenException(e)
      case e: java.nio.BufferUnderflowException => throw NotTokenException(e)
    }
  }
}

object ClientIDSecretToken {

}