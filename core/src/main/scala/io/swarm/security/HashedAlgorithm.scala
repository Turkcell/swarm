package io.swarm.security

import org.apache.shiro.crypto.hash.Sha1Hash
import io.swarm.Config

/**
 * Created by Anil Chalil on 11/28/13.
 */
object HashedAlgorithm {
  //for sha1
  val currentSHALength = 160 / 8
  val currentOauthTokenBaseLength = 16 + 16 + 20
  val algorithmName = Sha1Hash.ALGORITHM_NAME

  def toHex(value: String) = new Sha1Hash(value, Config.userInfoHash).toHex
}
