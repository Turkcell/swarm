package com.turkcellteknoloji.iotdb.security
import com.turkcellteknoloji.iotdb.domain._
/**
 * Created by capacman on 10/31/13.
 */
object AuthPrincipalType extends Enumeration {
  type AuthPrincipalType = Value
  val Organization, Admin, Database, Device, DatabaseUser = Value


  def fromBase64(s: String) = s match {
    case AuthPrincipalTypeValue.orgBase64 => Organization
    case AuthPrincipalTypeValue.adminBase64 => Admin
    case AuthPrincipalTypeValue.databaseBase64 => Database
    case AuthPrincipalTypeValue.deviceBase64 => Device
    case AuthPrincipalTypeValue.databaseUserBase64 => DatabaseUser
  }

  def fromPrefix(s: String) = s match {
    case "oa" => Organization
    case "ad" => Admin
    case "db" => Database
    case "dv" => Device
    case "du" => DatabaseUser
  }


  class AuthPrincipalTypeValue(val principalType: Value) {
    def prefix = principalType match {
      case Organization => "oa"
      case Admin => "ad"
      case Database => "db"
      case Device => "dv"
      case DatabaseUser => "du"
    }

    def base64Prefix = principalType match {
      case Organization => AuthPrincipalTypeValue.orgBase64
      case Admin => AuthPrincipalTypeValue.adminBase64
      case Database => AuthPrincipalTypeValue.databaseBase64
      case Device => AuthPrincipalTypeValue.deviceBase64
      case DatabaseUser => AuthPrincipalTypeValue.databaseUserBase64
    }
  }

  implicit def value2AuthPrincipalTypeValue(ap: Value) = new AuthPrincipalTypeValue(ap)

  object AuthPrincipalTypeValue {
    val orgBase64 = Organization.prefix.base64
    val adminBase64 = Admin.prefix.base64
    val databaseBase64 = Database.prefix.base64
    val deviceBase64 = Device.prefix.base64
    val databaseUserBase64 = DatabaseUser.prefix.base64
  }

}
