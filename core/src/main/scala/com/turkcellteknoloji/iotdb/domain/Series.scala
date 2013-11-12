package com.turkcellteknoloji.iotdb.domain

import java.util.UUID

/**
 * Created by Anil Chalil on 10/22/13.
 */
case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String])

trait UserInfo {
  def id: UUID

  def username: String

  def name: String

  def surname: String

  def email: String

  def activated: Boolean

  def confirmed: Boolean

  def disabled: Boolean
}

case class AdminUser(id: UUID, name: String, surname: String, username: String, email: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

case class DatabaseUser(id: UUID, name: String, surname: String, username: String, email: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

case class OrganizationInfo(id: UUID, name: String)

case class Organization(id: UUID, name: String, users: Set[AdminUser])

case class DatabaseInfo(id: UUID, name: String)

case class Database(id: UUID, name: String, owner: Organization)

case class Device(id: UUID, deviceID: String)


trait MetadataRepository {
  def createOrganization(name: String, users: Set[AdminUser]): Organization

  def updateOrganization(org: Organization)
}

