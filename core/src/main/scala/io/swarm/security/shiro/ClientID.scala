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

import java.util.UUID
import io.swarm.security.AuthPrincipalType._
import java.nio.{ByteBuffer, BufferUnderflowException}
import io.swarm.domain.IDEntity
import io.swarm.management.Management.{Domain, DeviceRef, OrganizationRef}

/**
 * Created by Anil Chalil on 11/19/13.
 */
class ClientID private(val id: String, val principalID: UUID, val authPrincipalType: AuthPrincipalType) {

  def canEqual(other: Any): Boolean = other.isInstanceOf[ClientID]

  override def equals(other: Any): Boolean = other match {
    case that: ClientID =>
      (that canEqual this) &&
        id == that.id &&
        principalID == that.principalID &&
        authPrincipalType == that.authPrincipalType
    case _ => false
  }

  override def hashCode(): Int = {
    val state = Seq(id, principalID, authPrincipalType)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object ClientID {
  def apply(id: String) = {
    try {
      if (id.length < AuthPrincipalTypeValue.base64prefixLength)
        throw NotClientIDException(s"clientID length could not be less than ${AuthPrincipalTypeValue.base64prefixLength}")
      val principalType = AuthPrincipalType.fromBase64(id.substring(0, AuthPrincipalTypeValue.base64prefixLength))
      if (principalType == AuthPrincipalType.Admin || principalType == AuthPrincipalType.User)
        throw NotClientIDException(s"clientID principal type cannot be $principalType")
      val bb = ByteBuffer.wrap(id.substring(AuthPrincipalTypeValue.base64prefixLength).decodeBase64)
      new ClientID(id, new UUID(bb.getLong, bb.getLong), principalType)
    } catch {
      case e: MatchError => throw NotClientIDException(e)
      case e: BufferUnderflowException => throw NotClientIDException(e)
    }
  }

  def apply(ref: IDEntity) = ref match {
    case o: OrganizationRef => new ClientID(AuthPrincipalType.Organization.base64Prefix + o.id.base64, o.id, AuthPrincipalType.Organization)
    case dom: Domain => new ClientID(AuthPrincipalType.Domain.base64Prefix + dom.id.base64, dom.id, AuthPrincipalType.Domain)
    case d: DeviceRef => new ClientID(AuthPrincipalType.Device.base64Prefix + d.id.base64, d.id, AuthPrincipalType.Device)
    case _ => throw new IllegalArgumentException(s"could not generate clientID for $ref")
  }
}
