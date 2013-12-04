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
import com.github.nscala_time.time.Imports._
/**
 * Created by Anil Chalil
 */
object TokenCategory extends Enumeration {
  type TokenCategory = Value
  val Access, Refresh, Offline, Email = Value

  def fromBase64(s: String) = s match {
    case TokenCategoryValue.accBase64 => Access
    case TokenCategoryValue.reBase64 => Refresh
    case TokenCategoryValue.offBase64 => Offline
    case TokenCategoryValue.emailBase64 => Email
  }

  def fromPrefix(s: String) = s match {
    case "ac" => Access
    case "re" => Refresh
    case "of" => Offline
    case "em" => Email
  }

  class TokenCategoryValue(val tokenCategory: Value) {
    def prefix = tokenCategory match {
      case Access => "ac"
      case Refresh => "re"
      case Offline => "of"
      case Email => "em"
    }

    def base64Prefix = tokenCategory match {
      case Access => TokenCategoryValue.accBase64
      case Refresh => TokenCategoryValue.reBase64
      case Offline => TokenCategoryValue.offBase64
      case Email => TokenCategoryValue.emailBase64
    }

    def expires = tokenCategory match {
      case Access => true
      case Refresh => false
      case Offline => false
      case Email => false
    }

    def expiration = tokenCategory match {
      case Access => TokenCategoryValue.longTokenAge
      case Refresh => TokenCategoryValue.longTokenAge
      case Offline => TokenCategoryValue.longTokenAge
      case Email => TokenCategoryValue.longTokenAge
      case _ => TokenCategoryValue.shortTokenAge
    }
  }

  implicit def value2TokenCategoryValue(ap: Value) = new TokenCategoryValue(ap)

  object TokenCategoryValue {
    val accBase64 = ("-" + Access.prefix).base64
    val reBase64 = ("-" + Refresh.prefix).base64
    val offBase64 = ("-" + Offline.prefix).base64
    val emailBase64 = ("-" + Email.prefix).base64
    val prefixLength = 2
    val base64prefixLength = 4
    val longTokenAge = (7 * 24 * 60 * 60 * 1000).toDuration
    val shortTokenAge = (24 * 60 * 60 * 1000).toDuration
  }

}
