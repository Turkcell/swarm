package com.turkcellteknoloji.iotdb.security.shiro.principals

import com.turkcellteknoloji.iotdb.domain._
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.security.shiro.credentials.AccessTokenCredentials

/**
 * Created by capacman on 11/1/13.
 */
trait PrincipalIdentifier {
  def disabled = false

  def activated = true

  def user: Option[UserInfo] = None

  def accessTokenCredentials: Option[AccessTokenCredentials]
}

trait UserPrincipal extends PrincipalIdentifier {
  override def disabled = if (user.isDefined) user.get.disabled else false

  override def activated = if (user.isDefined) user.get.activated else true
}

case class AdminUserPrincipal(override val user: Option[UserInfo], val accessTokenCredentials: Option[AccessTokenCredentials] = None) extends UserPrincipal

case class DatabaseUserPrincipal(override val user: Option[UserInfo], val accessTokenCredentials: Option[AccessTokenCredentials] = None) extends UserPrincipal

case class DatabasePrincipal(val database: DatabaseInfo, val accessTokenCredentials: Option[AccessTokenCredentials]) extends PrincipalIdentifier

case class OrganizationPrincipal(val database: OrganizationInfo, val accessTokenCredentials: Option[AccessTokenCredentials]) extends PrincipalIdentifier

case class DevicePrincipal(val device: Device, val accessTokenCredentials: Option[AccessTokenCredentials]) extends PrincipalIdentifier