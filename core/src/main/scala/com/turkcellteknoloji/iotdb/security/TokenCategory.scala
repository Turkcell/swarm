package com.turkcellteknoloji.iotdb
package security

/**
 * Created with IntelliJ IDEA.
 * User: capacman
 * Date: 11/12/13
 * Time: 10:40 PM
 * To change this template use File | Settings | File Templates.
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
    val longTokenAge = 7 * 24 * 60 * 60 * 1000
    val shortTokenAge = 24 * 60 * 60 * 1000
  }

}
