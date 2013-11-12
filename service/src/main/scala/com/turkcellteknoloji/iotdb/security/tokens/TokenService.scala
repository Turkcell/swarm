package com.turkcellteknoloji.iotdb.security.tokens

import com.turkcellteknoloji.iotdb.security.{OauthBearerToken, TokenInfo, AuthPrincipalInfo}
import com.turkcellteknoloji.iotdb.security.TokenCategory.TokenCategory


trait TokenService {
  def getTokenInfo(token: OauthBearerToken): TokenInfo

  def createToken(tokenCategory: TokenCategory, `type`: String,
                  principal: AuthPrincipalInfo, state: Map[String, Object], duration: Long)
}
