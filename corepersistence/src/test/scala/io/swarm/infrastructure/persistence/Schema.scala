package io.swarm.infrastructure.persistence

import io.swarm.domain._
import org.scalatest.{ConfigMap, BeforeAndAfterAllConfigMap, ShouldMatchers, FlatSpec}
import scala.slick.driver.{JdbcProfile, HsqldbDriver}
import io.swarm.UUIDGenerator
import io.swarm.security.HashedAlgorithm
import scala.slick.jdbc.JdbcBackend.Database
import io.swarm.management.Management._
import io.swarm.management.Management.DomainRef
import io.swarm.management.Management.AdminUserRef
import io.swarm.management.Management.OrganizationRef
import io.swarm.management.Management.DeviceRef
import scala.Some
import io.swarm.management.impl.ManagementDaoJDBC
import java.util.UUID
import io.swarm.management.dao.ManagementDaoComponent

/**
 * Created by capacman on 10/26/13.
 */

trait HSQLInMemoryManagementDaoComponent extends ManagementDaoComponent {

  val profile: JdbcProfile = HsqldbDriver

  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")

  val managementDao = new ManagementDaoJDBC(profile)
}

class SchemaTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAllConfigMap with HSQLInMemoryManagementDaoComponent {

  val domains = List(DomainRef(UUIDGenerator.randomGenerator.generate(), "dom1"), DomainRef(UUIDGenerator.randomGenerator.generate(), "dom2"))
  val organizations = List(OrganizationRef(UUIDGenerator.randomGenerator.generate(), "testorg", disabled = false), OrganizationRef(UUIDGenerator.randomGenerator.generate(), "withoutDom", disabled = false))
  val admins = List(AdminUserRef(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), activated = true, confirmed = true, disabled = false), AdminUserRef(UUIDGenerator.randomGenerator.generate(), Some("test2"), Some("test2"), "test2", "test2@test.com", HashedAlgorithm.toHex("test"), activated = true, confirmed = true, disabled = false))
  val devices = List(DeviceRef(UUIDGenerator.randomGenerator.generate(), "device1", activated = true, disabled = false), DeviceRef(UUIDGenerator.randomGenerator.generate(), "device2", activated = true, disabled = false))
  val users = List(UserRef(UUIDGenerator.randomGenerator.generate(), Some("user"), Some("user"), "user", "user@user.com", HashedAlgorithm.toHex("test"), activated = true, confirmed = true, disabled = false), UserRef(UUIDGenerator.randomGenerator.generate(), Some("user2"), Some("user2"), "user2", "user2@user.com", HashedAlgorithm.toHex("test"), activated = true, confirmed = true, disabled = false))
  val acls = Set(ACLEntry("service1", UUIDGenerator.randomGenerator.generate(), "get", List("a", "b", "c")), ACLEntry("service1", UUIDGenerator.randomGenerator.generate(), "get", List("d", "e", "f")))

  implicit class TupleACL(acl: (UUID, UUID, String, String, List[String])) {
    def toACLEntry = ACLEntry(acl._3, acl._2, acl._4, acl._5)
  }

  override def beforeAll(configMap: ConfigMap) {
    db withSession {
      implicit session =>
        managementDao.create
        organizations.foreach(managementDao.saveOrganizationRef)
        devices.foreach(managementDao.saveDeviceRef)
        admins.foreach(managementDao.saveAdminUserRef)
        managementDao.associateAdmin(organizations.head.id, admins.head.id)
        users.foreach(managementDao.saveUserRef)
        domains.foreach(managementDao.saveDomain(_, organizations.head.id))
        acls.foreach(p => managementDao.saveACL(devices.head.id, p))
        acls.foreach(p => managementDao.saveACL(users.head.id, p))
    }
  }

  override def afterAll(configMap: ConfigMap) {
    db withSession {
      implicit session =>
        managementDao.drop
    }
  }

  it should "get organization by id" in {
    db withSession {
      implicit session =>
        managementDao.getOrganizationRef(organizations.head.id) should be(Some(organizations.head))
        managementDao.getOrganizationRef(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "throw DuplicateIDEntity for duplicate organizations" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session =>
          managementDao.saveOrganizationRef(organizations.head)
      }
    }
  }

  it should "get organization with domains" in {
    db withSession {
      implicit session =>
        val org = managementDao.getOrganization(organizations.head.id)
        org should be(Some(organizations.head.toOrganization(domains.toSet, Set(admins.head))))
    }
  }

  it should "get organization with out domains" in {
    db withSession {
      implicit session =>
        val org = managementDao.getOrganization(organizations.last.id)
        org should be(Some((organizations.last.toOrganization(Set(), Set()))))
    }
  }

  it should "get device by id " in {
    db withSession {
      implicit session =>
        managementDao.getDeviceRef(devices.head.id) should be(Some(devices.head))
    }
  }

  it should "get device by id with perms" in {
    db withSession {
      implicit session =>
        managementDao.getDevice(devices.head.id) should be(Some(devices.head.toDevice(acls)))
    }
  }

  it should "get device withoutperm by id " in {
    db withSession {
      implicit session =>
        managementDao.getDevice(devices.last.id) should be(Some(devices.last.toDevice(Set())))
    }
  }

  it should "get device by deviceId " in {
    db withSession {
      implicit session =>
        managementDao.getDeviceRefByDeviceID(devices.head.deviceID) should be(Some(devices.head))
    }
  }


  it should "throw DuplicateIDEntity for duplicate devices" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session =>
          managementDao.saveDeviceRef(devices.head)
      }
    }
  }

  it should "update device by id " in {
    db withSession {
      implicit session =>
        val dev = devices.head.copy(activated = false, disabled = true, deviceID = "new deviceID")
        Some(managementDao.updateDeviceRef(dev)) should be(managementDao.getDeviceRef(dev.id))
    }
  }

  it should "get adminref by id" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserRef(admins.head.id) should be(Some(admins.head))
    }
  }

  it should "get adminref by email" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserRefByEmail(admins.head.email) should be(Some(admins.head))
    }
  }

  it should "get adminref by username" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserRefByUsername(admins.head.username) should be(Some(admins.head))
    }
  }

  it should "get admin by id" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUser(admins.head.id) should be(Some(admins.head.toAdminUser(Set(organizations.head))))
    }
  }

  it should "get admin by email" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserByEmail(admins.head.email) should be(Some(admins.head.toAdminUser(Set(organizations.head))))
    }
  }

  it should "get admin by username" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserByUsername(admins.head.username) should be(Some(admins.head.toAdminUser(Set(organizations.head))))
    }
  }

  it should "get adminwithoutorg by id" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUser(admins.last.id) should be(Some(admins.last.toAdminUser(Set())))
    }
  }

  it should "get adminwithoutorg by email" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserByEmail(admins.last.email) should be(Some(admins.last.toAdminUser(Set())))
    }
  }

  it should "get adminwithoutorg by username" in {
    db withSession {
      implicit session =>
        managementDao.getAdminUserByUsername(admins.last.username) should be(Some(admins.last.toAdminUser(Set())))
    }
  }

  it should "throw DuplicateIDEntity for duplicate admins" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session =>
          managementDao.saveAdminUserRef(admins.head)
      }
    }
  }

  it should "get userref by id" in {
    db withSession {
      implicit session =>
        managementDao.getUserRef(users.head.id) should be(Some(users.head))
    }
  }

  it should "get userref by email" in {
    db withSession {
      implicit session =>
        managementDao.getUserRefByEmail(users.head.email) should be(Some(users.head))
    }
  }

  it should "get userref by username" in {
    db withSession {
      implicit session =>
        managementDao.getUserRefByUsername(users.head.username) should be(Some(users.head))
    }
  }

  it should "get user by id" in {
    db withSession {
      implicit session =>
        managementDao.getUser(users.head.id) should be(Some(users.head.toUser(acls)))
    }
  }

  it should "get user by email" in {
    db withSession {
      implicit session =>
        managementDao.getUserByEmail(users.head.email) should be(Some(users.head.toUser(acls)))
    }
  }

  it should "get user by username" in {
    db withSession {
      implicit session =>
        managementDao.getUserByUsername(users.head.username) should be(Some(users.head.toUser(acls)))
    }
  }

  it should "get userWithoutPerm by id" in {
    db withSession {
      implicit session =>
        managementDao.getUser(users.last.id) should be(Some(users.last.toUser(Set())))
    }
  }

  it should "get userWithoutPerm by email" in {
    db withSession {
      implicit session =>
        managementDao.getUserByEmail(users.last.email) should be(Some(users.last.toUser(Set())))
    }
  }

  it should "get userWithoutPerm by username" in {
    db withSession {
      implicit session =>
        managementDao.getUserByUsername(users.last.username) should be(Some(users.last.toUser(Set())))
    }
  }

  it should "throw DuplicateIDEntity for duplicate users" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session =>
          managementDao.saveUserRef(users.head)
      }
    }
  }
}
