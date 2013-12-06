package io.swarm.security.persistence

import io.swarm.security.{TokenInfo, TokenRepositoryComponent}
import io.swarm.security.shiro.{OauthBearerToken, ClientSecret, ClientID}

/**
 * Created by Anil Chalil on 12/4/13.
 */
trait TokenRepositoryComponentCassandra extends TokenRepositoryComponent {
  val tokenRepository = new TokenRepository {

    def getTokenInfo(token: OauthBearerToken): TokenInfo = ???
    def putTokenInfo(tokenInfo: TokenInfo): Unit = ???

    def getClientSecret(clientID: ClientID): ClientSecret = ???

    def saveClientSecret(clientID: ClientID, secret: ClientSecret): Unit = ???
  }
}
