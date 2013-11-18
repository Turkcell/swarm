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

import org.apache.shiro.authc.AuthenticationToken
import AuthPrincipalType.{AuthPrincipalTypeValue, AuthPrincipalType}
import java.util.UUID
import java.nio.{BufferUnderflowException, ByteBuffer}
import TokenCategory.TokenCategory
import domain._
import org.joda.time.DateTime

/**
 * Created by Anil Chalil
 */

case class AuthPrincipalInfo(`type`: AuthPrincipalType, uuid: UUID)

object TokenType extends Enumeration {
  type TokenType = Value
  val Access, Activate, ResetPW, Confirm = Value
}


trait PrincipalAuthenticationToken extends AuthenticationToken {
  def authPrincipalType: AuthPrincipalType

  def principalID: UUID
}

case class UsernamePasswordToken(username: String, password: String, principalType: AuthPrincipalType) extends AuthenticationToken {
  def getPrincipal = username

  def getCredentials = password
}

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
      if (secret.length < 4)
        throw NotClientSecretException("secret length could not be less than 4")
      val principal = AuthPrincipalType.fromBase64(secret.substring(0, AuthPrincipalTypeValue.base64prefixLength))
      if (principal == AuthPrincipalType.Admin || principal == AuthPrincipalType.DatabaseUser)
        throw NotClientSecretException(s"clientSecret principal type cannot be $principal")
      new ClientSecret(secret, principal)
    } catch {
      case e: MatchError => throw NotClientSecretException(e)
    }
  }

  def apply(principalType: AuthPrincipalType) = {
    if (principalType == AuthPrincipalType.Admin || principalType == AuthPrincipalType.DatabaseUser)
      throw new IllegalArgumentException(s"could not generate ClientSecret for $principalType")
    val bb = ByteBuffer.allocate(20)
    bb.put((System.currentTimeMillis() + Config.clientTokenSecretSalt + UUIDGenerator.secretGenerator.generate()).sha)
    new ClientSecret(principalType.base64Prefix + bb.base64URLSafeString, principalType)
  }
}

object ClientID {
  def apply(id: String) = {
    try {
      if (id.length < AuthPrincipalTypeValue.base64prefixLength)
        throw NotClientIDException(s"clientID length could not be less than ${AuthPrincipalTypeValue.base64prefixLength}")
      val principalType = AuthPrincipalType.fromBase64(id.substring(0, AuthPrincipalTypeValue.base64prefixLength))
      if (principalType == AuthPrincipalType.Admin || principalType == AuthPrincipalType.DatabaseUser)
        throw NotClientIDException(s"clientID principal type cannot be $principalType")
      val bb = ByteBuffer.wrap(id.substring(AuthPrincipalTypeValue.base64prefixLength).decodeBase64)
      new ClientID(id, new UUID(bb.getLong, bb.getLong), principalType)
    } catch {
      case e: MatchError => throw NotClientIDException(e)
      case e: BufferUnderflowException => throw NotClientIDException(e)
    }
  }

  def apply(ref: IDEntity) = ref match {
    case o: OrganizationRef => new ClientID(AuthPrincipalType.Organization.base64Prefix + o.id.base64, o.id, AuthPrincipalType.Organization)
    case d: DatabaseRef => new ClientID(AuthPrincipalType.Database.base64Prefix + d.id.base64, d.id, AuthPrincipalType.Database)
    case d: Device => new ClientID(AuthPrincipalType.Device.base64Prefix + d.id.base64, d.id, AuthPrincipalType.Device)
    case _ => throw new IllegalArgumentException(s"could not generate clientID for $ref")
  }
}

class ClientIDSecretToken private(private val clientID: ClientID, private val clientSecret: ClientSecret) extends PrincipalAuthenticationToken {
  def getPrincipal = clientID

  def getCredentials = clientSecret

  def authPrincipalType: AuthPrincipalType = clientID.authPrincipalType

  def principalID: UUID = clientID.principalID

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientIDSecretToken]

  override def equals(other: Any): Boolean = other match {
    case that: ClientIDSecretToken =>
      (that canEqual this) &&
        clientID == that.clientID &&
        clientSecret == that.clientSecret
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(clientID, clientSecret)
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
    bytes.put(sha(tokenInfo.tokenCategory, tokenInfo.principal.`type`, expires,tokenInfo.uuid, tokenInfo.principal.uuid))
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

object ClientIDSecretToken {
  def apply(id: String, secret: String) = {
    val clientID = ClientID(id)
    val clientSecret = ClientSecret(secret)
    checkPrincipals(clientID, clientSecret)
    new ClientIDSecretToken(clientID, clientSecret)
  }

  def apply(id: ClientID, secret: ClientSecret) = {
    checkPrincipals(id, secret)
    new ClientIDSecretToken(id, secret)
  }

  def checkPrincipals(clientID: ClientID, clientSecret: ClientSecret) {
    if (clientID.authPrincipalType != clientSecret.authPrincipalType) throw InvalidClientIDSecretTokenException("clientID and clientSecret principalType should be same!")
  }
}