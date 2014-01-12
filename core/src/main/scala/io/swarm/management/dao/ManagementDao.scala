package io.swarm.management.dao

import java.util.UUID
import io.swarm.domain.{IDEntityNotFound, UserInfo}
import io.swarm.management.Management._

/**
 * Created by Anil Chalil on 1/9/14.
 */
trait ManagementDaoComponent {
  val managementDao: ManagementDao
}

trait ManagementDao {
  def getDomain(id: UUID): Option[Domain]

  def updateDomain(domain: DomainRef, orgID: UUID): DomainRef

  def saveACL(clientID: UUID, aclEntry: ACLEntry): Unit

  def dropACLEntry(clientID: UUID, aclEntry: ACLEntry): Unit

  def dropServiceACLS(clientID: UUID, serviceName: String): Unit

  def updateDeviceRef(ref: DeviceRef): DeviceRef

  def updateUserRef(ref: UserRef): UserRef

  def saveDeviceRef(ref: DeviceRef): DeviceRef

  def saveUserRef(ref: UserRef): UserRef

  def getDevice(id: UUID): Option[Device]

  def getDeviceRef(id: UUID): Option[DeviceRef]

  def getDeviceRefByDeviceID(deviceID: String): Option[DeviceRef]

  def getUserRef(id: UUID): Option[UserRef]

  def getUserRefByUsername(username: String): Option[UserRef]

  def getUserRefByEmail(email: String): Option[UserRef]

  def getUser(id: UUID): Option[User]

  def getUserByUsername(username: String): Option[User]

  def getUserByEmail(email: String): Option[User]

  def associateAdmin(orgID: UUID, adminID: UUID)

  def saveOrganizationRef(org: OrganizationRef): OrganizationRef

  def updateOrganizationRef(org: OrganizationRef): OrganizationRef

  def getOrganizationRef(id: UUID): Option[OrganizationRef]

  def getOrganization(id: UUID): Option[Organization]

  def getOrganizationRefByName(name: String): Option[OrganizationRef]

  def removeDomain(domainID: UUID)

  def getDomainCount(orgID: UUID): Int

  def saveDomain(domain: DomainRef, orgID: UUID): DomainRef

  def getAdminUserRef(uuid: UUID): Option[AdminUserRef]

  def getAdminUserRefByEmail(email: String): Option[AdminUserRef]

  def getAdminUserRefByUsername(username: String): Option[AdminUserRef]

  def getAdminUser(uuid: UUID): Option[AdminUser]

  def getAdminUserByEmail(email: String): Option[AdminUser]

  def getAdminUserByUsername(username: String): Option[AdminUser]

  def saveAdminUserRef(user: AdminUserRef): AdminUserRef

  def updateAdminUserRef(adminUser: AdminUserRef): AdminUserRef
}

trait OrganizationRepositoryDaoComponent extends OrganizationRepositoryComponent {
  this: ManagementDaoComponent =>
  val resourceRepository: OrganizationRepository = new OrganizationRepository {

    def saveOrganization(org: Organization): Organization = {
      managementDao.saveOrganizationRef(org.organizationRef)
      org.domains.foreach(managementDao.saveDomain(_, org.id))
      org.admins.foreach(a => managementDao.associateAdmin(org.id, a.id))
      org
    }

    def updateOrganization(id: UUID, f: Organization => Organization): Organization = {
      val old = getOrganization(id).getOrElse(throw IDEntityNotFound(s"organization with id:$id is not found"))
      val newOrg = f(old)
      if (newOrg.organizationRef != old.organizationRef) {
        managementDao.updateOrganizationRef(newOrg.organizationRef)
      }
      val changedAndNew = newOrg.domains -- old.domains
      changedAndNew.foreach {
        domain =>
          if (old.domains.exists(_.id == domain.id)) {
            managementDao.updateDomain(domain, id)
          } else {
            managementDao.saveDomain(domain, id)
          }
      }
      (old.domains -- newOrg.domains).foreach {
        domain =>
          managementDao.removeDomain(domain.id)
      }
      newOrg
    }

    def getOrganizationRef(id: UUID): Option[OrganizationRef] = managementDao.getOrganizationRef(id)

    def getOrganization(id: UUID): Option[Organization] = managementDao.getOrganization(id)

    def getOrganizationByName(name: String): Option[OrganizationRef] = managementDao.getOrganizationRefByName(name)

    def addAdminToOrganization(adminID: UUID, orgID: UUID) = managementDao.associateAdmin(orgID, adminID)

    def getAdminUserRef(id: UUID): Option[AdminUserRef] = managementDao.getAdminUserRef(id)

    def getAdminUserRefByEmail(email: String): Option[AdminUserRef] = managementDao.getAdminUserRefByEmail(email)

    def getAdminUserRefByUsername(username: String): Option[AdminUserRef] = managementDao.getAdminUserRefByUsername(username)

    def saveAdminUser(user: AdminUserRef): AdminUserRef = managementDao.saveAdminUserRef(user)

    def updateAdminUser(adminUser: AdminUserRef): AdminUserRef = managementDao.updateAdminUserRef(adminUser)

    def getDeviceRef(id: UUID): Option[DeviceRef] = managementDao.getDeviceRef(id)

    def saveDeviceRef(device: DeviceRef): DeviceRef = managementDao.saveDeviceRef(device)

    def updateDeviceRef(device: DeviceRef): DeviceRef = managementDao.updateDeviceRef(device)

    def updateDevice(id: UUID, f: Device => Device): Device = {
      val oldDevice = managementDao.getDevice(id).getOrElse(throw IDEntityNotFound(s"device with id $id is not found"))
      val newDevice = f(oldDevice)
      if (oldDevice.deviceRef != newDevice.deviceRef) {
        managementDao.updateDeviceRef(newDevice.deviceRef)
      }
      val changedAndNew = newDevice.permissions -- oldDevice.permissions
      changedAndNew.foreach {
        permission =>
          managementDao.saveACL(id, permission)
      }
      (oldDevice.permissions -- newDevice.permissions).foreach {
        permission =>
          managementDao.dropACLEntry(id, permission)
      }
      newDevice
    }

    def saveDevice(device: Device): Device = {
      managementDao.saveDeviceRef(device.deviceRef)
      device.permissions.foreach(permission => managementDao.saveACL(device.id, permission))
      device
    }

    def getAdminUserByUsername(username: String): Option[AdminUser] = managementDao.getAdminUserByUsername(username)

    def getAdminUserByEmail(email: String): Option[AdminUser] = managementDao.getAdminUserByEmail(email)

    def getAdminUser(id: UUID): Option[AdminUser] = managementDao.getAdminUser(id)

    def getDevice(id: UUID): Option[Device] = managementDao.getDevice(id)

    def getDomain(id: UUID): Option[Domain] = managementDao.getDomain(id)
  }

}

trait ClientRepositoryDaoComponent extends ClientRepositoryComponent {
  this: ManagementDaoComponent =>
  val clientRepository: ClientRepository = new ClientRepository {

    def getUserRefByEmail(email: String): Option[UserRef] = managementDao.getUserRefByEmail(email)

    def getUserRefByUsername(username: String): Option[UserRef] = managementDao.getUserRefByUsername(username)

    def getUserRef(id: UUID): Option[UserRef] = managementDao.getUserRef(id)

    def saveUserRef(user: UserRef): UserRef = managementDao.saveUserRef(user)

    def updateUserRef(user: UserRef): UserRef = managementDao.updateUserRef(user)

    def getUser(id: UUID): Option[User] = managementDao.getUser(id)

    def getUserByUsername(username: String): Option[User] = managementDao.getUserByUsername(username)

    def getUserByEmail(email: String): Option[User] = managementDao.getUserByEmail(email)
  }

  trait ACLServiceComponentDao extends ACLServiceComponent {
    this: ManagementDaoComponent =>
    val aclService = new ACLService {
      def truncateServicePermissions(clientID: UUID, serviceName: String): Unit = managementDao.dropServiceACLS(clientID, serviceName)
    }
  }

}