package io.swarm.management.impl

import java.util.UUID
import scala.Predef._

import io.swarm.management.Management
import io.swarm.management.Management._
import io.swarm.management.Management.AdminUserRef
import io.swarm.management.Management.OrganizationRef
import io.swarm.management.Management.Domain
import io.swarm.domain.{DuplicateIDEntity, UserInfo}
import scala.slick.driver.JdbcProfile
import java.sql.SQLIntegrityConstraintViolationException
import io.swarm.management.dao.ManagementDao


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
    def full = q leftJoin domains on (_.id is _.orgID) leftJoin organizationAdmins on (_._1.id is _.orgID) leftJoin adminUsers on (_._2.adminID is _.id)
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

  implicit class DevicesExtensions(val q: Query[Devices, Management.DeviceRef]) {
    def withPermissions = q leftJoin clientPermissions on (_.id is _.clientID)
  }

  class Devices(tag: Tag) extends Table[Management.DeviceRef](tag, "DEVICES") with HasID with HasDisabled {

    def deviceID = column[String]("DEVICE_OWN_ID")

    def activated = column[Boolean]("ACTIVATED")

    def * = (id, deviceID, activated, disabled) <>(Management.DeviceRef.tupled, Management.DeviceRef.unapply)

    def index_deviceID = index("idx_devicesdeviceID", deviceID, unique = true)
  }

  val devices = TableQuery[Devices]

  implicit class AdminExtensions(val q: Query[AdminUsers, Management.AdminUserRef]) {
    def withOrganizationRefs = q leftJoin organizationAdmins on (_.id is _.adminID) leftJoin organizations on (_._2.orgID is _.id)
  }

  class AdminUsers(tag: Tag) extends Table[Management.AdminUserRef](tag, "ADMIN_USERS") with UserTable {

    def * = (id, name, surname, username, email, credential, activated, confirmed, disabled) <>(Management.AdminUserRef.tupled, Management.AdminUserRef.unapply)

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

  implicit class UsersExtensions(val q: Query[Users, Management.UserRef]) {
    def withPermissions = q leftJoin clientPermissions on (_.id is _.clientID)
  }

  class Users(tag: Tag) extends Table[Management.UserRef](tag, "DATABASE_USERS") with UserTable {

    def * = (id, name, surname, username, email, credential, activated, confirmed, disabled) <>(Management.UserRef.tupled, Management.UserRef.unapply)

    def index_email = index("idx_databaseusersemail", email, unique = true)

    def index_username = index("idx_databaseusersusername", username, unique = true)
  }

  val users = TableQuery[Users]

  class ClientPermissions(tag: Tag) extends Table[(UUID, String, String, UUID, String)](tag, "CLIENT__PERMISSIONS") {
    def clientID = column[UUID]("CLIENTID")

    def serviceName = column[String]("SERVICE_NAME")

    def action = column[String]("ACTION")

    def tenantID = column[UUID]("TENANT_ID")

    def servicePerms = column[String]("SERVICE_PERMS")

    def * = (clientID, serviceName, action, tenantID, servicePerms)

    def index_clientID = index("idx_clientpermissionsclientid", clientID)
  }

  val clientPermissions = TableQuery[ClientPermissions]

  implicit class OrgWithDomainHelper(seq: Seq[(Management.OrganizationRef, (Option[UUID], Option[String]), (Option[UUID], Option[String], Option[String], Option[String], Option[String], Option[String], Option[Boolean], Option[Boolean], Option[Boolean]))]) {
    def toTuple =
      if (seq.isEmpty)
        None
      else Some((seq.head._1, seq.map(_._2.toDomain).flatten.toSet, seq.map(_._3.toAdmin).flatten.toSet))
  }

  implicit class DevWithPermHelper(seq: Seq[(Management.DeviceRef, (Option[String], Option[UUID], Option[String], Option[String]))]) {
    def toTuple =
      if (seq.isEmpty)
        None
      else Some((seq.head._1, seq.map {
        i =>
          i._2.toPermission
      }.flatten.toSet))
  }

  implicit class UserWithPermHelper(seq: Seq[(Management.UserRef, (Option[String], Option[UUID], Option[String], Option[String]))]) {
    def toUser =
      if (seq.isEmpty)
        None
      else Some((seq.head._1, seq.map {
        i =>
          i._2.toPermission
      }.flatten.toSet))
  }

  implicit class AdminWithOrgRefHelper(seq: Seq[(Management.AdminUserRef, (Option[UUID], Option[String], Option[Boolean]))]) {
    def toAdminUser =
      if (seq.isEmpty)
        None
      else Some((seq.head._1, seq.map {
        i =>
          i._2.toOrganizationRef
      }.flatten.toSet))
  }

  implicit class DomainHelper(t: (Option[UUID], Option[String])) {
    def toDomain = t._1.map(id => Domain(id, t._2.get))
  }

  implicit class AdminHelper(t: (Option[UUID], Option[String], Option[String], Option[String], Option[String], Option[String], Option[Boolean], Option[Boolean], Option[Boolean])) {
    def toAdmin = t._1.map {
      id =>
        AdminUserRef(id, t._2, t._3, t._4.get, t._5.get, t._6.get, t._7.get, t._8.get, t._9.get)
    }
  }

  implicit class OrganizationRefHelper(t: (Option[UUID], Option[String], Option[Boolean])) {
    def toOrganizationRef = t._1.map {
      id =>
        OrganizationRef(id, t._2.get, t._3.get)
    }
  }

  implicit class PermissionHelper(t: (Option[String], Option[UUID], Option[String], Option[String])) {
    def toPermission = t._1.map {
      serviceName =>
        ACLEntry(serviceName, t._2.get, t._3.get, t._4.get.split(",").toList)
    }
  }


  object OrganizationQueries {

    private def byIDQuery(id: Column[UUID]) = for {
      o <- organizations.where(_.id is id)
    } yield (o)

    private def byIDWithDomainsQuery(id: Column[UUID]) = for {
      (((o, d), od), a) <- organizations.where(_.id is id).full
    } yield (o, (d.id.?, d.name.?), (a.id.?, a.name, a.surname, a.username.?, a.email.?, a.credential.?, a.activated.?, a.confirmed.?, a.disabled.?))

    private def byNameQuery(name: Column[String]) = for {
      o <- organizations.where(_.name === name)
    } yield o

    private def domainQuery(id: Column[UUID]) = for {
      d <- domains if d.id is id
    } yield d

    private def domainCount(id: Column[UUID]) = (for {d <- domains if d.orgID is id} yield d).length

    val byID = Compiled(byIDQuery _)
    val byIDWithDomains = Compiled(byIDWithDomainsQuery _)
    val byName = Compiled(byNameQuery _)
    val countDomains = Compiled(domainCount _)
    val domainByID = Compiled(domainQuery _)
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

    val byIDWithPerms = {
      def query(id: Column[UUID]) = for {
        (d, p) <- devices.where(_.id is id).withPermissions
      } yield (d, (p.serviceName.?, p.tenantID.?, p.action.?, p.servicePerms.?))
      Compiled(query _)
    }
  }

  object AdminUserQueries {
    val adminRefByID = {
      def query(id: Column[UUID]) = for {
        a <- adminUsers if a.id is id
      } yield a
      Compiled(query _)
    }

    val adminRefByEmail = {
      def query(email: Column[String]) = for {
        a <- adminUsers if a.email is email
      } yield a
      Compiled(query _)
    }

    val adminRefByUsername = {
      def query(username: Column[String]) = for {
        a <- adminUsers if a.username is username
      } yield a
      Compiled(query _)
    }

    val adminByID = {
      def query(id: Column[UUID]) = for {
        ((a, oa), o) <- adminUsers.where(_.id is id).withOrganizationRefs
      } yield (a, (o.id.?, o.name.?, o.disabled.?))
      Compiled(query _)
    }

    val adminByEmail = {
      def query(email: Column[String]) = for {
        ((a, oa), o) <- adminUsers.where(_.email is email).withOrganizationRefs
      } yield (a, (o.id.?, o.name.?, o.disabled.?))
      Compiled(query _)
    }

    val adminByUsername = {
      def query(username: Column[String]) = for {
        ((a, oa), o) <- adminUsers.where(_.username is username).withOrganizationRefs
      } yield (a, (o.id.?, o.name.?, o.disabled.?))
      Compiled(query _)
    }
  }


  object UserQueries {

    val userRefByID = {
      def query(id: Column[UUID]) = for {
        u <- users if u.id is id
      } yield u
      Compiled(query _)
    }

    val userRefByEmail = {
      def query(email: Column[String]) = for {
        u <- users if u.email is email
      } yield u
      Compiled(query _)
    }

    val userRefByUsername = {
      def query(username: Column[String]) = for {
        u <- users if u.username is username
      } yield u
      Compiled(query _)
    }

    val userByID = {
      def query(id: Column[UUID]) = for {
        (u, p) <- users.where(_.id is id).withPermissions
      } yield (u, (p.serviceName.?, p.tenantID.?, p.action.?, p.servicePerms.?))
      Compiled(query _)
    }

    val userByEmail = {
      def query(email: Column[String]) = for {
        (u, p) <- users.where(_.email is email).withPermissions
      } yield (u, (p.serviceName.?, p.tenantID.?, p.action.?, p.servicePerms.?))
      Compiled(query _)
    }

    val userByUsername = {
      def query(username: Column[String]) = for {
        (u, p) <- users.where(_.username is username).withPermissions
      } yield (u, (p.serviceName.?, p.tenantID.?, p.action.?, p.servicePerms.?))
      Compiled(query _)
    }
  }


  val clientACLByClientIDAndServiceNameQuery = {
    def query(clientID: Column[UUID], serviceName: Column[String]) = for {
      c <- clientPermissions if (c.clientID is clientID) && (c.serviceName is serviceName)
    } yield c
    Compiled(query _)
  }

  val clientACLByALL = {
    def query(clientID: Column[UUID], serviceName: Column[String], action: Column[String], tenantID: Column[UUID], ext: Column[String]) = for {
      c <- clientPermissions if (c.clientID is clientID) && (c.serviceName is serviceName) && (c.tenantID is tenantID) && (c.action is action) && (c.servicePerms is ext)
    } yield c
    Compiled(query _)
  }

  def create {
    (organizations.ddl ++ domains.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ users.ddl ++ devices.ddl ++ clientPermissions.ddl).create
  }

  def drop {
    (organizations.ddl ++ domains.ddl ++ adminUsers.ddl ++ organizationAdmins.ddl ++ users.ddl ++ devices.ddl ++ clientPermissions.ddl).drop
  }


  def checkEmpty[T, A](list: List[A])(body: => T): Option[T] = {
    if (list == Nil)
      None
    else Some(body)
  }


  def saveAdminUserRef(admin: Management.AdminUserRef) = {
    try {
      adminUsers.insert(admin)
    } catch {
      case e: SQLIntegrityConstraintViolationException => throw DuplicateIDEntity(e)
    }
    admin
  }

  def updateAdminUserRef(admin: Management.AdminUserRef) = {
    AdminUserQueries.adminRefByID(admin.id).update(admin)
    admin
  }

  def updateDeviceRef(ref: DeviceRef): DeviceRef = {
    DeviceQueries.byID(ref.id).update(ref)
    ref
  }

  def updateUserRef(ref: UserRef): UserRef = {
    UserQueries.userRefByID(ref.id).update(ref)
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

  def getUserRef(id: UUID): Option[UserRef] = UserQueries.userRefByID(id).firstOption

  def getUserRefByUsername(username: String): Option[UserRef] = UserQueries.userRefByUsername(username).firstOption

  def getUserRefByEmail(email: String): Option[UserRef] = UserQueries.userRefByEmail(email).firstOption

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

  def getOrganization(id: UUID): Option[Management.Organization] = OrganizationQueries.byIDWithDomains(id).list.toTuple.map {
    x =>
      x._1.toOrganization(x._2, x._3)
  }


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

  def getAdminUserRef(id: UUID): Option[AdminUserRef] = AdminUserQueries.adminRefByID(id).firstOption

  def getAdminUserRefByEmail(email: String): Option[AdminUserRef] = AdminUserQueries.adminRefByEmail(email).firstOption

  def getAdminUserRefByUsername(username: String): Option[AdminUserRef] = AdminUserQueries.adminRefByUsername(username).firstOption

  def getDevice(id: UUID): Option[Management.Device] = DeviceQueries.byIDWithPerms(id).list.toTuple.map {
    t =>
      t._1.toDevice(t._2)
  }

  def updateOrganizationRef(org: OrganizationRef): OrganizationRef = {
    OrganizationQueries.byID(org.id).update(org)
    org
  }

  def updateDomain(domain: Domain, orgID: UUID): Domain = {
    OrganizationQueries.domainByID(domain.id).update((domain.id, domain.name, orgID))
    domain
  }

  def dropACLEntry(clientID: UUID, aclEntry: ACLEntry): Unit = clientACLByALL(clientID, aclEntry.serviceName, aclEntry.action, aclEntry.serviceTenantID, aclEntry.extensions.mkString(","))

  def saveACL(clientID: UUID, aclEntry: ACLEntry): Unit = clientPermissions.insert((clientID, aclEntry.serviceName, aclEntry.action, aclEntry.serviceTenantID, aclEntry.extensions.mkString(",")))

  def dropServiceACLS(clientID: UUID, serviceName: String): Unit = clientACLByClientIDAndServiceNameQuery(clientID, serviceName).delete

  implicit def userTuple2User(userTuple: Option[(Management.UserRef, Set[Management.ACLEntry])]) = userTuple.map {
    tuple =>
      tuple._1.toUser(tuple._2)
  }

  def getUserByEmail(email: String): Option[User] = UserQueries.userByEmail(email).list.toUser

  def getUserByUsername(username: String): Option[User] = UserQueries.userByUsername(username).list.toUser

  def getUser(id: UUID): Option[User] = UserQueries.userByID(id).list.toUser

  implicit def adminTuple2Admin(adminTuple: Option[(Management.AdminUserRef, Set[Management.OrganizationRef])]) = adminTuple.map {
    tuple =>
      tuple._1.toAdminUser(tuple._2)
  }

  def getAdminUserByUsername(username: String): Option[AdminUser] = AdminUserQueries.adminByUsername(username).list.toAdminUser

  def getAdminUserByEmail(email: String): Option[AdminUser] = AdminUserQueries.adminByEmail(email).list.toAdminUser

  def getAdminUser(uuid: UUID): Option[AdminUser] = AdminUserQueries.adminByID(uuid).list.toAdminUser


}
