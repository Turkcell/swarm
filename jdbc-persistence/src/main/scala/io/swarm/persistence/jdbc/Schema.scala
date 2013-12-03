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

    def index_name = index("idx_orgname", (name), unique = false)

    val organizationByID = for {
      id <- Parameters[String]
      d <- Databases
      o <- this if (o.id is id) && (o.deleted is false) && (d.organization is id) && (d.deleted is false)
    } yield (o.name, d.id, d.name, d.oauthTTL)

    val organizationByName = for {
      name <- Parameters[String]
      d <- Databases
      o <- this if (o.name is name) && (o.deleted is false) && (d.organization is o.id) && (d.deleted is false)
    } yield (o.id, d.id, d.name, d.oauthTTL)
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

    def index_name = index("idx_dbname", (name), unique = false)

    def fk_org = foreignKey("database_org_fk", organization, Organizations)(_.id)

    val databaseByID = for {
      id <- Parameters[String]
      d <- this if (d.id is id) && (d.deleted is false)
    } yield (d.name, d.oauthTTL)

    val databaseByName = for {
      name <- Parameters[String]
      d <- this if (d.name is name) && (d.deleted is false)
    } yield (d.id, d.oauthTTL)
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

    def index_key = index("idx_serieskey", (key), unique = false)

    def fk_db = foreignKey("series_db_fk", dbID, Databases)(_.id)

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
  }

  object Tags extends Table[(String, String)]("SERIES_TAGS") {
    def seriesID = column[String]("SERIES_ID")

    def name = column[String]("TAG_NAME")

    def * = seriesID ~ name

    def fk_series = foreignKey("series_tag_fk", seriesID, Series)(_.id)

    def index_name = index("idx_tagsname", (name), unique = false)
  }

  object Attributes extends Table[(String, String, String)]("SERIES_ATTRIBUTES") {
    def seriesID = column[String]("SERIES_ID")

    def name = column[String]("ATTRIBUTE_NAME")

    def value = column[String]("ATTRIBUTE_VALUE")

    def * = seriesID ~ name ~ value

    def fk_series = foreignKey("series_attributes_fk", seriesID, Series)(_.id)

    def index_name = index("idx_attributesname", (name), unique = false)
  }

  object Devices extends Table[(String, String, String, Boolean, Boolean, Boolean)]("DEVICES") {
    def id = column[String]("DEVICE_ID", O.PrimaryKey)

    def deviceID = column[String]("DEVICE_OWN_ID")

    def dbID = column[String]("DB_ID")

    def activated = column[Boolean]("ACTIVATED")

    def disabled = column[Boolean]("DISABLED")

    def deleted = column[Boolean]("DELETED", O.Default(false))

    def * = id ~ deviceID ~ dbID ~ activated ~ disabled ~ deleted

    def fk_database = foreignKey("devices_db_fk", dbID, Databases)(_.id)

    def index_deviceID = index("idx_devicesdeviceID", (deviceID), unique = false)

    val deviceByID = for {
      id <- Parameters[String]
      db <- Databases
      p <- DevicePermissions
      d <- this if (d.id is id) && (d.deleted is false) && (db.id is d.dbID) && (p.id is d.id)
    } yield (d.deviceID, d.activated, d.disabled, p.devicePermission, db.id, db.name)

    val deviceByDeviceID = for {
      deviceID <- Parameters[String]
      db <- Databases
      p <- DevicePermissions
      d <- this if (d.deviceID is deviceID) && (d.deleted is false) && (db.id is d.dbID) && (p.id is d.id)
    } yield (d.id, d.activated, d.disabled, p.devicePermission, db.id, db.name)
  }

  object DevicePermissions extends Table[(String, String)]("DEVICE_PERMISSIONS") {
    def id = column[String]("DEVICE_ID")

    def devicePermission = column[String]("DEVICE_PERMISSION")

    def * = id ~ devicePermission

    def fk_device = foreignKey("device_permissions_device_fk", id, Devices)(_.id)
  }

  object AdminUsers extends Table[(String, Option[String], Option[String], String, String, String, Boolean, Boolean, Boolean, Boolean)]("ADMIN_USERS") {
    def id = column[String]("ID", O.PrimaryKey)

    def name = column[Option[String]]("NAME")

    def surname = column[Option[String]]("SURNAME")

    def username = column[String]("USERNAME")

    def email = column[String]("EMAIL")

    def credential = column[String]("CREDENTIAL")

    def activated = column[Boolean]("ACTIVATED")

    def confirmed = column[Boolean]("CONFIRMED")

    def disabled = column[Boolean]("DISABLED")

    def deleted = column[Boolean]("DELETED", O.Default(false))

    def * = id ~ name ~ surname ~ username ~ email ~ credential ~ activated ~ confirmed ~ disabled ~ deleted

    def index_email = index("idx_adminusersemail", (email), unique = true)

    def index_username = index("idx_adminusersusername", (username), unique = true)

    /*val adminByID = for {
      id <- Parameters[String]
      (((a, _), o), d) <- AdminUsers.where(a=> (a.id is id) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)*/

    /*val adminByEmail = for {
      email <- Parameters[String]
      (((a, _), o), d) <- AdminUsers.where(a => (a.email is email) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.id, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)*/

    /*val adminByUsername = for {
      username <- Parameters[String]
      (((a, _), o), d) <- AdminUsers.where(a => (a.username is username) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.id, a.email, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)*/
  }

  object OrganizationAdmins extends Table[(String, String)]("ORGANIZATION_ADMINS") {
    def orgID = column[String]("ORGID")

    def adminID = column[String]("ADMINID")

    def * = orgID ~ adminID

    def fk_organization = foreignKey("organization_admins_organization_fk", orgID, Organizations)(_.id)

    def fk_admin = foreignKey("organization_admins_admin_fk", adminID, AdminUsers)(_.id)
  }

  def create(implicit session: Session) {
    (Organizations.ddl ++ Databases.ddl ++ Series.ddl ++ Tags.ddl ++ Attributes.ddl ++ Devices.ddl ++ DevicePermissions.ddl ++ AdminUsers.ddl ++ OrganizationAdmins.ddl).create
  }

  def drop(implicit session: Session) {
    (Organizations.ddl ++ Databases.ddl ++ Series.ddl ++ Tags.ddl ++ Attributes.ddl ++ Devices.ddl ++ DevicePermissions.ddl ++ AdminUsers.ddl ++ OrganizationAdmins.ddl).drop
  }

  def saveDatabase(database: domain.Database, org: OrganizationRef)(implicit session: Session) {
    if (Databases.databaseByName(database.name).firstOption.isDefined)
      throw domain.DuplicateIDEntity(s"database with ${database.name} already exist!")
    (Databases.id ~ Databases.name ~ Databases.organization ~ Databases.oauthTTL).insert(database.id.toString, database.name, org.id.toString, database.metadata.oauthTTL)
  }

  def deleteDatabase(dbID: UUID)(implicit session: Session) {
    val database = for (d <- Databases if d.id is dbID.toString) yield d.deleted
    database.update(true)
  }


  def getDatabase(dbID: UUID)(implicit session: Session) = {
    Databases.databaseByID(dbID.toString).firstOption.map(d => domain.Database(dbID, d._1, domain.DatabaseMetadata(d._2)))
  }

  def getDatabaseByName(name: String)(implicit session: Session) = {
    Databases.databaseByName(name).firstOption.map(d => domain.Database(UUID.fromString(d._1), name, domain.DatabaseMetadata(d._2)))
  }

  def checkEmpty[T, A](list: List[A])(body: => T): Option[T] = {
    if (list == Nil)
      None
    else Some(body)
  }

  def getSeries(seriesID: UUID)(implicit session: Session) = {
    val list = Series.seriesByID(seriesID.toString).list
    checkEmpty(list) {
      val tags = (for (s <- list) yield s._3).toSet
      val attributes = (for (s <- list) yield (s._4, s._5)).toMap
      domain.Series(seriesID, list.head._2, list.head._1, tags, attributes, SeriesType.withName(list.head._6))
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
    checkEmpty(list) {
      val name = list.head._1
      val id = UUID.fromString(list.head._2)
      val tags = (for (s <- list) yield s._3).toSet
      val attributes = (for (s <- list) yield (s._4, s._5)).toMap
      domain.Series(id, key, name, tags, attributes, SeriesType.withName(list.head._6))
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

  def saveSeries(series: domain.Series, dbID: UUID)(implicit session: Session) {
    if (Series.seriesByKey(series.key, dbID.toString).firstOption.isDefined)
      throw domain.DuplicateIDEntity(s"series with key ${series.key} is alread defined!")
    else {
      val seriesID: String = series.id.toString
      (Series.id ~ Series.dbID ~ Series.key ~ Series.name ~ Series.seriesType).insert(seriesID, dbID.toString, series key, series.name, series.`type`.toString)
      insertTags(seriesID, series.tags)
      insertAttributes(seriesID, series.attributes)
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
    checkEmpty(orgs) {
      domain.Organization(orgID, orgs.head._1, orgs.map(o => domain.Database(UUID.fromString(o._2), o._3, domain.DatabaseMetadata(o._4))).toSet)
    }
  }

  def getOrganizationByName(orgName: String)(implicit session: Session) = {
    val orgs = Organizations.organizationByName(orgName).list
    checkEmpty(orgs) {
      domain.Organization(UUID.fromString(orgs.head._1), orgName, orgs.map(o => domain.Database(UUID.fromString(o._2), o._3, domain.DatabaseMetadata(o._4))).toSet)
    }
  }

  def saveOrganization(orgRef: OrganizationRef)(implicit session: Session) {
    if (Organizations.organizationByName(orgRef.name).firstOption.isDefined)
      throw domain.DuplicateIDEntity(s"organization with ${orgRef.name} is already defined!")
    else
      (Organizations.id ~ Organizations.name).insert(orgRef.id.toString, orgRef.name)
  }

  def getDeviceByID(id: UUID)(implicit session: Session) = {
    val devices = Devices.deviceByID(id.toString).list
    checkEmpty(devices) {
      val head = devices.head
      domain.Device(id, head._1, domain.DatabaseInfo(UUID.fromString(head._5), head._6), head._2, head._3, devices.map(_._4).toSet)
    }
  }

  def getDeviceByDeviceID(deviceID: String)(implicit session: Session) = {
    val devices = Devices.deviceByDeviceID(deviceID).list
    checkEmpty(devices) {
      val head = devices.head
      domain.Device(UUID.fromString(head._1), deviceID, domain.DatabaseInfo(UUID.fromString(head._5), head._6), head._2, head._3, devices.map(_._4).toSet)
    }
  }

  def saveDevice(device: domain.Device)(implicit session: Session) = {
    if (Devices.deviceByDeviceID(device.deviceID).firstOption.isDefined)
      throw domain.DuplicateIDEntity(s"device with ${device.deviceID} is already defined!")
    else {
      (Devices.id ~ Devices.deviceID ~ Devices.activated ~ Devices.disabled ~ Devices.dbID).insert(device.id.toString, device.deviceID, device.activated, device.disabled, device.databaseRef.id.toString)
      device.permissions.foreach(p => (DevicePermissions.id ~ DevicePermissions.devicePermission).insert(device.id.toString, p))
    }
  }

  protected def extractOrganizationFromAdminList(admins: List[(Option[String], Option[String], String, String, String, Boolean, Boolean, Boolean, Option[String], Option[String], Option[String], Option[String], Option[Long])]): Set[domain.Organization] = {

    admins.groupBy(_._9).map {
      r =>
        (r._1, r._2.head._10) match {
          case (None, None) => None
          case (Some(orgID), Some(orgName)) =>
            Some(domain.Organization(UUID.fromString(orgID), orgName, r._2.map {
              d =>
                (d._11, d._12, d._13) match {
                  case (None, None, None) => None
                  case (Some(dbid), Some(dbname), Some(oauthttl)) => Some(domain.Database(UUID.fromString(dbid), dbname, domain.DatabaseMetadata(oauthttl)))
                  case _ => throw new IllegalArgumentException(s"illegal row ${r._2}")
                }
            }.flatten.toSet))
          case _ => throw new IllegalArgumentException(s"illegal row ${r._2}")
        }
    }.flatten.toSet
  }


  def getAdminByID(id: UUID)(implicit session: Session) = {
    val admins = (for {
      (((a, _), o), d) <- AdminUsers.where(a => (a.id is id.toString) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)).list
    checkEmpty(admins) {
      val head = admins.head
      domain.AdminUser(id, head._1, head._2, head._3, head._4, head._5, head._6, head._7, head._8,
        extractOrganizationFromAdminList(admins))
    }
  }


  def getAdminByEmail(email: String)(implicit session: Session) = {
    val admins = (for {
      (((a, _), o), d) <- AdminUsers.where(a => (a.email is email) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.id, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)).list
    checkEmpty(admins) {
      val head = admins.head
      domain.AdminUser(UUID.fromString(head._4), head._1, head._2, head._3, email, head._5, head._6, head._7, head._8,
        extractOrganizationFromAdminList(admins))
    }
  }

  def getAdminByUsername(username: String)(implicit session: Session) = {
    val admins = (for {
      (((a, _), o), d) <- AdminUsers.where(a => (a.username is username) && (a.deleted is false)) leftJoin OrganizationAdmins on (_.id === _.adminID) leftJoin Organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin Databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.id, a.email, a.credential, a.activated, a.confirmed, a.disabled, o.id.?, o.name.?, d.id.?, d.name.?, d.oauthTTL.?)).list
    checkEmpty(admins) {
      val head = admins.head
      domain.AdminUser(UUID.fromString(head._3), head._1, head._2, username, head._4, head._5, head._6, head._7,
        head._8, extractOrganizationFromAdminList(admins))
    }
  }

  def saveAdminUser(admin: domain.AdminUser)(implicit session: Session) {
    val adminOption = getAdminByEmail(admin.email).map(v => v.email).orElse(getAdminByUsername(admin.username).map(v => v.username))
    if (adminOption.isDefined)
      throw domain.DuplicateIDEntity(s"admin user with ${adminOption.get} is already defined!")
    else {
      (AdminUsers.id ~ AdminUsers.name ~ AdminUsers.surname ~ AdminUsers.username ~ AdminUsers.email ~
        AdminUsers.credential ~ AdminUsers.activated ~ AdminUsers.confirmed ~ AdminUsers.disabled).insert(admin.id.toString,
          admin.name, admin.surname, admin.username, admin.email, admin.credential, admin.activated, admin.confirmed, admin.disabled)
      admin.organizations.foreach(o => (OrganizationAdmins.orgID ~ OrganizationAdmins.adminID).insert(o.id.toString, admin.id.toString))
    }
  }
}


