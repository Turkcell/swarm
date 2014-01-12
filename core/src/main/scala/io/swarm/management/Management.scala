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
    def truncateServicePermissions(clientID: UUID, serviceName: String)
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

    def saveOrganization(org: Organization): Organization

    def updateOrganization(id: UUID, f: Organization => Organization): Organization

    def getOrganizationRef(id: UUID): Option[OrganizationRef]

    def getOrganization(id: UUID): Option[Organization]

    def getOrganizationByName(name: String): Option[OrganizationRef]

    def getDomain(id: UUID): Option[Domain]

    def addAdminToOrganization(adminID: UUID, orgID: UUID)

    def getAdminUserRef(id: UUID): Option[AdminUserRef]

    def getAdminUserRefByEmail(email: String): Option[AdminUserRef]

    def getAdminUserRefByUsername(username: String): Option[AdminUserRef]

    def getAdminUser(id: UUID): Option[AdminUser]

    def getAdminUserByEmail(email: String): Option[AdminUser]

    def getAdminUserByUsername(username: String): Option[AdminUser]

    def saveAdminUser(user: AdminUserRef): AdminUserRef

    def updateAdminUser(adminUser: AdminUserRef): AdminUserRef

    def getDeviceRef(id: UUID): Option[DeviceRef]

    def getDevice(id: UUID): Option[Device]

    def saveDeviceRef(device: DeviceRef): DeviceRef

    def updateDeviceRef(device: DeviceRef): DeviceRef

    def saveDevice(device: Device): Device

    def updateDevice(id: UUID, f: Device => Device): Device
  }

  trait ClientRepositoryComponent {
    val clientRepository: ClientRepository
  }

  trait ClientRepository {

    def getUserRefByEmail(email: String): Option[UserRef]

    def getUserRefByUsername(username: String): Option[UserRef]

    def getUserRef(id: UUID): Option[UserRef]

    def getUserByEmail(email: String): Option[User]

    def getUserByUsername(username: String): Option[User]

    def getUser(id: UUID): Option[User]

    def saveUserRef(user: UserRef): UserRef

    def updateUserRef(user: UserRef): UserRef
  }

}
