package com.turkcellteknoloji.iotdb.security.tokens

import com.turkcellteknoloji.iotdb.security._
import com.turkcellteknoloji.iotdb.security.TokenCategory.TokenCategory
import java.util.UUID
import com.turkcellteknoloji.iotdb.security.AuthPrincipalInfo
import com.turkcellteknoloji.iotdb.security.TokenInfo

trait TokenServiceComponent {
  val tokenService: TokenService

  trait TokenService {
    def getClientSecret(clientID: ClientID): ClientSecret

    def getTokenInfo(token: OauthBearerToken): TokenInfo

    def createToken(tokenCategory: TokenCategory, `type`: String,
                    principal: AuthPrincipalInfo, state: Map[String, Object], duration: Long)
  }

}
