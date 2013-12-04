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
import io.swarm.security.TokenCategory._
import io.swarm.security.shiro._
import com.github.nscala_time.time.Imports._

/**
 * Created by Anil Chalil on 11/15/13.
 */
class TokenInfo private(val uuid: UUID, val `type`: TokenType.TokenType, val tokenCategory: TokenCategory, val created: DateTime, val accessed: DateTime, val inactive: Duration, val duration: Duration, val usageCount: Long, val maxUsageCount: Long, val principal: AuthPrincipalInfo, val permissions: Set[String] = Set()) {
  val expiration = {
    val uuidDate = uuid.timeStampInMillis.toDateTime
    if (duration > 0.toDuration) uuidDate + duration else uuidDate + tokenCategory.expiration
  }

  def isExpired = DateTime.now() > expiration || (maxUsageCount != 0 && usageCount > maxUsageCount)

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
        usageCount == that.usageCount &&
        maxUsageCount == that.maxUsageCount &&
        principal == that.principal &&
        permissions == that.permissions
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(uuid, `type`, tokenCategory, created, accessed, inactive, duration, usageCount, maxUsageCount, principal, permissions)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}


object TokenInfo {

  def apply(tokenCategory: TokenCategory, tokenType: TokenType.TokenType, principal: AuthPrincipalInfo, duration: Duration, maxUsageCount: Long, permissions: Set[String] = Set()) = {
    if (duration > tokenCategory.expiration) throw new IllegalArgumentException(s"Your token age cannot be more than the maxium age of ${tokenCategory.expiration} milliseconds")

    val uuid = UUIDGenerator.secretGenerator.generate()
    val creation = new DateTime(uuid.timeStampInMillis)
    new TokenInfo(uuid, if (tokenType == null) TokenType.Access else tokenType, tokenCategory, creation, creation, 0.toDuration, if (duration.getMillis > 0) duration else tokenCategory.expiration, 0, maxUsageCount, principal, permissions)
  }
}


trait TokenRepositoryComponent {
  val tokenRepository: TokenRepository

  trait TokenRepository {
    def getClientSecret(clientID: ClientID): ClientSecret

    def saveClientSecret(clientID: ClientID, secret: ClientSecret)

    def getTokenInfo(token: OauthBearerToken): TokenInfo

    def putTokenInfo(tokenInfo: TokenInfo)
  }

}