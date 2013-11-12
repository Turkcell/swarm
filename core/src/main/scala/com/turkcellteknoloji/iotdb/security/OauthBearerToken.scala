package com.turkcellteknoloji.iotdb.security

import org.apache.shiro.authc.AuthenticationToken
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain._
import java.util.UUID
import org.joda.time.DateTime
import com.turkcellteknoloji.iotdb.security.TokenCategory.TokenCategory
import java.nio.ByteBuffer
import com.turkcellteknoloji.iotdb.{Config, UUIDGenerator}

/**
 * Created by capacman on 11/11/13.
 */

case class AuthPrincipalInfo( `type`: AuthPrincipalType,  uuid: UUID)

object TokenType extends Enumeration {
  type TokenType = Value
  val Access, Activate, ResetPW, Confirm = Value
}

case class TokenInfo( uuid: UUID,  `type`: TokenType.TokenType, created: DateTime, accessed: DateTime, inactive: DateTime, duration: Long, principal: AuthPrincipalInfo)

object TokenCategory extends Enumeration {
  type TokenCategory = Value
  val Access, Refresh, Offline, Email = Value

  def fromBase64(s: String) = s match {
    case TokenCategoryValue.accBase64 => Access
    case TokenCategoryValue.reBase64 => Refresh
    case TokenCategoryValue.offBase64 => Offline
    case TokenCategoryValue.emailBase64 => Email
  }

  def fromPrefix(s: String) = s match {
    case "ac" => Access
    case "re" => Refresh
    case "of" => Offline
    case "em" => Email
  }

  class TokenCategoryValue(val tokenCategory: Value) {
    def prefix = tokenCategory match {
      case Access => "ac"
      case Refresh => "re"
      case Offline => "of"
      case Email => "em"
    }

    def base64Prefix = tokenCategory match {
      case Access => TokenCategoryValue.accBase64
      case Refresh => TokenCategoryValue.reBase64
      case Offline => TokenCategoryValue.offBase64
      case Email => TokenCategoryValue.emailBase64
    }

    def expires = tokenCategory match {
      case Access => true
      case Refresh => false
      case Offline => false
      case Email => false
    }

    def expiration = tokenCategory match {
      case Access => TokenCategoryValue.longTokenAge
      case Refresh => TokenCategoryValue.longTokenAge
      case Offline => TokenCategoryValue.longTokenAge
      case Email => TokenCategoryValue.longTokenAge
      case _ => TokenCategoryValue.shortTokenAge
    }
  }

  implicit def value2TokenCategoryValue(ap: Value) = new TokenCategoryValue(ap)

  object TokenCategoryValue {
    val accBase64 = ("-" + Access.prefix).base64
    val reBase64 = ("-" + Refresh.prefix).base64
    val offBase64 = ("-" + Offline.prefix).base64
    val emailBase64 = ("-" + Email.prefix).base64
    val prefixLength = 2
    val base64prefixLength = 4
    val longTokenAge = 7 * 24 * 60 * 60 * 1000
    val shortTokenAge = 24 * 60 * 60 * 1000
  }

}

trait IotDbAuthenticationToken extends AuthenticationToken {
  def authPrincipalType: AuthPrincipalType
}

trait IotDbBearerAuthenticationToken extends IotDbAuthenticationToken {
  def token: String
}

class OauthBearerToken(val token: String, val authPrincipalType: AuthPrincipalType) extends IotDbBearerAuthenticationToken {
  def getPrincipal = token

  def getCredentials = token
}

object OauthBearerToken {

  private[this] def sha(tokenCategory: TokenCategory.TokenCategory, principalType: AuthPrincipalType, expires: Long, uuid: UUID, principalUUID: UUID) = (tokenCategory.prefix + principalType.prefix + uuid + Config.tokenSecretSalt + expires + principalUUID).sha

  def apply(tokenInfo: TokenInfo, tokenCategory: TokenCategory, uuid: UUID) = {
    var l = 52
    if (tokenCategory.expires) {
      l += 8;
    }
    val bytes = ByteBuffer.allocate(l)
    bytes.put(uuid.asByteArray)
    var expires = Long.MaxValue
    if (tokenCategory.expires) {
      expires = if (tokenInfo.duration > 0) uuid.timeStampInMillis + (tokenInfo.duration) else uuid.timeStampInMillis + tokenCategory.expiration
      bytes.putLong(expires);
    }
    bytes.put(tokenInfo.principal.uuid.asByteArray)
    bytes.put(sha(tokenCategory, tokenInfo.principal.`type`, expires, uuid, tokenInfo.principal.uuid));
    new OauthBearerToken(tokenCategory.base64Prefix + tokenInfo.principal.`type`.base64Prefix + bytes.base64URLSafeString, tokenInfo.principal.`type`)
  }

  def apply(token: String) = {
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
    } else throw new IllegalArgumentException("Invalid Token Signature")
  }
}
