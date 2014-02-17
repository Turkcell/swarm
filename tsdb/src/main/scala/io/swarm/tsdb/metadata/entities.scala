package io.swarm.tsdb.metadata

import java.util.UUID
import io.swarm.domain.{IDEntity, ResourceRef}
import scala.concurrent._

/**
 * Created by Anil Chalil on 12/18/13.
 */
object SeriesType extends Enumeration {
  type SeriesType = Value
  val Long, Float = Value
}

case class Series(id: UUID, key: String, name: Option[String], tags: Set[String], attributes: Map[String, String], `type`: SeriesType.SeriesType) extends IDEntity

case class DatabaseInfo(id: UUID, name: String, version: Int) extends DatabaseRef

case class Database(id: UUID, name: String, metadata: DatabaseMetadata, version: Int) extends DatabaseRef {
  def databaseInfo = new DatabaseInfo(id, name, version)
}

trait DatabaseRef extends ResourceRef

case class DatabaseMetadata(val oauthTTL: Long)


trait DatabaseRepositoryComponent {

  trait DatabaseRepository {
    def saveDatabase(database: Database, domainID: UUID): Database

    def getDatabaseInfo(id: UUID): Option[DatabaseInfo]

    def getDatabaseByName(dbName: String, orgID: UUID): Option[Database]

    /*
    def saveSeries(orgName: String, dbName: String, series: Series): Series

    def getSeriesByID(orgName: String, dbName: String, id: UUID): Option[Series]

    def getSeriesByKey(orgName: String, dbName: String, id: UUID): Option[Series]
 */
  }

}