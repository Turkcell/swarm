package io.swarm.management

import java.util.UUID
import io.swarm.domain.{Client, UserInfo, ResourceRef}
import io.swarm.UUIDGenerator
import java.util.concurrent.ConcurrentHashMap

/**
 * Created by Anil Chalil on 12/18/13.
 */
object Management {
  type Organization = (OrganizationRef, Set[Domain])
  type User = (UserRef, Set[ACLEntry])
  type Device = (DeviceRef, Set[ACLEntry])

  case class AdminUser(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean)

  case class UserRef(id: UUID, name: Option[String], surname: Option[String], username: String, email: String, credential: String, activated: Boolean, confirmed: Boolean, disabled: Boolean) extends UserInfo

  case class ACLEntry(serviceName: String, serviceTenantID: UUID, action: String, extensions: List[String])

  case class OrganizationRef(id: UUID, name: String, disabled: Boolean) extends ResourceRef

  case class Domain(id: UUID, name: String) extends ResourceRef

  case class DeviceRef(id: UUID, deviceID: String, activated: Boolean, disabled: Boolean) extends Client {
    def confirmed = true
  }

  trait ServiceProvider {
    def `type`: String

    def initialize(serviceName: String): UUID

    def drop(uuid: UUID)
  }

  trait ACLService {
    def assignPermission(tenantID: UUID, clientID: UUID, serviceName: String, action: String, extensions: List[String])

    def truncateServicePermissions(clientID: UUID, serviceName: String)
  }

  trait ACLServiceComponent {
    this: ManagementDaoComponent =>
    val aclService = new ACLService {
      def truncateServicePermissions(clientID: UUID, serviceName: String): Unit = managementDao.dropACLs(clientID, serviceName)

      def assignPermission(tenantID: UUID, clientID: UUID, serviceName: String, action: String, extensions: List[String]): Unit = managementDao.saveACL(tenantID, clientID, serviceName, action, extensions)
    }
  }

  trait ServiceProviderRegistryComponent {
    this: ManagementDaoComponent =>

    import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter


    val serviceRegistry: ServiceProviderRegistry

    trait ServiceProviderRegistry {
      val registry = new ConcurrentHashMap[String, ServiceProvider]() asScala

      def addServiceProvider(`type`: String, provider: ServiceProvider) {
        //TODO
        //registry.putIfAbsent()
      }
    }

  }

  trait OrganizationService {
    this: ManagementDaoComponent =>

    case class DomainRemovedEvent(id: UUID)

    def createDomain(orgID: UUID, domainName: String) = {
      val domain = Domain(UUIDGenerator.randomGenerator.generate(), domainName)
      managementDao.saveDomain(domain, orgID)
      domain
    }

    def removeDomain(domainID: UUID) = {
      managementDao.removeDomain(domainID)
    }
  }


  trait OrganizationRepositoryComponent {
    this: ManagementDaoComponent =>
    val resourceRepository: OrganizationRepository

    trait OrganizationRepository {

      def saveOrganization(org: OrganizationRef): OrganizationRef = managementDao.saveOrganizationRef(org)

      def getOrganization(id: UUID): Option[OrganizationRef] = managementDao.getOrganizationRef(id)

      def getOrganizationWithDomains(id: UUID): Option[(OrganizationRef, Set[Domain])] = managementDao.getOrganization(id)

      def getOrganizationByName(name: String): Option[OrganizationRef] = managementDao.getOrganizationRefByName(name)

      def addAdminToOrganization(adminID: UUID, orgID: UUID) = managementDao.associateAdmin(orgID, adminID)

      def getAdminUser(id: UUID): Option[AdminUser] = managementDao.getAdminUser(id)

      def getAdminUserByEmail(email: String): Option[AdminUser] = managementDao.getAdminUserByEmail(email)

      def getAdminUserByUsername(username: String): Option[AdminUser] = managementDao.getAdminUserByUsername(username)

      def saveAdminUser(user: AdminUser): AdminUser = managementDao.saveAdminUser(user)

      def updateAdminUser(adminUser: AdminUser): AdminUser = managementDao.updateAdminUser(adminUser)
    }

  }

  trait ClientRepositoryComponent {
    this: ManagementDaoComponent =>
    val clientRepository: ClientRepository

    trait ClientRepository {

      def getUserRefByEmail(email: String): Option[UserRef] = managementDao.getUserRefByEmail(email)

      def getUserRefByUsername(username: String): Option[UserInfo] = managementDao.getUserRefByUsername(username)

      def getUserRef(id: UUID): Option[UserInfo] = managementDao.getUserRef(id)

      def getDeviceRef(id: UUID): Option[DeviceRef] = managementDao.getDeviceRef(id)

      def saveUserRef(user: UserRef): UserRef = managementDao.saveUserRef(user)

      def saveDeviceRef(device: DeviceRef): DeviceRef = managementDao.saveDeviceRef(device)

      def updateUserRef(user: UserRef): UserRef = managementDao.updateUserRef(user)

      def updateDeviceRef(device: DeviceRef): DeviceRef = managementDao.updateDeviceRef(device)

    }

  }

  trait ManagementDao {
    def saveACL(clientID: UUID, tenantID: UUID, serviceName: String, action: String, servicePerms: List[String]): Unit

    def dropACLs(clientID: UUID, serviceName: String): Unit

    def updateDeviceRef(ref: DeviceRef): DeviceRef

    def updateUserRef(ref: UserRef): UserRef

    def saveDeviceRef(ref: DeviceRef): DeviceRef

    def saveUserRef(ref: UserRef): UserRef

    def getDevice(id: UUID): Option[Device]

    def getDeviceRef(id: UUID): Option[DeviceRef]

    def getDeviceRefByDeviceID(deviceID: String): Option[DeviceRef]

    def getUserRef(id: UUID): Option[UserInfo]

    def getUserRefByUsername(username: String): Option[UserInfo]

    def getUserRefByEmail(email: String): Option[UserRef]

    def associateAdmin(orgID: UUID, adminID: UUID)

    def saveOrganizationRef(org: OrganizationRef): OrganizationRef

    def getOrganizationRef(id: UUID): Option[OrganizationRef]

    def getOrganization(id: UUID): Option[Organization]

    def getOrganizationRefByName(name: String): Option[OrganizationRef]

    def removeDomain(domainID: UUID)

    def getDomainCount(orgID: UUID): Int

    def saveDomain(domain: Domain, orgID: UUID): Domain

    def getAdminUser(uuid: UUID): Option[AdminUser]

    def getAdminUserByEmail(email: String): Option[AdminUser]

    def getAdminUserByUsername(username: String): Option[AdminUser]

    def saveAdminUser(user: AdminUser): AdminUser

    def updateAdminUser(adminUser: AdminUser): AdminUser
  }

  trait ManagementDaoComponent {
    val managementDao: ManagementDao
  }

}



