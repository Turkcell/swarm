package io.swarm.management.impl

import java.util.UUID
import scala.Predef._

import io.swarm.management.Management
import io.swarm.management.Management._
import io.swarm.management.Management.AdminUser
import io.swarm.management.Management.OrganizationRef
import io.swarm.management.Management.Domain
import io.swarm.domain.{DuplicateIDEntity, UserInfo}
import scala.slick.driver.JdbcProfile
import java.sql.SQLIntegrityConstraintViolationException


class ManagementDaoJDBC(val profile: JdbcProfile) extends ManagementDao {

  import profile.simple._
  import Database.dynamicSession

  implicit val uuidColumnType = MappedColumnType.base[UUID, String](_.toString, UUID.fromString(_))

  trait HasID {
    this: Table[_] =>
    def id = column[UUID]("ID", O.PrimaryKey)
  }

  trait HasDisabled {
    this: Table[_] =>
    def disabled = column[Boolean]("DISABLED", O.Default(false))
  }

  trait HasSoftDelete {
    this: Table[_] =>
    def deleted = column[Boolean]("DELETED", O.Default(false))
  }

  trait HasName {
    this: Table[_] =>
    def name = column[String]("NAME")
  }

  trait UserTable extends HasID with HasDisabled {
    this: Table[_] =>
    def name = column[Option[String]]("NAME")

    def surname = column[Option[String]]("SURNAME")

    def username = column[String]("USERNAME")

    def email = column[String]("EMAIL")

    def credential = column[String]("CREDENTIAL")

    def activated = column[Boolean]("ACTIVATED")

    def confirmed = column[Boolean]("CONFIRMED")

  }

  implicit class OrganizationExtensions(val q: Query[Organizations, Management.OrganizationRef]) {
    def withDomains = q leftJoin domains on (_.id is _.orgID)
  }


  // Definition of the Organizations table
  class Organizations(tag: Tag) extends Table[Management.OrganizationRef](tag, "ORGANIZATIONS") with HasID with HasDisabled with HasName {
    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, name, disabled) <>(Management.OrganizationRef.tupled, Management.OrganizationRef.unapply)

    def index_name = index("idx_orgname", name, unique = true)
  }

  val organizations = TableQuery[Organizations]

  class Domains(tag: Tag) extends Table[(UUID, String, UUID)](tag, "DOMAINS") with HasID with HasName {
    // Every table needs a * projection with the same type as the table's type parameter

    def orgID = column[UUID]("ORG_ID")

    def * = (id, name, orgID)

    def index_name = index("idx_domname", (orgID, name), unique = true)

    def org_fk = foreignKey("domains_organization_fk", orgID, organizations)(_.id)
  }

  val domains = TableQuery[Domains]

  class Devices(tag: Tag) extends Table[Management.DeviceRef](tag, "DEVICES") with HasID with HasDisabled {

    def deviceID = column[String]("DEVICE_OWN_ID")

    def activated = column[Boolean]("ACTIVATED")

    def * = (id, deviceID, activated, disabled) <>(Management.DeviceRef.tupled, Management.DeviceRef.unapply)

    def index_deviceID = index("idx_devicesdeviceID", deviceID, unique = true)
  }

  val devices = TableQuery[Devices]

  class AdminUsers(tag: Tag) extends Table[Management.AdminUser](tag, "ADMIN_USERS") with UserTable {

    def * = (id, name, surname, username, email, credential, activated, confirmed, disabled) <>(Management.AdminUser.tupled, Management.AdminUser.unapply)

    def index_email = index("idx_adminusersemail", email, unique = true)

    def index_username = index("idx_adminusersusername", username, unique = true)
  }

  val adminUsers = TableQuery[AdminUsers]

  class OrganizationAdmins(tag: Tag) extends Table[(UUID, UUID)](tag, "ORGANIZATION_ADMINS") {
    def orgID = column[UUID]("ORGID")

    def adminID = column[UUID]("ADMINID")

    def * = (orgID, adminID)

    def fk_organization = foreignKey("organization_admins_organization_fk", orgID, organizations)(_.id)

    def fk_admin = foreignKey("organization_admins_admin_fk", adminID, adminUsers)(_.id)
  }

  val organizationAdmins = TableQuery[OrganizationAdmins]

  class Users(tag: Tag) extends Table[Management.UserRef](tag, "DATABASE_USERS") with UserTable {

    def * = (id, name, surname, username, email, credential, activated, confirmed, disabled) <>(Management.UserRef.tupled, Management.UserRef.unapply)

    def index_email = index("idx_databaseusersemail", email, unique = true)

    def index_username = index("idx_databaseusersusername", username, unique = true)
  }

  val users = TableQuery[Users]

  case class ClientPermission(clientID: UUID, serviceName: String, action: String, tenantID: UUID, servicePerms: String)

  class ClientPermissions(tag: Tag) extends Table[ClientPermission](tag, "CLIENT__PERMISSIONS") {
    def clientID = column[UUID]("CLIENTID")

    def serviceName = column[String]("SERVICE_NAME")

    def action = column[String]("ACTION")

    def tenantID = column[UUID]("TENANT_ID")

    def servicePerms = column[String]("SERVICE_PERMS")

    def * = (clientID, serviceName, action, tenantID, servicePerms) <>(ClientPermission.tupled, ClientPermission.unapply)

    def index_clientID = index("idx_clientpermissionsclientid", clientID)
  }

  val clientPermissions = TableQuery[ClientPermissions]

  implicit class OrgWithDomainHelper(seq: Seq[(Management.OrganizationRef, Option[UUID], Option[String])]) {
    def toTuple =
      if (seq.isEmpty)
        None
      else Some((seq.head._1, seq.map {
        i =>
          (i._2, i._3) match {
            case (Some(l), Some(r)) => Management.Domain(l, r)
          }
      }.toSet))
  }

  object OrganizationQueries {

    private def byIDQuery(id: Column[UUID]) = for {
      o <- organizations.where(_.id is id)
    } yield (o)

    private def byIDWithDomainsQuery(id: Column[UUID]) = for {
      (o, d) <- organizations.where(_.id is id).withDomains
    } yield (o, d.id.?, d.name.?)

    private def byNameQuery(name: Column[String]) = for {
      o <- organizations.where(_.name === name)
    } yield o

    private def domainCount(id: Column[UUID]) = (for {d <- domains if d.orgID is id} yield d).length

    val byID = Compiled(byIDQuery _)
    val byIDWithDomains = Compiled(byIDWithDomainsQuery _)
    val byName = Compiled(byNameQuery _)
    val countDomains = Compiled(domainCount _)
  }


  object DomainQueries {
    private def byIDQuery(id: Column[UUID]) = for {
      d <- domains if d.id is id
    } yield d

    val byID = Compiled(byIDQuery _)
  }

  object DeviceQueries {
    val byID = {
      def query(id: Column[UUID]) = for {
        d <- devices if d.id is id
      } yield d
      Compiled(query _)
    }

    val byDeviceID = {
      def query(deviceID: Column[String]) = for {
        d <- devices if d.deviceID is deviceID
      } yield d
      Compiled(query _)
    }
  }


  val adminByIDQuery = {
    def query(id: Column[UUID]) = for {
      a <- adminUsers if a.id is id
    } yield a
    Compiled(query _)
  }

  val adminByEmailQuery = {
    def query(email: Column[String]) = for {
      a <- adminUsers if a.email is email
    } yield a
    Compiled(query _)
  }

  val adminByUsernameQuery = {
    def query(username: Column[String]) = for {
      a <- adminUsers if a.username is username
    } yield a
    Compiled(query _)
  }

  val userByIDQuery = {
    def query(id: Column[UUID]) = for {
      u <- users if u.id is id
    } yield u
    Compiled(query _)
  }

  val userByEmailQuery = {
    def query(email: Column[String]) = for {
      u <- users if u.email is email
    } yield u
    Compiled(query _)
  }

  val userByUsernameQuery = {
    def query(username: Column[String]) = for {
      u <- users if u.username is username
    } yield u
    Compiled(query _)
  }

  def create {
    (organizations.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ users.ddl ++ devices.ddl ++ clientPermissions.ddl).create
  }

  def drop {
    (organizations.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ users.ddl ++ devices.ddl ++ clientPermissions.ddl).drop
  }


  def checkEmpty[T, A](list: List[A])(body: => T): Option[T] = {
    if (list == Nil)
      None
    else Some(body)
  }


  def saveAdminUser(admin: Management.AdminUser) = {
    try {
      adminUsers.insert(admin)
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    admin
  }

  def updateAdminUser(admin: Management.AdminUser) = {
    adminByIDQuery(admin.id).update(admin)
    admin
  }

  def updateDeviceRef(ref: DeviceRef): DeviceRef = {
    DeviceQueries.byID(ref.id).update(ref)
    ref
  }

  def updateUserRef(ref: UserRef): UserRef = {
    userByIDQuery(ref.id).update(ref)
    ref
  }

  def saveDeviceRef(ref: DeviceRef): DeviceRef = {
    try {
      devices.insert(ref)
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    ref
  }

  def saveUserRef(ref: UserRef): UserRef = {
    try {
      users.insert(ref)
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    ref
  }

  def getDeviceRef(id: UUID): Option[DeviceRef] = DeviceQueries.byID(id).firstOption

  def getDeviceRefByDeviceID(deviceID: String): Option[DeviceRef] = DeviceQueries.byDeviceID(deviceID).firstOption

  def getUserRef(id: UUID): Option[UserInfo] = userByIDQuery(id).firstOption

  def getUserRefByUsername(username: String): Option[UserInfo] = userByUsernameQuery(username).firstOption

  def getUserRefByEmail(email: String): Option[UserRef] = userByEmailQuery(email).firstOption

  def associateAdmin(orgID: UUID, adminID: UUID): Unit = organizationAdmins.insert((orgID, adminID))

  def saveOrganizationRef(org: OrganizationRef): OrganizationRef = {
    try {
      organizations.insert(org)
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    org
  }

  def getOrganizationRef(id: UUID): Option[OrganizationRef] = OrganizationQueries.byID(id).firstOption

  def getOrganization(id: UUID): Option[Management.Organization] = OrganizationQueries.byIDWithDomains(id).list.toTuple

  def getOrganizationRefByName(name: String): Option[OrganizationRef] = OrganizationQueries.byName(name).firstOption

  def removeDomain(id: UUID): Unit = DomainQueries.byID(id).delete

  def getDomainCount(orgID: UUID): Int = OrganizationQueries.countDomains(orgID).run

  def saveDomain(domain: Domain, orgID: UUID): Domain = {
    try {
      domains.insert((domain.id, domain.name, orgID))
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    domain
  }

  def getAdminUser(id: UUID): Option[AdminUser] = adminByIDQuery(id).firstOption

  def getAdminUserByEmail(email: String): Option[AdminUser] = adminByEmailQuery(email).firstOption

  def getAdminUserByUsername(username: String): Option[AdminUser] = adminByUsernameQuery(username).firstOption
}
