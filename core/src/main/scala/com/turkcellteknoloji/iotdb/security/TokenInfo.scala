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

package com.turkcellteknoloji.iotdb.security

import java.util.UUID
import org.joda.time.DateTime
import com.turkcellteknoloji.iotdb.security.TokenCategory._
import com.turkcellteknoloji.iotdb.UUIDGenerator
import com.turkcellteknoloji.iotdb.domain.{ ResourceRepositoryComponent, ClientRepositoryComponent }

/**
 * Created by Anil Chalil on 11/15/13.
 */
case class TokenInfo(uuid: UUID, `type`: TokenType.TokenType, created: DateTime, accessed: DateTime, inactive: Long, duration: Long, principal: AuthPrincipalInfo, state: Map[String, AnyRef] = Map())

trait TokenRepositoryComponent {
  this: ClientRepositoryComponent with ResourceRepositoryComponent =>
  val tokenRepository: TokenRepository

  trait TokenRepository {
    def getClientSecret(clientID: ClientID): ClientSecret

    def getTokenInfo(token: OauthBearerToken): TokenInfo

    protected def putTokenInfo(tokenInfo: TokenInfo)

    protected def getMaxTtl(tokenCategory: TokenCategory, principal: AuthPrincipalInfo): Long = {
      if (principal == null)
        TokenCategoryValue.longTokenAge
      else {
        val dbMetadata = if (principal.`type` == AuthPrincipalType.Database) resourceRepository.getDatabaseMetadata(principal.uuid) else clientRepository.getDevice(principal.uuid).flatMap(d => resourceRepository.getDatabaseMetadata(d.databaseInfo.id))
        dbMetadata.map(meta => if (meta.oauthTTL == 0) TokenCategoryValue.longTokenAge else meta.oauthTTL).getOrElse(TokenCategoryValue.longTokenAge)
      }
    }

    def createOauthToken(tokenCategory: TokenCategory, tokenType: TokenType.TokenType, principal: AuthPrincipalInfo, duration: Long, creationTimestamp: Long, state: Map[String, AnyRef]) = {
      val maxTokenTtl = getMaxTtl(tokenCategory, principal);

      if (duration > maxTokenTtl) throw new IllegalArgumentException(s"Your token age cannot be more than the maxium age of $maxTokenTtl milliseconds");

      val uuid = UUIDGenerator.secretGenerator.generate()
      val creation = new DateTime(uuid.timestamp())
      val tokenInfo = new TokenInfo(uuid, if (tokenType == null) TokenType.Access else tokenType, creation, creation, 0, if (duration == 0) maxTokenTtl else duration, principal, state)
      putTokenInfo(tokenInfo);
      OauthBearerToken(tokenInfo, tokenCategory, uuid)
    }
  }

}