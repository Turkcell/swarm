package com.turkcellteknoloji.iotdb.management

import com.turkcellteknoloji.iotdb.domain.{Device, DatabaseInfo, OrganizationInfo, UserInfo}
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType

/**
 * Created by capacman on 11/1/13.
 */
trait ManagementService {
  def deviceFromAccessToken(token: String): Device

  def databaseInfoFromAccessToken(token: String): DatabaseInfo

  def organizationInfoFromAccessToken(token: String): OrganizationInfo

  def userInfoFromAccessToken(token: String, `type`: AuthPrincipalType): UserInfo
}
