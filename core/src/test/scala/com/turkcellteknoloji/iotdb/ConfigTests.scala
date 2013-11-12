package com.turkcellteknoloji.iotdb

import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * Created by capacman on 11/12/13.
 */
@RunWith(classOf[JUnitRunner])
class ConfigTests extends FlatSpec with ShouldMatchers {

  "config " should "provide tokensecret " in {
    Config.tokenSecretSalt === "mytokenSecret"
  }

  it should "provide client secret" in {
    Config.clientTokenSecretSalt === "myclientSecret"
  }
}
