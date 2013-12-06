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
import io.swarm.persistence.PersistenceSessionComponent

/**
 * Created by Anil Chalil on 10/22/13.
 */

object SeriesType extends Enumeration {
  type SeriesType = Value
  val Long, Float = Value
}

case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String], `type`: SeriesType.SeriesType) extends IDEntity

trait IDEntity {
  def id: UUID
}

trait Versioned {
  def version: Int
}

trait Client extends IDEntity with Versioned {

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

case class AdminUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, organizations: Set[Organization], version: Int) extends UserInfo

case class DatabaseUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, permissions: Set[String], version: Int) extends UserInfo with CustomPermissions

trait ResourceRef extends IDEntity with Versioned {
  def name: String
}

trait OrganizationRef extends ResourceRef

trait DatabaseRef extends ResourceRef

case class DatabaseMetadata(val oauthTTL: Long)

case class OrganizationInfo(id: UUID, name: String, version: Int) extends OrganizationRef

case class Organization(id: UUID, name: String, databases: Set[Database], version: Int) extends OrganizationRef {
  def organizationInfo = new OrganizationInfo(id, name, version)
}

case class DatabaseInfo(id: UUID, name: String, version: Int) extends DatabaseRef

case class Database(id: UUID, name: String, metadata: DatabaseMetadata, version: Int) extends DatabaseRef {
  def databaseInfo = new DatabaseInfo(id, name, version)
}

case class Device(id: UUID, deviceID: String, databaseRef: DatabaseRef, activated: Boolean, disabled: Boolean, permissions: Set[String], version: Int) extends Client with CustomPermissions {
  def confirmed = true
}


trait ResourceRepositoryComponent {
  this: PersistenceSessionComponent =>
  val resourceRepository: ResourceRepository

  trait ResourceRepository {

    def saveOrganization(org: Organization): Organization

    def getOrganization(id: UUID): Option[Organization]

    def getOrganizationAsync(id: UUID)(implicit execctx: ExecutionContext): Future[Option[Organization]] = future(persistenceSession.withSession(getOrganization(id)))

    def getOrganizationByName(name: String): Option[Organization]

    def saveDatabase(database: Database, orgID: UUID): Database

    def getDatabaseInfo(id: UUID): Option[DatabaseInfo]

    def getDatabaseInfoAsync(id: UUID)(implicit execctx: ExecutionContext): Future[Option[DatabaseInfo]] = future(persistenceSession.withSession(getDatabaseInfo(id)))

    def getDatabaseByName(dbName: String, orgID: UUID): Option[Database]

    def addAdminToOrganization(adminID: UUID, orgID: UUID)
    /*
        def saveSeries(orgName: String, dbName: String, series: Series): Series

        def getSeriesByID(orgName: String, dbName: String, id: UUID): Option[Series]

        def getSeriesByKey(orgName: String, dbName: String, id: UUID): Option[Series]
     */
  }

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


trait ClientRepositoryComponent {
  this: PersistenceSessionComponent =>
  val clientRepository: ClientRepository

  trait ClientRepository {

    def getAdminUser(uuid: UUID): Option[UserInfo]

    def getAdminUserAsync(uuid: UUID)(implicit execctx: ExecutionContext): Future[Option[UserInfo]] = future(persistenceSession.withSession(getAdminUser(uuid)))

    def getAdminUserByEmail(email: String): Option[UserInfo]

    def getAdminUserByUsername(username: String): Option[UserInfo]

    def getDatabaseUserByEmail(email: String): Option[UserInfo]

    def getDatabaseUserByUsername(username: String): Option[UserInfo]

    def getDatabaseUser(uuid: UUID): Option[UserInfo]

    def getDatabaseUserAsync(uuid: UUID)(implicit execctx: ExecutionContext): Future[Option[UserInfo]] = future(persistenceSession.withSession(getDatabaseUser(uuid)))

    def getDevice(uuid: UUID): Option[Device]

    def getDeviceAsync(uuid: UUID)(implicit execctx: ExecutionContext): Future[Option[Device]] = future(persistenceSession.withSession(getDevice(uuid)))

    def saveAdminUser(user: UserInfo): Unit

    def saveDatabaseUser(user: UserInfo): Unit

    def saveDevice(device: Device): Unit

    def upsertAdminUser(user: UserInfo): Option[UserInfo]

    def upsertDatabaseUser(user: UserInfo): Option[UserInfo]

    def upsertDevice(device: Device): Option[Device]
  }

}

