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

case class DatabaseAccessToken(token: String) extends AccessTokenCredentials with DatabaseCredentials

case class DatabaseGuest(token: String) extends AccessTokenCredentials with DatabaseCredentials

case class DatabaseClientCredentials(id: String, secret: String) extends ClientCredentials with DatabaseCredentials


trait OrganizationCredentials extends PrincipalCredentials

case class OrganizationAccessToken(token: String) extends AccessTokenCredentials with OrganizationCredentials

case class OrganizationClientCredentials(id: String, secret: String) extends ClientCredentials with OrganizationCredentials


trait UserCredentials extends PrincipalCredentials

case class UserAccessToken(token: String) extends AccessTokenCredentials with UserCredentials

case class UserPassword(password: String) extends PasswordCredentials with UserCredentials


trait DeviceCredentials extends PrincipalCredentials

case class DeviceAccessToken(token: String) extends AccessTokenCredentials with DeviceCredentials

case class DeviceClientCredentials(id: String, secret: String) extends ClientCredentials with DeviceCredentials