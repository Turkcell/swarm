package com.turkcellteknoloji.iotdb.domain

import java.util.UUID

/**
 * Created by Anil Chalil on 10/22/13.
 */
case class Series(val id: UUID, val key: String, val name: Option[String], val tags: Set[String], val attributes: Map[String, String])

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

case class AdminUser(val id: UUID, val name: String, val surname: String, val username: String, val email: String, val activated: Boolean, val confirmed: Boolean, val disabled: Boolean) extends UserInfo

case class DatabaseUser(val id: UUID, val name: String, val surname: String, val username: String, val email: String, val activated: Boolean, val confirmed: Boolean, val disabled: Boolean) extends UserInfo

case class OrganizationInfo(val id: UUID, val name: String)

case class Organization(val id: UUID, val name: String, users: Set[AdminUser])

case class DatabaseInfo(val id: UUID, val name: String)

case class Database(val id: UUID, val name: String, val owner: Organization)

case class Device(val id: UUID, val deviceID: String)


trait MetadataRepository {
  def createOrganization(name: String, users: Set[AdminUser]): Organization
  def updateOrganization(org:Organization)
}

