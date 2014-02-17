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
  type Session

  def getDomain(id: UUID, reg: ServiceProviderRegistry)(implicit session: Session): Option[Domain]

  def updateDomain(domain: DomainRef, orgID: UUID)(implicit session: Session): DomainRef

  def saveACL(clientID: UUID, aclEntry: ACLEntry)(implicit session: Session): Unit

  def dropACLEntry(clientID: UUID, aclEntry: ACLEntry)(implicit session: Session): Unit

  def dropServiceACLS(clientID: UUID, serviceName: String)(implicit session: Session): Unit

  def updateDeviceRef(ref: DeviceRef)(implicit session: Session): DeviceRef

  def updateUserRef(ref: UserRef)(implicit session: Session): UserRef

  def saveDeviceRef(ref: DeviceRef)(implicit session: Session): DeviceRef

  def saveUserRef(ref: UserRef)(implicit session: Session): UserRef

  def getDevice(id: UUID)(implicit session: Session): Option[Device]

  def getDeviceRef(id: UUID)(implicit session: Session): Option[DeviceRef]

  def getDeviceRefByDeviceID(deviceID: String)(implicit session: Session): Option[DeviceRef]

  def getUserRef(id: UUID)(implicit session: Session): Option[UserRef]

  def getUserRefByUsername(username: String)(implicit session: Session): Option[UserRef]

  def getUserRefByEmail(email: String)(implicit session: Session): Option[UserRef]

  def getUser(id: UUID)(implicit session: Session): Option[User]

  def getUserByUsername(username: String)(implicit session: Session): Option[User]

  def getUserByEmail(email: String)(implicit session: Session): Option[User]

  def associateAdmin(orgID: UUID, adminID: UUID)(implicit session: Session)

  def saveOrganizationRef(org: OrganizationRef)(implicit session: Session): OrganizationRef

  def updateOrganizationRef(org: OrganizationRef)(implicit session: Session): OrganizationRef

  def getOrganizationRef(id: UUID)(implicit session: Session): Option[OrganizationRef]

  def getOrganization(id: UUID)(implicit session: Session): Option[Organization]

  def getOrganizationRefByName(name: String)(implicit session: Session): Option[OrganizationRef]

  def removeDomain(domainID: UUID)(implicit session: Session)

  def getDomainCount(orgID: UUID)(implicit session: Session): Int

  def saveDomain(domain: DomainRef, orgID: UUID)(implicit session: Session): DomainRef

  def getAdminUserRef(uuid: UUID)(implicit session: Session): Option[AdminUserRef]

  def getAdminUserRefByEmail(email: String)(implicit session: Session): Option[AdminUserRef]

  def getAdminUserRefByUsername(username: String)(implicit session: Session): Option[AdminUserRef]

  def getAdminUser(uuid: UUID)(implicit session: Session): Option[AdminUser]

  def getAdminUserByEmail(email: String)(implicit session: Session): Option[AdminUser]

  def getAdminUserByUsername(username: String)(implicit session: Session): Option[AdminUser]

  def saveAdminUserRef(user: AdminUserRef)(implicit session: Session): AdminUserRef

  def updateAdminUserRef(adminUser: AdminUserRef)(implicit session: Session): AdminUserRef
}

trait OrganizationRepositoryDaoComponent extends OrganizationRepositoryComponent {
  this: ManagementDaoComponent with ServiceProviderRegistryComponent =>
  val resourceRepository: OrganizationRepository = new OrganizationRepository {
    type Session = managementDao.Session

    def saveOrganization(org: Organization)(implicit session: this.type#Session): Organization = {
      managementDao.saveOrganizationRef(org.organizationRef)
      org.domains.foreach(managementDao.saveDomain(_, org.id))
      org.admins.foreach(a => managementDao.associateAdmin(org.id, a.id))
      org
    }

    def updateOrganization(id: UUID, f: Organization => Organization)(implicit session: this.type#Session): Organization = {
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

    def getOrganizationRef(id: UUID)(implicit session: this.type#Session): Option[OrganizationRef] = managementDao.getOrganizationRef(id)

    def getOrganization(id: UUID)(implicit session: this.type#Session): Option[Organization] = managementDao.getOrganization(id)

    def getOrganizationByName(name: String)(implicit session: this.type#Session): Option[OrganizationRef] = managementDao.getOrganizationRefByName(name)

    def addAdminToOrganization(adminID: UUID, orgID: UUID)(implicit session: this.type#Session) = managementDao.associateAdmin(orgID, adminID)

    def getAdminUserRef(id: UUID)(implicit session: this.type#Session): Option[AdminUserRef] = managementDao.getAdminUserRef(id)

    def getAdminUserRefByEmail(email: String)(implicit session: this.type#Session): Option[AdminUserRef] = managementDao.getAdminUserRefByEmail(email)

    def getAdminUserRefByUsername(username: String)(implicit session: this.type#Session): Option[AdminUserRef] = managementDao.getAdminUserRefByUsername(username)

    def saveAdminUser(user: AdminUserRef)(implicit session: this.type#Session): AdminUserRef = managementDao.saveAdminUserRef(user)

    def updateAdminUser(adminUser: AdminUserRef)(implicit session: this.type#Session): AdminUserRef = managementDao.updateAdminUserRef(adminUser)

    def getDeviceRef(id: UUID)(implicit session: this.type#Session): Option[DeviceRef] = managementDao.getDeviceRef(id)

    def saveDeviceRef(device: DeviceRef)(implicit session: this.type#Session): DeviceRef = managementDao.saveDeviceRef(device)

    def updateDeviceRef(device: DeviceRef)(implicit session: this.type#Session): DeviceRef = managementDao.updateDeviceRef(device)

    def updateDevice(id: UUID, f: Device => Device)(implicit session: this.type#Session): Device = {
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

    def saveDevice(device: Device)(implicit session: this.type#Session): Device = {
      managementDao.saveDeviceRef(device.deviceRef)
      device.permissions.foreach(permission => managementDao.saveACL(device.id, permission))
      device
    }

    def getAdminUserByUsername(username: String)(implicit session: this.type#Session): Option[AdminUser] = managementDao.getAdminUserByUsername(username)

    def getAdminUserByEmail(email: String)(implicit session: this.type#Session): Option[AdminUser] = managementDao.getAdminUserByEmail(email)

    def getAdminUser(id: UUID)(implicit session: this.type#Session): Option[AdminUser] = managementDao.getAdminUser(id)

    def getDevice(id: UUID)(implicit session: this.type#Session): Option[Device] = managementDao.getDevice(id)

    def getDomain(id: UUID)(implicit session: this.type#Session): Option[Domain] = managementDao.getDomain(id, registry)
  }

}

trait UserRepositoryDaoComponent extends UserRepositoryComponent {
  this: ManagementDaoComponent =>
  val userRepository: UserRepository = new UserRepository {
    type Session = managementDao.Session

    def getUserRefByEmail(email: String)(implicit session: this.type#Session): Option[UserRef] = managementDao.getUserRefByEmail(email)

    def getUserRefByUsername(username: String)(implicit session: this.type#Session): Option[UserRef] = managementDao.getUserRefByUsername(username)

    def getUserRef(id: UUID)(implicit session: this.type#Session): Option[UserRef] = managementDao.getUserRef(id)

    def saveUserRef(user: UserRef)(implicit session: this.type#Session): UserRef = managementDao.saveUserRef(user)

    def updateUserRef(user: UserRef)(implicit session: this.type#Session): UserRef = managementDao.updateUserRef(user)

    def getUser(id: UUID)(implicit session: this.type#Session): Option[User] = managementDao.getUser(id)

    def getUserByUsername(username: String)(implicit session: this.type#Session): Option[User] = managementDao.getUserByUsername(username)

    def getUserByEmail(email: String)(implicit session: this.type#Session): Option[User] = managementDao.getUserByEmail(email)
  }

  trait ACLServiceComponentDao extends ACLServiceComponent {
    this: ManagementDaoComponent =>
    val aclService = new ACLService {
      type Session = managementDao.Session

      def truncateServicePermissions(clientID: UUID, serviceName: String)(implicit session: this.type#Session): Unit = managementDao.dropServiceACLS(clientID, serviceName)
    }
  }

}