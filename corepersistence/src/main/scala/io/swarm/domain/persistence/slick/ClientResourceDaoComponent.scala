package io.swarm.domain.persistence.slick

import io.swarm.domain
import java.util.UUID
import scala.slick.jdbc.{StaticQuery => Q}
import io.swarm.domain.{IDEntityNotFound, OrganizationInfo, Organization, SeriesType}
import io.swarm.infrastructure.persistence.slick.SlickProfileComponent
import io.swarm.infrastructure.persistence.OptimisticLockingException


trait ClientResourceDaoComponent {
  this: SlickProfileComponent =>

  val clientResourceDao: ClientResourceDao

  trait ClientResourceDao {

    //...to be able import profile.simple._

    import profile.simple._
    import Database.dynamicSession

    // Definition of the Organizations table
    class Organizations(tag: Tag) extends Table[(String, String, Boolean, Int)](tag, "Organizations") {
      // This is the primary key column
      def id = column[String]("ORG_ID", O.PrimaryKey)

      def name = column[String]("ORG_NAME")

      def deleted = column[Boolean]("DELETED", O.Default(false))

      def version = column[Int]("VERSION", O.Default(0))

      // Every table needs a * projection with the same type as the table's type parameter
      def * = (id, name, deleted, version)

      def index_name = index("idx_orgname", name, unique = false)
    }

    val organizations = TableQuery[Organizations]

    // Definition of the databases table
    class Databases(tag: Tag) extends Table[(String, String, String, Long, Boolean, Int)](tag, "DATABASES") {
      // This is the primary key column
      def id = column[String]("DB_ID", O.PrimaryKey)

      def name = column[String]("DB_NAME")

      def organization = column[String]("DB_ORGANIZATION")

      def oauthTTL = column[Long]("OAUTH_TTL", O.Default(0L))

      def deleted = column[Boolean]("DELETED", O.Default(false))

      def version = column[Int]("VERSION", O.Default(0))

      // Every table needs a * projection with the same type as the table's type parameter
      def * = (id, name, organization, oauthTTL, deleted, version)

      def index_name = index("idx_dbname", name, unique = false)

      def fk_org = foreignKey("database_org_fk", organization, organizations)(_.id)
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

    class Devices(tag: Tag) extends Table[(String, String, String, Boolean, Boolean, Boolean, Int)](tag, "DEVICES") {
      def id = column[String]("DEVICE_ID", O.PrimaryKey)

      def deviceID = column[String]("DEVICE_OWN_ID")

      def parent = column[String]("DB_ID")

      def activated = column[Boolean]("ACTIVATED")

      def disabled = column[Boolean]("DISABLED")

      def deleted = column[Boolean]("DELETED", O.Default(false))

      def version = column[Int]("VERSION", O.Default(0))

      def * = (id, deviceID, parent, activated, disabled, deleted, version)

      def fk_database = foreignKey("devices_db_fk", parent, organizations)(_.id)

      def index_deviceID = index("idx_devicesdeviceID", deviceID, unique = false)
    }

    val devices = TableQuery[Devices]

    class DevicePermissions(tag: Tag) extends Table[(String, String)](tag, "DEVICE_PERMISSIONS") {
      def id = column[String]("DEVICE_ID")

      def devicePermission = column[String]("DEVICE_PERMISSION")

      def * = (id, devicePermission)

      def fk_device = foreignKey("device_permissions_device_fk", id, devices)(_.id)
    }

    val devicePermissions = TableQuery[DevicePermissions]

    class AdminUsers(tag: Tag) extends Table[(String, Option[String], Option[String], String, String, String, Boolean, Boolean, Boolean, Boolean, Int)](tag, "ADMIN_USERS") {
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

      def version = column[Int]("VERSION", O.Default(0))

      def * = (id, name, surname, username, email, credential, activated, confirmed, disabled, deleted, version)

      def index_email = index("idx_adminusersemail", email, unique = true)

      def index_username = index("idx_adminusersusername", username, unique = true)
    }

    val adminUsers = TableQuery[AdminUsers]

    class OrganizationAdmins(tag: Tag) extends Table[(String, String)](tag, "ORGANIZATION_ADMINS") {
      def orgID = column[String]("ORGID")

      def adminID = column[String]("ADMINID")

      def * = (orgID, adminID)

      def fk_organization = foreignKey("organization_admins_organization_fk", orgID, organizations)(_.id)

      def fk_admin = foreignKey("organization_admins_admin_fk", adminID, adminUsers)(_.id)
    }

    val organizationAdmins = TableQuery[OrganizationAdmins]

    class DatabaseUsers(tag: Tag) extends Table[(String, Option[String], Option[String], String, String, String, Boolean, Boolean, Boolean, Boolean, Int)](tag, "DATABASE_USERS") {
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

      def version = column[Int]("VERSION", O.Default(0))

      def * = (id, name, surname, username, email, credential, activated, confirmed, disabled, deleted, version)

      def index_email = index("idx_databaseusersemail", email, unique = true)

      def index_username = index("idx_databaseusersusername", username, unique = true)
    }

    val databaseUsers = TableQuery[DatabaseUsers]

    class DatabaseUserPermissions(tag: Tag) extends Table[(String, String)](tag, "DATABASER_USER_PERMISSIONS") {
      def id = column[String]("USER_ID")

      def userPermission = column[String]("USER_PERMISSION")

      def * = (id, userPermission)

      def fk_device = foreignKey("databaseuser_permissions_user_fk", id, databaseUsers)(_.id)
    }

    val databaseUserPermissions = TableQuery[DatabaseUserPermissions]


    def databaseByID(d: Databases, id: String) = (d.id is id) && (d.deleted is false)

    def databaseByID(d: Databases, id: Column[String]) = (d.id is id) && (d.deleted is false)

    def databaseByName(d: Databases, name: String) = (d.name is name) && (d.deleted is false)

    def databaseByName(d: Databases, name: Column[String]) = (d.name is name) && (d.deleted is false)

    def organizationByID(o: Organizations, id: String) = (o.id is id) && (o.deleted is false)

    def organizationByID(o: Organizations, id: Column[String]) = (o.id is id) && (o.deleted is false)

    def organizationByName(o: Organizations, name: String) = (o.name is name) && (o.deleted is false)

    def organizationByName(o: Organizations, name: Column[String]) = (o.name is name) && (o.deleted is false)

    def adminByID(a: AdminUsers, id: String) = (a.id is id) && (a.deleted is false)

    def adminByID(a: AdminUsers, id: Column[String]) = (a.id is id) && (a.deleted is false)

    def userByID(a: DatabaseUsers, id: String) = (a.id is id) && (a.deleted is false)

    def userByID(a: DatabaseUsers, id: Column[String]) = (a.id is id) && (a.deleted is false)

    val organizationByIDQuery = for {
      id <- Parameters[String]
      (o, d) <- organizations.where(org => organizationByID(org, id)) leftJoin databases.where(_.deleted is false) on (_.id === _.organization)
    } yield (o.name, o.version, d.id.?, d.name.?, d.oauthTTL.?, d.version.?)

    val organizationInfoByIDQuery = for {
      id <- Parameters[String]
      o <- organizations if organizationByID(o, id)
    } yield (o.name, o.version)

    val organizationByNameQuery = for {
      name <- Parameters[String]
      (o, d) <- organizations.where(org => organizationByName(org, name)) leftJoin databases.where(_.deleted is false) on (_.id === _.organization)
    } yield (o.id, o.version, d.id.?, d.name.?, d.oauthTTL.?, d.version.?)

    val organizationInfoByNameQuery = for {
      name <- Parameters[String]
      d <- databases
      o <- organizations if (o.name is name) && (o.deleted is false)
    } yield (o.id, o.version)

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

    val deviceByIDQuery = for {
      id <- Parameters[String]
      ((d, db), p) <- devices.where(d => (d.id is id) && (d.deleted is false)) innerJoin databases.where(_.deleted is false) on (_.parent === _.id) leftJoin devicePermissions on (_._1.id === _.id)
    } yield (d.deviceID, d.activated, d.disabled, p.devicePermission.?, d.version, db.id, db.name, db.version)

    val deviceByDeviceIDQuery = for {
      deviceID <- Parameters[String]
      ((d, db), p) <- devices.where(d => (d.deviceID is deviceID) && (d.deleted is false)) innerJoin databases.where(_.deleted is false) on (_.parent === _.id) leftJoin devicePermissions on (_._1.id === _.id)
    } yield (d.id, d.activated, d.disabled, p.devicePermission.?, d.version, db.id, db.name, db.version)

    val updateDeviceByIDQuery = for {
      (id, version) <- Parameters[(String, Int)]
      (d, db) <- devices.where(d => (d.id is id) && (d.deleted is false) && (d.version is version)) innerJoin databases.where(_.deleted is false) on (_.parent === _.id)
    } yield (d.deviceID, d.activated, d.disabled, d.version)

    val deleteDevicePermissionsQuery = for {
      id <- Parameters[String]
      p <- devicePermissions if p.id is id
    } yield p

    val checkAdminQuery = for {
      adminID <- Parameters[String]
      a <- adminUsers if adminByID(a, adminID)
    } yield 1

    val adminByIDQuery = for {
      id <- Parameters[String]
      (((a, _), o), d) <- adminUsers.where(a => adminByID(a, id)) leftJoin organizationAdmins on (_.id === _.adminID) leftJoin organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled, a.version, o.id.?, o.name.?, o.version.?, d.id.?, d.name.?, d.oauthTTL.?, d.version.?)

    val adminByEmailQuery = for {
      email <- Parameters[String]
      (((a, _), o), d) <- adminUsers.where(a => (a.email is email) && (a.deleted is false)) leftJoin organizationAdmins on (_.id === _.adminID) leftJoin organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.username, a.id, a.credential, a.activated, a.confirmed, a.disabled, a.version, o.id.?, o.name.?, o.version.?, d.id.?, d.name.?, d.oauthTTL.?, d.version.?)

    val adminByUsernameQuery = for {
      username <- Parameters[String]
      (((a, _), o), d) <- adminUsers.where(a => (a.username is username) && (a.deleted is false)) leftJoin organizationAdmins on (_.id === _.adminID) leftJoin organizations.where(_.deleted is false) on (_._2.orgID === _.id) leftJoin databases.where(_.deleted is false) on (_._2.id === _.organization)
    } yield (a.name, a.surname, a.id, a.email, a.credential, a.activated, a.confirmed, a.disabled, a.version, o.id.?, o.name.?, o.version.?, d.id.?, d.name.?, d.oauthTTL.?, d.version.?)

    val userByIDQuery = for {
      id <- Parameters[String]
      (a, p) <- databaseUsers.where(a => userByID(a, id)) leftJoin databaseUserPermissions on (_.id === _.id)
    } yield (a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled, a.version, p.userPermission.?)

    val userByEmailQuery = for {
      email <- Parameters[String]
      (a, p) <- databaseUsers.where(a => (a.email is email) && (a.deleted is false)) leftJoin databaseUserPermissions on (_.id === _.id)
    } yield (a.name, a.surname, a.username, a.id, a.credential, a.activated, a.confirmed, a.disabled, a.version, p.userPermission.?)

    val userByUsernameQuery = for {
      username <- Parameters[String]
      (a, p) <- databaseUsers.where(a => (a.username is username) && (a.deleted is false)) leftJoin databaseUserPermissions on (_.id === _.id)
    } yield (a.name, a.surname, a.id, a.email, a.credential, a.activated, a.confirmed, a.disabled, a.version, p.userPermission.?)

    def create {
      (organizations.ddl ++ databases.ddl ++ series.ddl ++ tags.ddl ++ attributes.ddl ++ devices.ddl ++
        devicePermissions.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ databaseUsers.ddl ++ databaseUserPermissions.ddl).create
    }

    def drop {
      (organizations.ddl ++ databases.ddl ++ series.ddl ++ tags.ddl ++ attributes.ddl ++ devices.ddl ++
        devicePermissions.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ databaseUsers.ddl ++ databaseUserPermissions.ddl).drop
    }

    def saveDatabase(database: domain.Database, orgID: UUID) = {
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
      databaseByIDQuery(dbID.toString).firstOption.map(d => domain.Database(dbID, d._1, domain.DatabaseMetadata(d._2), d._3))
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


    def getOrganizationByID(orgID: UUID) = {
      val orgs = organizationByIDQuery(orgID.toString).list
      checkEmpty(orgs) {
        domain.Organization(orgID, orgs.head._1, extractDatabases(orgs), orgs.head._2)
      }
    }

    def getOrganizationByName(orgName: String) = {
      val orgs = organizationByNameQuery(orgName).list
      checkEmpty(orgs) {
        domain.Organization(UUID.fromString(orgs.head._1), orgName, extractDatabases(orgs), orgs.head._2)
      }
    }

    def saveOrganization(org: Organization) {
      if (organizationByNameQuery(org.name).firstOption.isDefined)
        throw domain.DuplicateIDEntity(s"organization with ${org.name} is already defined!")
      else
        organizations.map(o => (o.id, o.name)) +=(org.id.toString, org.name)
    }

    def getDeviceByID(id: UUID) = {
      val devices = deviceByIDQuery(id.toString).list
      checkEmpty(devices) {
        val head = devices.head
        domain.Device(id, head._1, domain.DatabaseInfo(UUID.fromString(head._6), head._7, head._8), head._2, head._3, devices.map(_._4).flatten.toSet, head._5)
      }
    }

    def getDeviceByDeviceID(deviceID: String) = {
      val devices = deviceByDeviceIDQuery(deviceID).list
      checkEmpty(devices) {
        val head = devices.head
        domain.Device(UUID.fromString(head._1), deviceID, domain.DatabaseInfo(UUID.fromString(head._6), head._7, head._8), head._2, head._3, devices.map(_._4).flatten.toSet, head._5)
      }
    }

    def saveDevice(device: domain.Device) = {
      devices.map(d => (d.id, d.deviceID, d.activated, d.disabled, d.parent)) +=(device.id.toString, device.deviceID, device.activated, device.disabled, device.databaseRef.id.toString)
      devicePermissions ++= device.permissions.map(p => (device.id.toString, p)).toSeq
    }

    def updateDevice(device: domain.Device) = {
      if (deviceByIDQuery(device.id.toString).firstOption.isEmpty)
        throw IDEntityNotFound(s"device with id ${device.id} not found!")
      val count = (for {
        d <- devices if (d.id is device.id.toString) && (d.deleted is false) && (d.version is device.version)
      } yield (d.deviceID, d.activated, d.disabled, d.version)) update ((device.deviceID, device.activated, device.disabled, device.version + 1))
      if (count > 0) {
        deleteDevicePermissionsQuery(device.id.toString).delete
        devicePermissions ++= device.permissions.map(p => (device.id.toString, p)).toSeq
        device.copy(version = device.version + 1)
      } else {
        throw OptimisticLockingException(s"device with id ${device.id} is already changed!")
      }
    }

    protected def extractOrganizationFromAdminList(admins: List[(Option[String], Option[String], String, String, String, Boolean, Boolean, Boolean, Int, Option[String], Option[String], Option[Int], Option[String], Option[String], Option[Long], Option[Int])]): Set[domain.Organization] = admins.groupBy(_._10).map {
      r =>
        (r._1, r._2.head._11, r._2.head._12) match {
          case (None, None, None) => None
          case (Some(orgID), Some(orgName), Some(version)) =>
            Some(domain.Organization(UUID.fromString(orgID), orgName, r._2.map {
              d =>
                (d._13, d._14, d._15, d._16) match {
                  case (None, None, None, None) => None
                  case (Some(dbid), Some(dbname), Some(oauthttl), Some(dversion)) => Some(domain.Database(UUID.fromString(dbid), dbname, domain.DatabaseMetadata(oauthttl), dversion))
                  case _ => throw new IllegalArgumentException(s"illegal row ${r._2}")
                }
            }.flatten.toSet, version))
          case _ => throw new IllegalArgumentException(s"illegal row ${r._2}")
        }
    }.flatten.toSet

    def isAdminExist(id: UUID) = checkAdminQuery(id.toString).firstOption.isDefined

    def getAdminByID(id: UUID) = {
      val admins = adminByIDQuery(id.toString).list
      checkEmpty(admins) {
        val head = admins.head
        domain.AdminUser(id, head._1, head._2, head._3, head._4, head._5, head._6, head._7, head._8,
          extractOrganizationFromAdminList(admins), head._9)
      }
    }


    def getAdminByEmail(email: String) = {
      val admins = adminByEmailQuery(email).list
      checkEmpty(admins) {
        val head = admins.head
        domain.AdminUser(UUID.fromString(head._4), head._1, head._2, head._3, email, head._5, head._6, head._7, head._8,
          extractOrganizationFromAdminList(admins), head._9)
      }
    }

    def getAdminByUsername(username: String) = {
      val admins = adminByUsernameQuery(username).list
      checkEmpty(admins) {
        val head = admins.head
        domain.AdminUser(UUID.fromString(head._3), head._1, head._2, username, head._4, head._5, head._6, head._7,
          head._8, extractOrganizationFromAdminList(admins), head._9)
      }
    }

    def saveAdminUser(admin: domain.AdminUser) {
      adminUsers.map(a => (a.id, a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled)) +=(admin.id.toString,
        admin.name, admin.surname, admin.username, admin.email, admin.credential, admin.activated, admin.confirmed, admin.disabled)
    }

    def updateAdminUser(admin: domain.AdminUser) = ???

    def getOrganizationInfoByID(uuid: UUID): Option[OrganizationInfo] = organizationInfoByIDQuery(uuid.toString).firstOption.map(r => OrganizationInfo(uuid, r._1, r._2))

    def addAdminToOrganization(adminID: UUID, orgID: UUID) {
      organizationAdmins +=(orgID.toString, adminID.toString)
    }

    def getUserByID(id: UUID) = {
      val users = userByIDQuery(id.toString).list
      checkEmpty(users) {
        val head = users.head
        domain.DatabaseUser(id, head._1, head._2, head._3, head._4, head._5, head._6, head._7, head._8, users.map(_._10).flatten.toSet, head._9)
      }
    }

    def getUserByEmail(email: String) = {
      val users = userByEmailQuery(email).list
      checkEmpty(users) {
        val head = users.head
        domain.DatabaseUser(UUID.fromString(head._4), head._1, head._2, head._3, email, head._5, head._6, head._7, head._8, users.map(_._10).flatten.toSet, head._9)
      }
    }

    def getUserByUsername(username: String) = {
      val users = userByUsernameQuery(username).list
      checkEmpty(users) {
        val head = users.head
        domain.DatabaseUser(UUID.fromString(head._3), head._1, head._2, username, head._4, head._5, head._6, head._7, head._8, users.map(_._10).flatten.toSet, head._9)
      }
    }

    //TODO remove schema validation
    def saveUser(user: domain.DatabaseUser) {
      databaseUsers.map(a => (a.id, a.name, a.surname, a.username, a.email, a.credential, a.activated, a.confirmed, a.disabled)) +=(user.id.toString,
        user.name, user.surname, user.username, user.email, user.credential, user.activated, user.confirmed, user.disabled)
      databaseUserPermissions ++= user.permissions.map(p => (user.id.toString, p)).toSeq
    }

    def updateUser(user: domain.DatabaseUser) = {

    }
  }

}
