package io.swarm.domain.persistence.slick

import io.swarm.domain._
import java.util.UUID
import io.swarm.domain.Organization
import io.swarm.infrastructure.persistence.PersistenceSessionComponent

/**
 * Created by Anil Chalil on 12/4/13.
 */
trait ResourceRepositoryComponentJDBC extends ResourceRepositoryComponent {
  this: ClientResourceDaoComponent with PersistenceSessionComponent =>



  val resourceRepository: ResourceRepository = new ResourceRepository {
    def saveOrganization(org: Organization): Organization = {
      clientResourceDao.saveOrganization(org)
      org
    }

    def getDatabaseInfo(id: UUID): Option[DatabaseInfo] = clientResourceDao.getDatabaseInfo(id)

    def getOrganization(id: UUID): Option[Organization] = clientResourceDao.getOrganizationByID(id)

    def getOrganizationByName(name: String): Option[Organization] = clientResourceDao.getOrganizationByName(name)

    def saveDatabase(database: Database, orgID: UUID): Database = {
      if (clientResourceDao.getOrganizationInfoByID(orgID).isDefined)
        clientResourceDao.saveDatabase(database, orgID)
      else
        throw IDEntityNotFound(s"organization with id ${orgID} not found!")
    }

    def getDatabaseByName(dbName: String, orgID: UUID): Option[Database] = clientResourceDao.getDatabaseByName(dbName, orgID)

    def addAdminToOrganization(adminID: UUID, orgID: UUID) {
      if (!clientResourceDao.isAdminExist(adminID))
        throw IDEntityNotFound(s"admin with id ${adminID} not found!")
      if (clientResourceDao.getOrganizationInfoByID(orgID).isEmpty)
        throw IDEntityNotFound(s"organization with id ${orgID} not found!")
      clientResourceDao.addAdminToOrganization(adminID, orgID)
    }
  }
}
