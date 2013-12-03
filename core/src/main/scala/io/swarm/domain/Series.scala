/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.swarm.domain

import java.util.UUID
import scala.concurrent._
import io.swarm.IOTDBException

/**
 * Created by Anil Chalil on 10/22/13.
 */

object SeriesType extends Enumeration {
  type SeriesType = Value
  val Long, Float = Value
}

case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String], `type`: SeriesType.SeriesType)

trait IDEntity {
  def id: UUID
}

trait Client extends IDEntity {

  def activated: Boolean

  def confirmed: Boolean

  def disabled: Boolean
}

trait UserInfo extends Client {
  def username: String

  def name: Option[String]

  def surname: Option[String]

  def email: String

  def credential: String
}

trait CustomPermissions {
  def permissions: Set[String]
}

case class AdminUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, organizations: Set[Organization]) extends UserInfo

case class DatabaseUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, permissions: Set[String]) extends UserInfo with CustomPermissions

trait ResourceRef extends IDEntity {
  def name: String
}

trait OrganizationRef extends ResourceRef

trait DatabaseRef extends ResourceRef

case class DatabaseMetadata(val oauthTTL: Long)

case class OrganizationInfo(id: UUID, name: String) extends OrganizationRef

case class Organization(id: UUID, name: String, databases: Set[Database]) extends OrganizationRef {
  def organizationInfo = new OrganizationInfo(id, name)
}

case class DatabaseInfo(id: UUID, name: String) extends DatabaseRef

case class Database(id: UUID, name: String, metadata: DatabaseMetadata) extends DatabaseRef {
  def databaseInfo = new DatabaseInfo(id, name)
}

case class Device(id: UUID, deviceID: String, databaseRef: DatabaseRef, activated: Boolean, disabled: Boolean, permissions: Set[String]) extends Client with CustomPermissions {
  def confirmed = true
}

trait ResourceRepository {
  def getOrganizationInfoAsync(uuid: UUID): Future[Option[OrganizationInfo]]

  def getDatabaseInfoAsync(uuid: UUID): Future[Option[DatabaseInfo]]

  def saveOrganization(org: Organization): Organization

  def upsertOrganization(org: Organization): Option[Organization]

  def getOrganizationInfo(id: UUID): Option[OrganizationInfo]

  def getOrganization(id: UUID): Option[Organization]

  def getDatabaseInfo(id: UUID): Option[DatabaseInfo]

  def getDatabase(id: UUID): Option[Database]

  def getDatabaseMetadata(id: UUID): Option[DatabaseMetadata]
}

trait ResourceRepositoryComponent {
  val resourceRepository: ResourceRepository
}

trait DuplicateIDEntity extends IOTDBException

trait IDEntityNotFound extends IOTDBException

object IDEntityNotFound {
  def apply(message: String) = new RuntimeException(message) with DuplicateIDEntity
}

object DuplicateIDEntity {
  def apply(message: String) = new RuntimeException(message) with DuplicateIDEntity

  def apply(t: Throwable) = new RuntimeException(t) with DuplicateIDEntity
}

trait ClientRepository {
  def getAdminUser(uuid: UUID): Option[UserInfo]

  def getAdminUserByEmail(email: String): Option[UserInfo]

  def getAdminUserByUsername(username: String): Option[UserInfo]

  def getDatabaseUserByEmail(email: String): Option[UserInfo]

  def getDatabaseUserByUsername(username: String): Option[UserInfo]

  def getDatabaseUser(uuid: UUID): Option[UserInfo]

  def getDevice(uuid: UUID): Option[Device]

  def getAdminUserAsync(uuid: UUID): Future[Option[UserInfo]]

  def getDatabaseUserAsync(uuid: UUID): Future[Option[UserInfo]]

  def getDeviceAsync(uuid: UUID): Future[Option[Device]]

  def saveAdminUser(user: UserInfo): Unit

  def saveDatabaseUser(user: UserInfo): Unit

  def saveDevice(device: Device): Unit

  def upsertAdminUser(user: UserInfo): Option[UserInfo]

  def upsertDatabaseUser(user: UserInfo): Option[UserInfo]

  def upsertDevice(device: Device): Option[Device]
}

trait ClientRepositoryComponent {
  val clientRepository: ClientRepository

}

