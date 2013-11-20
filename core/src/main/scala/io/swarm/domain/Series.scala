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
case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String])

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

  def name: String

  def surname: String

  def email: String

  def credential: String
}

case class AdminUser(id: UUID, name: String, surname: String, username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

case class DatabaseUser(id: UUID, name: String, surname: String, username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

trait ResourceRef extends IDEntity {
  def name: String
}

trait OrganizationRef extends ResourceRef

trait DatabaseRef extends ResourceRef

case class DatabaseMetadata(val oauthTTL: Long)

case class OrganizationInfo(id: UUID, name: String) extends OrganizationRef

case class Organization(id: UUID, name: String, users: Set[AdminUser]) extends OrganizationRef

case class DatabaseInfo(id: UUID, name: String) extends DatabaseRef

case class Database(id: UUID, name: String, metadata: DatabaseMetadata, owner: OrganizationInfo) extends DatabaseRef

case class Device(id: UUID, deviceID: String, databaseInfo: DatabaseInfo, activated: Boolean, disabled: Boolean) extends Client {
  def confirmed = true
}

trait ResourceRepository {
  def getOrganizationInfoAsync(uuid: UUID): Future[Option[OrganizationInfo]]

  def getDatabaseInfoAsync(uuid: UUID): Future[Option[DatabaseInfo]]

  def saveOrganization(org: Organization): Unit

  def upsertOrganization(org: Organization): Option[Organization]

  def saveDatabase(db: Database): Unit

  def upsertDatabase(db: Database): Option[Database]

  def getOrganizationInfo(id: UUID): Option[OrganizationInfo]

  def getOrganization(id: UUID): Option[Organization]

  def getDatabaseInfo(id: UUID): Option[DatabaseInfo]

  def getDatabase(id: UUID): Option[Database]

  def getDatabaseMetadata(id: UUID): Option[DatabaseMetadata]
}

trait ResourceRepositoryComponent {
  val resourceRepository: ResourceRepository
}
trait DublicateIDEntity extends IOTDBException

object DublicateIDEntity {
  def apply(message: String) = new RuntimeException(message) with DublicateIDEntity
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

