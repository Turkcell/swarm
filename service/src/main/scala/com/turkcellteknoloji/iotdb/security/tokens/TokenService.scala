package com.turkcellteknoloji.iotdb.security.tokens

import java.util.UUID
import org.joda.time.DateTime
import com.turkcellteknoloji.iotdb.security.AuthPrincipalInfo

/**
 * Created by capacman on 11/1/13.
 */


case class TokenInfo(val uuid: UUID, val `type`: String, created: DateTime, accessed: DateTime, inactive: DateTime, duration: Long, principal: AuthPrincipalInfo)

trait TokenService {
  def getTokenInfo(token: String): TokenInfo

}
