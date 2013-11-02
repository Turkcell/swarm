package com.turkcellteknoloji.iotdb.security.shiro.credentials

/**
 * Created by capacman on 11/1/13.
 */
trait PrincipalCredentials

trait AccessTokenCredentials extends PrincipalCredentials {
  def token: String
}

trait ClientCredentials extends PrincipalCredentials {
  def id: String

  def secret: String
}

trait PasswordCredentials extends PrincipalCredentials {
  def password: String
}

trait DatabaseCredentials extends PrincipalCredentials

case class DatabaseAccessToken(val token: String) extends AccessTokenCredentials with DatabaseCredentials

case class DatabaseGuest(val token: String) extends AccessTokenCredentials with DatabaseCredentials

case class DatabaseClientCredentials(val id: String, val secret: String) extends ClientCredentials with DatabaseCredentials


trait OrganizationCredentials extends PrincipalCredentials

case class OrganizationAccessToken(val token: String) extends AccessTokenCredentials with OrganizationCredentials

case class OrganizationClientCredentials(val id: String, val secret: String) extends ClientCredentials with OrganizationCredentials


trait UserCredentials extends PrincipalCredentials

case class UserAccessToken(val token: String) extends AccessTokenCredentials with UserCredentials

case class UserPassword(val password: String) extends PasswordCredentials with UserCredentials


trait DeviceCredentials extends PrincipalCredentials

case class DeviceAccessToken(val token: String) extends AccessTokenCredentials with DeviceCredentials

case class DeviceClientCredentials(val id: String, val secret: String) extends ClientCredentials with DeviceCredentials