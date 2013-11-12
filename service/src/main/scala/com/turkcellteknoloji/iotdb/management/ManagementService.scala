package com.turkcellteknoloji.iotdb.management

import com.turkcellteknoloji.iotdb.domain._
import com.turkcellteknoloji.iotdb.security.AuthPrincipalType.AuthPrincipalType
import com.turkcellteknoloji.iotdb.domain.OrganizationInfo
import com.turkcellteknoloji.iotdb.domain.DatabaseInfo
import com.turkcellteknoloji.iotdb.domain.Device
import java.util.UUID

/**
 * Created by capacman on 11/1/13.
 */
trait ManagementService {
  def deviceFromAccessToken(token: String): Device

  def databaseInfoFromAccessToken(token: String): DatabaseInfo

  def organizationInfoFromAccessToken(token: String): OrganizationInfo

  def userInfoFromAccessToken(token: String, `type`: AuthPrincipalType): UserInfo

  def createOrganization(name: String, adminUsers: Set[AdminUser]): Organization

  def createDatabase(name: String, orgID: UUID): Database
}
