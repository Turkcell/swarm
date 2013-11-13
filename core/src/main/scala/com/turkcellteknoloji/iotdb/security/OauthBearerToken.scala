package com.turkcellteknoloji.iotdb
package security

import org.apache.shiro.authc.AuthenticationToken
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.{AuthPrincipalTypeValue, AuthPrincipalType}
import java.util.UUID
import org.joda.time.DateTime
import java.nio.{BufferUnderflowException, ByteBuffer}
import com.turkcellteknoloji.iotdb.Config
import com.turkcellteknoloji.iotdb.security.TokenCategory.TokenCategory
import com.turkcellteknoloji.iotdb.domain.{DeviceRef, DatabaseRef, OrganizationRef, EntityRef}

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

  def principalID: UUID
}

class OauthBearerToken private(val token: String, val authPrincipalType: AuthPrincipalType, val tokenID: UUID, val principalID: UUID) extends PrincipalAuthenticationToken {
  def getPrincipal = token

  def getCredentials = token

  def canEqual(other: Any): Boolean = other.isInstanceOf[OauthBearerToken]

  override def equals(other: Any): Boolean = other match {
    case that: OauthBearerToken =>
      (that canEqual this) &&
        token == that.token &&
        authPrincipalType == that.authPrincipalType &&
        tokenID == that.tokenID &&
        principalID == that.principalID
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(token, authPrincipalType, tokenID, principalID)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class ClientID private(val id: String, val principalID: UUID, val authPrincipalType: AuthPrincipalType) {

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientID]

  override def equals(other: Any): Boolean = other match {
    case that: ClientID =>
      (that canEqual this) &&
        id == that.id &&
        principalID == that.principalID &&
        authPrincipalType == that.authPrincipalType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(id, principalID, authPrincipalType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

class ClientSecret private(val secret: String, val authPrincipalType: AuthPrincipalType) {

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientSecret]

  override def equals(other: Any): Boolean = other match {
    case that: ClientSecret =>
      (that canEqual this) &&
        secret == that.secret &&
        authPrincipalType == that.authPrincipalType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(secret, authPrincipalType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object ClientSecret {
  def apply(secret: String) = {
    try {
      new ClientSecret(secret, AuthPrincipalType.fromBase64(secret.substring(0, AuthPrincipalTypeValue.base64prefixLength)))
    } catch {
      case e: MatchError => throw NotClientSecretException(e)
    }
  }
}

object ClientID {
  def apply(id: String) = {
    try {
      val principalType = AuthPrincipalType.fromBase64(id.substring(0, AuthPrincipalTypeValue.base64prefixLength))
      val bb = ByteBuffer.wrap(id.substring(AuthPrincipalTypeValue.base64prefixLength).decodeBase64)
      new ClientID(id, new UUID(bb.getLong, bb.getLong), principalType)
    } catch {
      case e: MatchError => throw NotClientIDException(e)
      case e: BufferUnderflowException => throw NotClientIDException(e)
    }
  }

  def apply(ref: EntityRef) = ref match {
    case o: OrganizationRef => new ClientID(AuthPrincipalType.Organization.base64Prefix + o.id.base64, o.id, AuthPrincipalType.Organization)
    case d: DatabaseRef => new ClientID(AuthPrincipalType.Database.base64Prefix + d.id.base64, d.id, AuthPrincipalType.Database)
    case d: DeviceRef => new ClientID(AuthPrincipalType.Device.base64Prefix + d.id.base64, d.id, AuthPrincipalType.Device)
  }
}

class ClientIDSecretToken private(private val clienID: ClientID, private val clientSecret: String) extends PrincipalAuthenticationToken {
  def getPrincipal = clienID

  def getCredentials = clientSecret

  def authPrincipalType: AuthPrincipalType = clienID.authPrincipalType

  def principalID: UUID = clienID.principalID

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientIDSecretToken]

  override def equals(other: Any): Boolean = other match {
    case that: ClientIDSecretToken =>
      (that canEqual this) &&
        clienID == that.clienID &&
        clientSecret == that.clientSecret
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(clienID, clientSecret)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
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
    new OauthBearerToken(tokenCategory.base64Prefix + tokenInfo.principal.`type`.base64Prefix + bytes.base64URLSafeString, tokenInfo.principal.`type`, uuid, tokenInfo.principal.uuid)
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
        new OauthBearerToken(token, principleType, tokenUUID, principalID)
      } else throw BadTokenException("Invalid Token Signature")
    } catch {
      case e: MatchError => throw NotTokenException(e)
      case e: java.nio.BufferUnderflowException => throw NotTokenException(e)
    }
  }
}

object ClientIDSecretToken {

}