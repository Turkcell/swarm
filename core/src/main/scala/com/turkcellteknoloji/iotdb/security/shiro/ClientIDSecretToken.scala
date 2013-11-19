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

package com.turkcellteknoloji.iotdb.security.shiro

import com.turkcellteknoloji.iotdb.security.AuthPrincipalType._
import java.util.UUID
import com.turkcellteknoloji.iotdb.security.InvalidClientIDSecretTokenException

/**
 * Created by Anil Chalil on 11/19/13.
 */
class ClientIDSecretToken private(private val clientID: ClientID, private val clientSecret: ClientSecret) extends PrincipalAuthenticationToken {
  def getPrincipal = clientID

  def getCredentials = clientSecret

  def authPrincipalType: AuthPrincipalType = clientID.authPrincipalType

  def principalID: UUID = clientID.principalID

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientIDSecretToken]

  override def equals(other: Any): Boolean = other match {
    case that: ClientIDSecretToken =>
      (that canEqual this) &&
        clientID == that.clientID &&
        clientSecret == that.clientSecret
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(clientID, clientSecret)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
object ClientIDSecretToken {
  def apply(id: String, secret: String) = {
    val clientID = ClientID(id)
    val clientSecret = ClientSecret(secret)
    checkPrincipals(clientID, clientSecret)
    new ClientIDSecretToken(clientID, clientSecret)
  }

  def apply(id: ClientID, secret: ClientSecret) = {
    checkPrincipals(id, secret)
    new ClientIDSecretToken(id, secret)
  }

  def checkPrincipals(clientID: ClientID, clientSecret: ClientSecret) {
    if (clientID.authPrincipalType != clientSecret.authPrincipalType) throw InvalidClientIDSecretTokenException("clientID and clientSecret principalType should be same!")
  }
}
