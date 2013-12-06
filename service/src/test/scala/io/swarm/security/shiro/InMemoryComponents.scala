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

package io.swarm.security.shiro

import java.util.UUID
import scala.collection.mutable.Map
import io.swarm.security.TokenRepositoryComponent
import io.swarm.security.TokenInfo

trait InMemoryComponents extends TokenRepositoryComponent {
  val tokenRepository = new TokenRepository {
    val tokenStore = Map[UUID, TokenInfo]()
    val secretStore = Map[UUID, String]()

    def getTokenInfo(token: OauthBearerToken): TokenInfo = this.synchronized(tokenStore(token.tokenID))

    def getClientSecret(clientID: ClientID): ClientSecret = this.synchronized(ClientSecret(secretStore(clientID.principalID)))

    def putTokenInfo(tokenInfo: TokenInfo) = this.synchronized {
      tokenStore += (tokenInfo.uuid -> tokenInfo)
    }

    def saveClientSecret(clientID: ClientID, secret: ClientSecret) = this.synchronized {
      secretStore += (clientID.principalID -> secret.secret)
    }
  }
}