package io.swarm.persistence.jdbc

import scala.slick.driver.ExtendedProfile
import io.swarm.domain
import java.util.UUID
import scala.slick.jdbc.{GetResult, StaticQuery => Q}
import io.swarm.domain.{SeriesType, OrganizationRef}

/**
 * Created by capacman on 10/25/13.
 */

trait Profile {
  val profile: ExtendedProfile
}

trait MetadataComponent {
  this: Profile =>
  //...to be able import profile.simple._
  import profile.simple._



  // Definition of the Organizations table
  object Organizations extends Table[(String, String, Boolean)]("Organizations") {
    // This is the primary key column
    def id = column[String]("ORG_ID", O.PrimaryKey)

    def name = column[String]("ORG_NAME")

    def deleted = column[Boolean]("DELETED", O.Default(false))

    // Every table needs a * projection with the same type as the table's type parameter
    def * = id ~ name ~ deleted

    val organizationByID = for {
      id <- Parameters[String]
      d <- Databases
      o <- this if (o.id is id) && (o.deleted is false) && (d.organization is id) && (d.deleted is false)
    } yield (o.name, d.id, d.name, d.oauthTTL)
  }

  // Definition of the databases table
  object Databases extends Table[(String, String, String, Long, Boolean)]("DATABASES") {
    // This is the primary key column
    def id = column[String]("DB_ID", O.PrimaryKey)

    def name = column[String]("DB_NAME")

    def organization = column[String]("DB_ORGANIZATION")

    def oauthTTL = column[Long]("OAUTH_TTL", O.Default(0L))

    def deleted = column[Boolean]("DELETED", O.Default(false))

    // Every table needs a * projection with the same type as the table's type parameter
    def * = id ~ name ~ organization ~ oauthTTL ~ deleted

    val databaseByID = for {
      id <- Parameters[String]
      d <- this if (d.id is id) && (d.deleted is false)
    } yield (d.name, d.oauthTTL)
  }


  // Definition of the Series table
  object Series extends Table[(String, String, String, Option[String], String, Boolean)]("SERIES") {
    // This is the primary key column
    def id = column[String]("SERIES_ID", O.PrimaryKey)

    def dbID = column[String]("DB_ID")

    def key = column[String]("SERIES_KEY")

    def name = column[Option[String]]("SERIES_NAME")

    def seriesType = column[String]("SERIES_TYPE")

    def deleted = column[Boolean]("DELETED", O.Default(false))

    // Every table needs a * projection with the same type as the table's type parameter
    def * = id ~ dbID ~ key ~ name ~ seriesType ~ deleted

    val seriesByID = for {
      id <- Parameters[String]
      t <- Tags
      a <- Attributes
      s <- this if (s.id is id) && (s.id is t.seriesID) && (s.id is a.seriesID) && (s.deleted is false)
    } yield (s.name, s.key, t.name, a.name, a.value, s.seriesType)

    val seriesByKey = for {
      (key, dbID) <- Parameters[(String, String)]
      t <- Tags
      a <- Attributes
      s <- this if (s.key is key) && (s.dbID is dbID) && (s.id is t.seriesID) && (s.id is a.seriesID) && (s.deleted is false)
    } yield (s.name, s.id, t.name, a.name, a.value, s.seriesType)


    def database = foreignKey("DATABASES_FK", dbID, Databases)(_.id)
  }

  object Tags extends Table[(String, String)]("SERIES_TAGS") {
    def seriesID = column[String]("SERIES_ID")

    def name = column[String]("TAG_NAME")

    def * = seriesID ~ name

    def series = foreignKey("SERIES_TAG_FK", seriesID, Series)(_.id)
  }

  object Attributes extends Table[(String, String, String)]("SERIES_ATTRIBUTES") {
    def seriesID = column[String]("SERIES_ID")

    def name = column[String]("ATTRIBUTE_NAME")

    def value = column[String]("ATTRIBUTE_VALUE")

    def * = seriesID ~ name ~ value

    def series = foreignKey("SERIES_ATTRIBUTES_FK", seriesID, Series)(_.id)
  }

  def create(implicit session: Session) {
    (Databases.ddl ++ Series.ddl ++ Tags.ddl ++ Attributes.ddl).create
  }

  def drop(implicit session: Session) {
    (Databases.ddl ++ Series.ddl ++ Tags.ddl ++ Attributes.ddl).drop
  }

  def saveDatabase(database: domain.Database, org: OrganizationRef)(implicit session: Session) {
    (Databases.id ~ Databases.name ~ Databases.organization ~ Databases.oauthTTL).insert(database.id.toString, database.name, org.id.toString, database.metadata.oauthTTL)
  }

  def deleteDatabase(dbID: UUID)(implicit session: Session) {
    val database = for (d <- Databases if d.id is dbID.toString) yield d.deleted
    database.update(true)
  }


  def getDatabase(dbID: UUID)(implicit session: Session) = {
    Databases.databaseByID(dbID.toString).firstOption.map(d => domain.Database(dbID, d._1, domain.DatabaseMetadata(d._2)))
  }

  def getSeries(seriesID: UUID)(implicit session: Session) = {
    val list = Series.seriesByID(seriesID.toString).list
    if (list.isEmpty)
      None
    else {
      val tags = (for (s <- list) yield s._3).toSet
      val attributes = (for (s <- list) yield (s._4, s._5)).toMap
      Some(domain.Series(seriesID, list.head._2, list.head._1, tags, attributes, SeriesType.withName(list.head._6)))
    }
  }

  def getSeriesSet(seriesID: Set[UUID])(implicit session: Session) = {
    if (seriesID.isEmpty) Set[domain.Series]()
    else {
      val list = (for {
        t <- Tags
        a <- Attributes
        s <- Series if (s.id inSet seriesID.map(_.toString)) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
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

  def getSeriesByKey(key: String, dbID: UUID)(implicit session: Session) = {
    val list = Series.seriesByKey(key, dbID.toString).list
    if (list.isEmpty)
      None
    else {
      val name = list.head._1
      val id = UUID.fromString(list.head._2)
      val tags = (for (s <- list) yield s._3).toSet
      val attributes = (for (s <- list) yield (s._4, s._5)).toMap
      Some(domain.Series(id, key, name, tags, attributes, SeriesType.withName(list.head._6)))
    }
  }

  def getSeriesByKeys(keys: Set[String], dbID: UUID)(implicit session: Session) = {
    val list = (for {
      t <- Tags
      a <- Attributes
      s <- Series if (s.key inSet keys.map(_.toString)) && (s.dbID is dbID.toString) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
    } yield (s.id, s.key, s.name, t.name, a.name, a.value, s.seriesType)).list
    extractSeries(list).toSet
  }

  def getSeriesIdsByKeys(keys: Set[String], dbID: UUID)(implicit session: Session) = {
    (for {
      t <- Tags
      a <- Attributes
      s <- Series if (s.key inSet keys.map(_.toString)) && (s.dbID is dbID.toString) && (s.deleted is false) && (s.id is a.seriesID) && (s.id is t.seriesID)
    } yield s.id).list.toSet
  }

  def insertTags(seriesID: String, tags: Set[String])(implicit session: Session) {
    Tags.insertAll({
      for (t <- tags) yield (seriesID, t)
    }.toArray: _*)
  }

  def deleteTags(seriesID: UUID)(implicit session: Session) {
    (for (t <- Tags if t.seriesID is seriesID.toString) yield t).delete
  }

  def deleteAttributes(seriesID: UUID)(implicit session: Session) {
    (for (a <- Attributes if a.seriesID is seriesID.toString) yield a).delete
  }

  def insertAttributes(seriesID: String, attributes: Map[String, String])(implicit session: Session) {
    Attributes.insertAll({
      for ((attr, value) <- attributes) yield (seriesID, attr, value)
    }.toArray: _*)
  }

  def saveSeries(series: domain.Series, dbID: UUID)(implicit session: Session) = {
    val exists = Query(Series).filter(s => (s.deleted is false) && (s.key is series.key) && (s.dbID is dbID.toString)).map(_.id).firstOption
    if (exists.isDefined)
      None
    else {
      val seriesID: String = series.id.toString
      (Series.id ~ Series.dbID ~ Series.key ~ Series.name ~ Series.seriesType).insert(seriesID, dbID.toString, series key, series.name, series.`type`.toString)
      insertTags(seriesID, series.tags)
      insertAttributes(seriesID, series.attributes)
      series
    }
  }

  def updateSeries(series: domain.Series)(implicit session: Session) = {
    val q = for {s <- Series if s.id is series.id.toString} yield s.name
    val count = q.update(series.name)
    if (count > 0) {
      None
    } else {
      deleteTags(series.id)
      deleteAttributes(series.id)
      insertTags(series.id.toString, series.tags)
      insertAttributes(series.id.toString, series.attributes)
      Some(series)
    }
  }

  private def getSeriesByTagsInner(tags: Set[String], dbID: UUID)(implicit session: Session) = {
    val inner = """SELECT s."SERIES_ID" FROM SERIES AS s
                  JOIN SERIES_TAGS AS t ON s."SERIES_ID" = t."SERIES_ID"
                  WHERE s."DB_ID"=?  and s."DELETED"=false and t."TAG_NAME"
                  IN """ + tags.map("'" + _ + "'").mkString("(", ",", ")") +
      """ GROUP BY s."SERIES_ID" HAVING count(DISTINCT t."TAG_NAME") = """ + tags.size
    //TODO convert to inner query
    Q.query[String, String](inner).list(dbID.toString).toSet
  }

  def getSeriesByTags(tags: Set[String], dbID: UUID)(implicit session: Session) = getSeriesSet(getSeriesByTagsInner(tags, dbID).map(UUID.fromString))

  private def getSeriesByAttributesInner(attributes: Map[String, String], dbID: UUID)(implicit session: Session) = {
    val start = """select distinct s."SERIES_ID"
    from SERIES AS s
                """
    val end =
      """
      where s."DB_ID"=? and s."DELETED"=false """
    val query = start + attributes.zipWithIndex.map(v => s"""inner join SERIES_ATTRIBUTES a${v._2} on a${v._2}.series_id=s.series_id and a${v._2}.\"ATTRIBUTE_NAME\"='${v._1._1}' and a${v._2}.\"ATTRIBUTE_VALUE\"='${v._1._2}'""").mkString("\n") + end
    Q.query[String, String](query).list(dbID.toString).toSet
  }

  def getSeriesByAttributes(attributes: Map[String, String], dbID: UUID)(implicit session: Session) = getSeriesSet(getSeriesByAttributesInner(attributes, dbID).map(UUID.fromString))


  def getSeriesIntersection(ids: Set[UUID], keys: Set[String], tags: Set[String], attributes: Map[String, String], dbID: UUID)(implicit session: Session) = {
    def inner(previous: Option[Set[String]], in: Iterable[_], f: () => Set[String]) = {
      if (in.isEmpty) previous else if (previous.isDefined) Some(previous.get.intersect(f())) else Some(f())
    }
    var result = inner(None, ids, () => ids.map(_.toString))
    result = inner(result, keys, () => getSeriesIdsByKeys(keys, dbID))
    result = inner(result, tags, () => getSeriesByTagsInner(tags, dbID))
    result = inner(result, attributes, () => getSeriesByAttributesInner(attributes, dbID))
    result.map(x => getSeriesSet(x.map(UUID.fromString))).getOrElse(Set[domain.Series]())
  }

  def deleteAllSeries(dbID: String)(implicit session: Session) {
    val series = for (s <- Series if s.dbID is dbID) yield s.deleted
    series.update(true)
  }

  def getOrganizationByID(orgID: UUID)(implicit session: Session) = {
    val orgs = Organizations.organizationByID(orgID.toString).list
    if (orgs == Nil)
      None
    else
      Some(domain.Organization(orgID, orgs.head._1, orgs.map(o => domain.Database(UUID.fromString(o._2), o._3, domain.DatabaseMetadata(o._4))).toSet))
  }
}


