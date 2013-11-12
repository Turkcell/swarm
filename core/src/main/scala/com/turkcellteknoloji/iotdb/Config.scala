package com.turkcellteknoloji.iotdb

import com.typesafe.config.ConfigFactory

/**
 * Created by capacman on 11/11/13.
 */
object Config {
  val conf = ConfigFactory.load
  val clientTokenSecretSalt = conf.getString("oauth.clientSecretSalt")
  val tokenSecretSalt = conf.getString("oauth.tokenSecretSalt")

}
