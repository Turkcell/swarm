package io.swarm.management

import java.util.UUID
import io.swarm.infrastructure.persistence.PersistenceSessionComponent
import scala.concurrent._
import io.swarm.domain.{Client, UserInfo, ResourceRef}

/**
 * Created by Anil Chalil on 12/18/13.
 */
trait CustomPermissions {
  def permissions: Set[String]
}

case class AdminUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, organizations: Set[Organization], version: Int) extends UserInfo

case class DatabaseUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, permissions: Set[String], version: Int) extends UserInfo with CustomPermissions


trait OrganizationRef extends ResourceRef

trait DomainRef extends ResourceRef

case class OrganizationInfo(id: UUID, name: String, version: Int) extends OrganizationRef

case class Organization(id: UUID, name: String, version: Int) extends OrganizationRef {
  def organizationInfo = new OrganizationInfo(id, name, version)
}

case class Device(id: UUID, deviceID: String, domainRef: DomainRef, activated: Boolean, disabled: Boolean, permissions: Set[String], version: Int) extends Client with CustomPermissions {
  def confirmed = true
}


trait ManagementRepositoryComponent {
  this: PersistenceSessionComponent =>
  val resourceRepository: ResourceRepository

  trait ResourceRepository {

    def saveOrganization(org: Organization): Organization

    def getOrganization(id: UUID): Option[Organization]

    def getOrganizationAsync(id: UUID)(implicit execctx: ExecutionContext): Future[Option[Organization]] = future(persistenceSession.withSession(getOrganization(id)))

    def getOrganizationByName(name: String): Option[Organization]

    def addAdminToOrganization(adminID: UUID, orgID: UUID)
  }

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

    def saveAdminUser(user: AdminUser): Unit

    def saveDatabaseUser(user: DatabaseUser): Unit

    def saveDevice(device: Device): Unit

    def updateAdminUser(adminUser: AdminUser): Unit

    def updateDatabaseUser(databaseUser: DatabaseUser): Unit

    def updateDevice(device: Device): Unit
  }

}

