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

package io.swarm
package security
package shiro

import io.swarm.security.AuthPrincipalType._
import java.nio.ByteBuffer
import io.swarm.{UUIDGenerator, Config}

/**
 * Created by Anil Chalil on 11/19/13.
 */
class ClientSecret private(val secret: String, val authPrincipalType: AuthPrincipalType) {

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientSecret]

  override def equals(other: Any): Boolean = other match {
    case that: ClientSecret =>
      (that canEqual this) &&
        secret == that.secret &&
        authPrincipalType == that.authPrincipalType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(secret, authPrincipalType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
object ClientSecret {
  def apply(secret: String) = {
    try {
      if (secret.length < 4)
        throw NotClientSecretException("secret length could not be less than 4")
      val principal = AuthPrincipalType.fromBase64(secret.substring(0, AuthPrincipalTypeValue.base64prefixLength))
      if (principal == AuthPrincipalType.Admin || principal == AuthPrincipalType.DatabaseUser)
        throw NotClientSecretException(s"clientSecret principal type cannot be $principal")
      new ClientSecret(secret, principal)
    } catch {
      case e: MatchError => throw NotClientSecretException(e)
    }
  }

  def apply(principalType: AuthPrincipalType) = {
    if (principalType == AuthPrincipalType.Admin || principalType == AuthPrincipalType.DatabaseUser)
      throw new IllegalArgumentException(s"could not generate ClientSecret for $principalType")
    val bb = ByteBuffer.allocate(20)
    bb.put((System.currentTimeMillis() + Config.clientTokenSecretSalt + UUIDGenerator.secretGenerator.generate()).sha)
    new ClientSecret(principalType.base64Prefix + bb.base64URLSafeString, principalType)
  }
}
