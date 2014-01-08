/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.swarm
package security

import java.nio.ByteBuffer
import io.swarm.{UUIDGenerator, Config}


/**
 * @author Anil Chalil
 */
object AuthPrincipalType extends Enumeration {
  type AuthPrincipalType = Value
  val Organization, Admin, Domain, Device, User = Value


  def fromBase64(s: String) = s match {
    case AuthPrincipalTypeValue.orgBase64 => Organization
    case AuthPrincipalTypeValue.adminBase64 => Admin
    case AuthPrincipalTypeValue.databaseBase64 => Domain
    case AuthPrincipalTypeValue.deviceBase64 => Device
    case AuthPrincipalTypeValue.databaseUserBase64 => User
  }

  def fromPrefix(s: String) = s match {
    case "oa" => Organization
    case "ad" => Admin
    case "do" => Domain
    case "dv" => Device
    case "us" => User
  }


  class AuthPrincipalTypeValue(val principalType: Value) {
    def prefix = principalType match {
      case Organization => "oa"
      case Admin => "ad"
      case Domain => "do"
      case Device => "dv"
      case User => "us"
    }

    def base64Prefix = principalType match {
      case Organization => AuthPrincipalTypeValue.orgBase64
      case Admin => AuthPrincipalTypeValue.adminBase64
      case Domain => AuthPrincipalTypeValue.databaseBase64
      case Device => AuthPrincipalTypeValue.deviceBase64
      case User => AuthPrincipalTypeValue.databaseUserBase64
    }
  }

  implicit def value2AuthPrincipalTypeValue(ap: Value) = new AuthPrincipalTypeValue(ap)

  object AuthPrincipalTypeValue {
    val orgBase64 = (":" + Organization.prefix).base64
    val adminBase64 = (":" + Admin.prefix).base64
    val databaseBase64 = (":" + Domain.prefix).base64
    val deviceBase64 = (":" + Device.prefix).base64
    val databaseUserBase64 = (":" + User.prefix).base64

    val prefixLength = 2
    val base64prefixLength = 4
  }

}
