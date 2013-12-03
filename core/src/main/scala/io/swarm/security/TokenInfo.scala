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


import java.util.UUID
import org.joda.time.DateTime
import domain.{ResourceRepositoryComponent, ClientRepositoryComponent}
import io.swarm.security.TokenCategory._
import io.swarm.security.shiro._

/**
 * Created by Anil Chalil on 11/15/13.
 */
class TokenInfo private(val uuid: UUID, val `type`: TokenType.TokenType, val tokenCategory: TokenCategory, val created: DateTime, val accessed: DateTime, val inactive: Long, val duration: Long, val maxUsageCount: Long, val principal: AuthPrincipalInfo, val permissions: Set[String] = Set()) {
  val expiration = if (duration > 0) uuid.timeStampInMillis + duration else uuid.timeStampInMillis + tokenCategory.expiration

  def canEqual(other: Any): Boolean = other.isInstanceOf[TokenInfo]

  override def equals(other: Any): Boolean = other match {
    case that: TokenInfo =>
      (that canEqual this) &&
        uuid == that.uuid &&
        `type` == that.`type` &&
        tokenCategory == that.tokenCategory &&
        created == that.created &&
        accessed == that.accessed &&
        inactive == that.inactive &&
        duration == that.duration &&
        maxUsageCount == that.maxUsageCount &&
        principal == that.principal &&
        permissions == that.permissions
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(uuid, `type`, tokenCategory, created, accessed, inactive, duration, maxUsageCount, principal, permissions)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

trait TokenFactory {
  this: ResourceRepositoryComponent with ClientRepositoryComponent =>

  object TokenInfo {
    protected def getMaxTtl(tokenCategory: TokenCategory, principal: AuthPrincipalInfo): Long = {
      if (principal == null)
        TokenCategoryValue.longTokenAge
      else {
        val dbMetadata = if (principal.`type` == AuthPrincipalType.Database) resourceRepository.getDatabaseMetadata(principal.uuid) else clientRepository.getDevice(principal.uuid).flatMap(d => resourceRepository.getDatabaseMetadata(d.databaseRef.id))
        dbMetadata.map(meta => if (meta.oauthTTL == 0) TokenCategoryValue.longTokenAge else meta.oauthTTL).getOrElse(TokenCategoryValue.longTokenAge)
      }
    }

    def apply(tokenCategory: TokenCategory, tokenType: TokenType.TokenType, principal: AuthPrincipalInfo, duration: Long, maxUsageCount: Long, permissions: Set[String] = Set()) = {
      val maxTokenTtl = getMaxTtl(tokenCategory, principal)

      if (duration > maxTokenTtl) throw new IllegalArgumentException(s"Your token age cannot be more than the maxium age of $maxTokenTtl milliseconds")

      val uuid = UUIDGenerator.secretGenerator.generate()
      val creation = new DateTime(uuid.timestamp())
      val tokenInfo = new TokenInfo(uuid, if (tokenType == null) TokenType.Access else tokenType, tokenCategory, creation, creation, 0, if (duration == 0) maxTokenTtl else duration, maxUsageCount, principal, permissions)
      OauthBearerToken(tokenInfo)
    }
  }

}


trait TokenRepositoryComponent {
  this: ClientRepositoryComponent with ResourceRepositoryComponent =>
  val tokenRepository: TokenRepository

  trait TokenRepository {
    def getClientSecret(clientID: ClientID): ClientSecret

    def saveClientSecret(clientID: ClientID, secret: ClientSecret)

    def getTokenInfo(token: OauthBearerToken): TokenInfo

    def putTokenInfo(tokenInfo: TokenInfo)
  }
}