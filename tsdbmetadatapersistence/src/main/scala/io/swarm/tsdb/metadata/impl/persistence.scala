package io.swarm.tsdb.metadata.impl

import io.swarm.infrastructure.persistence.slick.SlickProfileComponent
import io.swarm.management.impl.ManagementResourceDaoComponent
import io.swarm.management
import java.util.UUID
import scala.slick.jdbc.StaticQuery


/**
 * Created by Anil Chalil on 12/18/13.
 */

trait DatabaseMetadataDaoComponent {
  this: SlickProfileComponent with ManagementResourceDaoComponent =>
  val databaseMetadataDao: DatabaseMetadataDao


  trait DatabaseMetadataDao {

    import profile.simple._
    import Database.dynamicSession

    // Definition of the databases table
    class Databases(tag: Tag) extends Table[(String, String, String, Long, Boolean, Int)](tag, "DATABASES") {
      // This is the primary key column
      def id = column[String]("DB_ID", O.PrimaryKey)

      def name = column[String]("DB_NAME")

      def domain = column[String]("DOMAIN_ID")

      def oauthTTL = column[Long]("OAUTH_TTL", O.Default(0L))

      def deleted = column[Boolean]("DELETED", O.Default(false))

      def version = column[Int]("VERSION", O.Default(0))

      // Every table needs a * projection with the same type as the table's type parameter
      def * = (id, name, domain, oauthTTL, deleted, version)

      def index_name = index("idx_dbname", name, unique = false)

      def fk_org = foreignKey("database_org_fk", domain, managementResourceDao.organizations)(_.id)
    }

    val databases = TableQuery[Databases]

    // Definition of the Series table
    class Series(tag: Tag) extends Table[(String, String, String, Option[String], String, Boolean)](tag, "SERIES") {
      // This is the primary key column
      def id = column[String]("SERIES_ID", O.PrimaryKey)

      def dbID = column[String]("DB_ID")

      def key = column[String]("SERIES_KEY")

      def name = column[Option[String]]("SERIES_NAME")

      def seriesType = column[String]("SERIES_TYPE")

      def deleted = column[Boolean]("DELETED", O.Default(false))

      // Every table needs a * projection with the same type as the table's type parameter
      def * = (id, dbID, key, name, seriesType, deleted)

      def index_key = index("idx_serieskey", key, unique = false)

      def fk_db = foreignKey("series_db_fk", dbID, databases)(_.id)
    }

    val series = TableQuery[Series]

    class Tags(tag: Tag) extends Table[(String, String)](tag, "SERIES_TAGS") {
      def seriesID = column[String]("SERIES_ID")

      def name = column[String]("TAG_NAME")

      def * = (seriesID, name)

      def fk_series = foreignKey("series_tag_fk", seriesID, series)(_.id)

      def index_name = index("idx_tagsname", name, unique = false)
    }

    val tags = TableQuery[Tags]

    class Attributes(tag: Tag) extends Table[(String, String, String)](tag, "SERIES_ATTRIBUTES") {
      def seriesID = column[String]("SERIES_ID")

      def name = column[String]("ATTRIBUTE_NAME")

      def value = column[String]("ATTRIBUTE_VALUE")

      def * = (seriesID, name, value)

      def fk_series = foreignKey("series_attributes_fk", seriesID, series)(_.id)

      def index_name = index("idx_attributesname", name, unique = false)
    }

    val attributes = TableQuery[Attributes]

    def databaseByID(d: Databases, id: String) = (d.id is id) && (d.deleted is false)

    def databaseByID(d: Databases, id: Column[String]) = (d.id is id) && (d.deleted is false)

    def databaseByName(d: Databases, name: String) = (d.name is name) && (d.deleted is false)

    def databaseByName(d: Databases, name: Column[String]) = (d.name is name) && (d.deleted is false)
    val databaseByIDQuery = for {
      id <- Parameters[String]
      o <- organizations
      d <- databases if databaseByID(d, id) && organizationByID(o, d.organization)
    } yield (d.name, d.oauthTTL, d.version)

    val databaseInfoByIDQuery = for {
      id <- Parameters[String]
      o <- organizations
      d <- databases if databaseByID(d, id) && organizationByID(o, d.organization)
    } yield (d.name, d.version)

    val databaseByNameOrgIDQuery = for {
      (name, orgID) <- Parameters[(String, String)]
      o <- organizations
      d <- databases if databaseByName(d, name) && organizationByID(o, d.organization) && (d.organization is orgID)
    } yield (d.id, d.oauthTTL, d.version)

    val databaseInfoByNameOrgIDQuery = for {
      (name, orgID) <- Parameters[(String, String)]
      o <- organizations
      d <- databases if databaseByName(d, name) && organizationByID(o, d.organization) && (d.organization is orgID)
    } yield (d.id, d.version)

    val seriesByIDQuery = for {
      id <- Parameters[String]
      t <- tags
      a <- attributes
      s <- series if (s.id is id) && (s.id is t.seriesID) && (s.id is a.seriesID) && (s.deleted is false)
    } yield (s.name, s.key, t.name, a.name, a.value, s.seriesType)

    val seriesByKeyQuery = for {
      (key, dbID) <- Parameters[(String, String)]
      t <- tags
      a <- attributes
      s <- series if (s.key is key) && (s.dbID is dbID) && (s.id is t.seriesID) && (s.id is a.seriesID) && (s.deleted is false)
    } yield (s.name, s.id, t.name, a.name, a.value, s.seriesType)

    def saveDatabase(database: management.Database, orgID: UUID) = {
      if (databaseByNameOrgIDQuery(database.name, orgID.toString).firstOption.isDefined)
        throw domain.DuplicateIDEntity(s"database with ${database.name} already exist!")
      databases.map(d => (d.id, d.name, d.organization, d.oauthTTL)) +=(database.id.toString, database.name, orgID.toString, database.metadata.oauthTTL)
      database
    }

    def deleteDatabase(dbID: UUID) {
      val database = for (d <- databases if d.id is dbID.toString) yield d.deleted
      database.update(true)
    }


    def getDatabase(dbID: UUID) = {
      databaseByIDQuery(dbID.toString).firstOption.map(d => management.Database(dbID, d._1, domain.DatabaseMetadata(d._2), d._3))
    }

    def getDatabaseInfo(dbID: UUID) = {
      databaseInfoByIDQuery(dbID.toString).firstOption.map(d => domain.DatabaseInfo(dbID, d._1, d._2))
    }

    def getDatabaseByName(name: String, orgID: UUID) = {
      databaseByNameOrgIDQuery(name, orgID.toString).firstOption.map(d => domain.Database(UUID.fromString(d._1), name, domain.DatabaseMetadata(d._2), d._3))
    }

    def getDatabaseInfoByName(name: String, orgID: UUID) = {
      databaseInfoByNameOrgIDQuery(name, orgID.toString).firstOption.map(d => domain.DatabaseInfo(UUID.fromString(d._1), name, d._2))
    }

    def checkEmpty[T, A](list: List[A])(body: => T): Option[T] = {
      if (list == Nil)
        None
      else Some(body)
    }

    def getSeries(seriesID: UUID) = {
      val list = seriesByIDQuery(seriesID.toString).list
      checkEmpty(list) {
        val tags = (for (s <- list) yield s._3).toSet
        val attributes = (for (s <- list) yield (s._4, s._5)).toMap
        domain.Series(seriesID, list.head._2, list.head._1, tags, attributes, SeriesType.withName(list.head._6))
      }
    }

    def getSeriesSet(seriesID: Set[UUID]) = {
      if (seriesID.isEmpty) Set[domain.Series]()
      else {
        val list = (for {
          t <- tags
          a <- attributes
          s <- series if (s.id inSet seriesID.map(_.toString)) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
        } yield (s.id, s.key, s.name, t.name, a.name, a.value, s.seriesType)).list
        extractSeries(list).toSet
      }
    }


    private def extractSeries(list: List[(String, String, Option[String], String, String, String, String)]): List[domain.Series] = {
      if (list.isEmpty)
        Nil
      else {
        val series = list.groupBy(_._1)
        series.map {
          case (seriesID, data) =>
            val tags = (for (s <- data) yield s._4).toSet
            val attributes = (for (s <- data) yield (s._5, s._6)).toMap
            domain.Series(UUID.fromString(seriesID), data.head._2, data.head._3, tags, attributes, SeriesType.withName(data.head._7))
        }.toList
      }
    }

    def getSeriesByKey(key: String, dbID: UUID) = {
      val list = seriesByKeyQuery(key, dbID.toString).list
      checkEmpty(list) {
        val name = list.head._1
        val id = UUID.fromString(list.head._2)
        val tags = (for (s <- list) yield s._3).toSet
        val attributes = (for (s <- list) yield (s._4, s._5)).toMap
        domain.Series(id, key, name, tags, attributes, SeriesType.withName(list.head._6))
      }
    }

    def getSeriesByKeys(keys: Set[String], dbID: UUID) = {
      val list = (for {
        t <- tags
        a <- attributes
        s <- series if (s.key inSet keys.map(_.toString)) && (s.dbID is dbID.toString) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
      } yield (s.id, s.key, s.name, t.name, a.name, a.value, s.seriesType)).list
      extractSeries(list).toSet
    }

    def getSeriesIdsByKeys(keys: Set[String], dbID: UUID) = {
      (for {
        t <- tags
        a <- attributes
        s <- series if (s.key inSet keys.map(_.toString)) && (s.dbID is dbID.toString) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
      } yield s.id).list.toSet
    }

    def insertTags(seriesID: String, tagsValues: Set[String]) {
      tags ++= (for (t <- tagsValues) yield (seriesID, t)).toSeq
    }

    def deleteTags(seriesID: UUID) {
      (for (t <- tags if t.seriesID is seriesID.toString) yield t).delete
    }

    def deleteAttributes(seriesID: UUID) {
      (for (a <- attributes if a.seriesID is seriesID.toString) yield a).delete
    }

    def insertAttributes(seriesID: String, attributesValues: Map[String, String]) {
      attributes ++= (for ((attr, value) <- attributesValues) yield (seriesID, attr, value)).toSeq
    }

    def saveSeries(seriesValue: domain.Series, dbID: UUID) {
      if (seriesByKeyQuery(seriesValue.key, dbID.toString).firstOption.isDefined)
        throw domain.DuplicateIDEntity(s"series with key ${seriesValue.key} is alread defined!")
      else {
        val seriesID: String = seriesValue.id.toString
        series.map(s => (s.id, s.dbID, s.key, s.name, s.seriesType)) +=(seriesID, dbID.toString, seriesValue.key, seriesValue.name, seriesValue.`type`.toString)
        insertTags(seriesID, seriesValue.tags)
        insertAttributes(seriesID, seriesValue.attributes)
      }
    }

    def updateSeries(seriesValue: domain.Series) = {
      val q = for {s <- series if (s.id is seriesValue.id.toString) && (s.deleted is false)} yield s.name
      val count = q.update(seriesValue.name)
      if (count > 0) {
        None
      } else {
        deleteTags(seriesValue.id)
        deleteAttributes(seriesValue.id)
        insertTags(seriesValue.id.toString, seriesValue.tags)
        insertAttributes(seriesValue.id.toString, seriesValue.attributes)
        Some(seriesValue)
      }
    }

    private def getSeriesByTagsInner(tags: Set[String], dbID: UUID) = {
      val inner = """SELECT s."SERIES_ID" FROM SERIES AS s
                  JOIN SERIES_TAGS AS t ON s."SERIES_ID" = t."SERIES_ID"
                  WHERE s."DB_ID"=?  and s."DELETED"=false and t."TAG_NAME"
                  IN """ + tags.map("'" + _ + "'").mkString("(", ",", ")") +
        """ GROUP BY s."SERIES_ID" HAVING count(DISTINCT t."TAG_NAME") = """ + tags.size
      //TODO convert to inner query
      Q.query[String, String](inner).list(dbID.toString).toSet
    }

    def getSeriesByTags(tags: Set[String], dbID: UUID) = getSeriesSet(getSeriesByTagsInner(tags, dbID).map(UUID.fromString))

    private def getSeriesByAttributesInner(attributes: Map[String, String], dbID: UUID) = {
      val start = """select distinct s."SERIES_ID"
    from SERIES AS s
                  """
      val end =
        """
      where s."DB_ID"=? and s."DELETED"=false """
      val query = start + attributes.zipWithIndex.map(v => s"""inner join SERIES_ATTRIBUTES a${v._2} on a${v._2}.series_id=s.series_id and a${v._2}.\"ATTRIBUTE_NAME\"='${v._1._1}' and a${v._2}.\"ATTRIBUTE_VALUE\"='${v._1._2}'""").mkString("\n") + end
      Q.query[String, String](query).list(dbID.toString).toSet
    }

    def getSeriesByAttributes(attributes: Map[String, String], dbID: UUID) = getSeriesSet(getSeriesByAttributesInner(attributes, dbID).map(UUID.fromString))


    def getSeriesIntersection(ids: Set[UUID], keys: Set[String], tags: Set[String], attributes: Map[String, String], dbID: UUID) = {
      def inner(previous: Option[Set[String]], in: Iterable[_], f: () => Set[String]) = {
        if (in.isEmpty) previous else if (previous.isDefined) Some(previous.get.intersect(f())) else Some(f())
      }
      var result = inner(None, ids, () => ids.map(_.toString))
      result = inner(result, keys, () => getSeriesIdsByKeys(keys, dbID))
      result = inner(result, tags, () => getSeriesByTagsInner(tags, dbID))
      result = inner(result, attributes, () => getSeriesByAttributesInner(attributes, dbID))
      result.map(x => getSeriesSet(x.map(UUID.fromString))).getOrElse(Set[domain.Series]())
    }

    def deleteAllSeries(dbID: String) {
      val seriesQ = for (s <- series if (s.dbID is dbID) && (s.deleted is false)) yield s.deleted
      seriesQ.update(true)
    }

    def extractDatabases(orgs: List[(String, Int, Option[String], Option[String], Option[Long], Option[Int])]): Set[domain.Database] = orgs.collect {
      case (_, _, Some(id), Some(name), Some(ttl), Some(version)) => domain.Database(UUID.fromString(id), name, domain.DatabaseMetadata(ttl), version)
    }.toSet
  }

}