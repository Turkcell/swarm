package io.swarm.management

import java.util.UUID
import io.swarm.domain._
import java.util.concurrent.ConcurrentHashMap
import io.swarm.management.dao.ManagementDaoComponent

/**
 * Created by Anil Chalil on 12/18/13.
 */
object Management {

  case class AdminUserRef(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo {
    def toAdminUser(orgs: Set[OrganizationRef]) = AdminUser(id, name, surname, username, email, credential, activated, confirmed, disabled, orgs)
  }

  case class AdminUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, organizations: Set[OrganizationRef]) extends UserInfo {
    def adminUserRef = AdminUserRef(id, name, surname, username, email, credential, activated, confirmed, disabled)
  }

  case class UserRef(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo {
    def toUser(permissions: Set[ACLEntry]) = User(id, name, surname, username, email, credential, activated, confirmed, disabled, permissions)
  }

  case class User(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean, permissions: Set[ACLEntry]) extends UserInfo {
    def userRef = UserRef(id, name, surname, username, email, credential, activated, confirmed, disabled)
  }

  case class ACLEntry(serviceName: String, serviceTenantID: UUID, action: String, extensions: List[String])

  case class OrganizationRef(id: UUID, name: String, disabled: Boolean) extends DisableableResourceRef {
    def toOrganization(domains: Set[DomainRef], admins: Set[AdminUserRef]) = Organization(id, name, disabled, domains, admins)
  }

  case class Organization(id: UUID, name: String, disabled: Boolean, domains: Set[DomainRef], admins: Set[AdminUserRef]) {
    def organizationRef = OrganizationRef(id, name, disabled)
  }

  case class Domain(id: UUID, name: String, organization: OrganizationRef) {
    def domainRef = DomainRef(id, name)
  }

  case class DomainRef(id: UUID, name: String) extends ResourceRef {
    def toDomain(orgRef: OrganizationRef) = Domain(id, name, orgRef)
  }

  case class DeviceRef(id: UUID, deviceID: String, activated: Boolean, disabled: Boolean) extends Client {
    def confirmed = true

    def toDevice(permissions: Set[ACLEntry]) = Device(id, deviceID, activated, disabled, permissions)
  }

  case class Device(id: UUID, deviceID: String, activated: Boolean, disabled: Boolean, permissions: Set[ACLEntry]) extends Client {
    def confirmed = true

    def deviceRef = DeviceRef(id, deviceID, activated, disabled)
  }

  trait ServiceProvider {
    def `type`: String

    def initialize(serviceName: String): UUID

    def drop(uuid: UUID)
  }

  trait ACLService {
    type Session

    def truncateServicePermissions(clientID: UUID, serviceName: String)(implicit session: Session)
  }

  trait ACLServiceComponent {
    val aclService: ACLService
  }

  trait ServiceProviderRegistryComponent {
    this: ManagementDaoComponent =>
    val serviceRegistry: ServiceProviderRegistry

    trait ServiceProviderRegistry {
      val registry = new ConcurrentHashMap[String, ServiceProvider]

      def addServiceProvider(`type`: String, provider: ServiceProvider) {
        //TODO
        //registry.putIfAbsent()
      }
    }

  }

  trait OrganizationRepositoryComponent {
    val organizationRepository: OrganizationRepository
  }

  trait OrganizationRepository {
    type Session

    def saveOrganization(org: Organization)(implicit session: Session): Organization

    def updateOrganization(id: UUID, f: Organization => Organization)(implicit session: Session): Organization

    def getOrganizationRef(id: UUID)(implicit session: Session): Option[OrganizationRef]

    def getOrganization(id: UUID)(implicit session: Session): Option[Organization]

    def getOrganizationByName(name: String)(implicit session: Session): Option[OrganizationRef]

    def getDomain(id: UUID)(implicit session: Session): Option[Domain]

    def addAdminToOrganization(adminID: UUID, orgID: UUID)(implicit session: Session)

    def getAdminUserRef(id: UUID)(implicit session: Session): Option[AdminUserRef]

    def getAdminUserRefByEmail(email: String)(implicit session: Session): Option[AdminUserRef]

    def getAdminUserRefByUsername(username: String)(implicit session: Session): Option[AdminUserRef]

    def getAdminUser(id: UUID)(implicit session: Session): Option[AdminUser]

    def getAdminUserByEmail(email: String)(implicit session: Session): Option[AdminUser]

    def getAdminUserByUsername(username: String)(implicit session: Session): Option[AdminUser]

    def saveAdminUser(user: AdminUserRef)(implicit session: Session): AdminUserRef

    def updateAdminUser(adminUser: AdminUserRef)(implicit session: Session): AdminUserRef

    def getDeviceRef(id: UUID)(implicit session: Session): Option[DeviceRef]

    def getDevice(id: UUID)(implicit session: Session): Option[Device]

    def saveDeviceRef(device: DeviceRef)(implicit session: Session): DeviceRef

    def updateDeviceRef(device: DeviceRef)(implicit session: Session): DeviceRef

    def saveDevice(device: Device)(implicit session: Session): Device

    def updateDevice(id: UUID, f: Device => Device)(implicit session: Session): Device
  }

  trait ClientRepositoryComponent {
    val clientRepository: ClientRepository
  }

  trait ClientRepository {
    type Session

    def getUserRefByEmail(email: String)(implicit session: Session): Option[UserRef]

    def getUserRefByUsername(username: String)(implicit session: Session): Option[UserRef]

    def getUserRef(id: UUID)(implicit session: Session): Option[UserRef]

    def getUserByEmail(email: String)(implicit session: Session): Option[User]

    def getUserByUsername(username: String)(implicit session: Session): Option[User]

    def getUser(id: UUID)(implicit session: Session): Option[User]

    def saveUserRef(user: UserRef)(implicit session: Session): UserRef

    def updateUserRef(user: UserRef)(implicit session: Session): UserRef
  }

}
