package io.swarm.domain.persistence.slick

import io.swarm.domain._
import java.util.UUID
import io.swarm.domain.Device
import io.swarm.domain.DatabaseUser
import io.swarm.infrastructure.persistence.PersistenceSessionComponent

/**
 * Created by Anil Chalil on 12/6/13.
 */
trait ClientRepositoryComponentSlick extends ClientRepositoryComponent {
  this: ClientResourceDaoComponent with PersistenceSessionComponent =>

  val clientRepository = new ClientRepository {
    def getDatabaseUserByUsername(username: String): Option[UserInfo] = clientResourceDao.getUserByUsername(username)

    def saveDatabaseUser(user: DatabaseUser): Unit = clientResourceDao.saveUser(user)

    def getAdminUser(uuid: UUID): Option[UserInfo] = clientResourceDao.getAdminByID(uuid)

    def getDatabaseUserByEmail(email: String): Option[UserInfo] = clientResourceDao.getUserByEmail(email)

    def saveAdminUser(user: AdminUser): Unit = clientResourceDao.saveAdminUser(user)

    def saveDevice(device: Device): Unit = clientResourceDao.saveDevice(device)

    def getAdminUserByUsername(username: String): Option[UserInfo] = clientResourceDao.getAdminByUsername(username)

    def getAdminUserByEmail(email: String): Option[UserInfo] = clientResourceDao.getAdminByEmail(email)

    def getDatabaseUser(uuid: UUID): Option[UserInfo] = clientResourceDao.getUserByID(uuid)

    def getDevice(uuid: UUID): Option[Device] = clientResourceDao.getDeviceByID(uuid)

    def updateAdminUser(adminUser: AdminUser): Unit = clientResourceDao.updateAdminUser(adminUser)

    def updateDatabaseUser(databaseUser: DatabaseUser): Unit = clientResourceDao.updateUser(databaseUser)

    def updateDevice(device: Device): Unit = clientResourceDao.updateDevice(device)
  }

}
