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

/**
 * Created by Anil Chalil on 11/15/13.
 */
case class TokenInfo(uuid: UUID, `type`: TokenType.TokenType, created: DateTime, accessed: DateTime, inactive: DateTime, duration: Long, principal: AuthPrincipalInfo)

trait TokenRepository {
  def getClientSecret(clientID: ClientID): ClientSecret

  def getTokenInfo(token: OauthBearerToken): TokenInfo

  def createToken(tokenCategory: TokenCategory, `type`: String,
                  principal: AuthPrincipalInfo, state: Map[String, Object], duration: Long)
}

trait TokenRepositoryComponent {
  val tokenRepository: TokenRepository
}