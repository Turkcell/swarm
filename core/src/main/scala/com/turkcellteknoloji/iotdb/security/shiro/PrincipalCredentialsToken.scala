package com.turkcellteknoloji.iotdb.security.shiro

import org.apache.shiro.authc.AuthenticationToken
import com.turkcellteknoloji.iotdb.security.shiro.credentials._
import com.turkcellteknoloji.iotdb.security.shiro.principals._
import com.turkcellteknoloji.iotdb.domain.UserInfo
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.Device
import scala.Some
import com.turkcellteknoloji.iotdb.security.shiro.principals.DatabasePrincipal
import com.turkcellteknoloji.iotdb.security.shiro.principals.OrganizationPrincipal
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.security.shiro.principals.AdminUserPrincipal
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.security.shiro.principals.DatabaseUserPrincipal
import com.turkcellteknoloji.iotdb.security.shiro.credentials.DatabaseAccessToken
import com.turkcellteknoloji.iotdb.security.shiro.principals.DatabasePrincipal
import com.turkcellteknoloji.iotdb.security.shiro.principals.OrganizationPrincipal
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.Device
import scala.Some
import com.turkcellteknoloji.iotdb.security.shiro.credentials.UserAccessToken
import com.turkcellteknoloji.iotdb.security.shiro.principals.AdminUserPrincipal
import com.turkcellteknoloji.iotdb.security.shiro.credentials.OrganizationAccessToken

/**
 * Created by capacman on 11/1/13.
 */
class PrincipalCredentialsToken(val principal: PrincipalIdentifier,
                                val credential: PrincipalCredentials) extends AuthenticationToken {
  def getPrincipal = principal

  def getCredentials = credential
}


object PrincipalCredentialsToken {
  def apply(info: DatabaseInfo, token: String) = {
    val credentials = DatabaseAccessToken(token)
    new PrincipalCredentialsToken(DatabasePrincipal(info, Some(credentials)), credentials)
  }

  def apply(info: OrganizationInfo, token: String) = {
    val credentials = OrganizationAccessToken(token)
    new PrincipalCredentialsToken(OrganizationPrincipal(info, Some(credentials)), credentials)
  }

  def apply(info: UserInfo, token: String, principalType: AuthPrincipalType) = {
    val credentials = UserAccessToken(token)
    principalType match {
      case AuthPrincipalType.Admin => new PrincipalCredentialsToken(new AdminUserPrincipal(Some(info), Some(credentials)), credentials)
      case AuthPrincipalType.DatabaseUser => new PrincipalCredentialsToken(new DatabaseUserPrincipal(Some(info), Some(credentials)), credentials)
      case _ => throw new IllegalArgumentException(s"${classOf[UserInfo].getSimpleName} cannot have ${classOf[AuthPrincipalType].getSimpleName} ${principalType}")
    }
  }

  def apply(device: Device, token: String) = {
    val credentials = DeviceAccessToken(token)
    new PrincipalCredentialsToken(DevicePrincipal(device, Some(credentials)), credentials)
  }
}