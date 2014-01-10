package io.swarm.infrastructure.persistence

import io.swarm.domain._
import org.scalatest.{ConfigMap, BeforeAndAfterAllConfigMap, ShouldMatchers, FlatSpec}
import scala.slick.driver.{JdbcProfile, HsqldbDriver}
import io.swarm.UUIDGenerator
import io.swarm.security.HashedAlgorithm
import scala.slick.jdbc.JdbcBackend.Database
import io.swarm.management.Management._
import io.swarm.management.Management.Domain
import io.swarm.management.Management.AdminUser
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

  val domains = List(Domain(UUIDGenerator.randomGenerator.generate(), "dom1"), Domain(UUIDGenerator.randomGenerator.generate(), "dom2"))
  val organizations = List(OrganizationRef(UUIDGenerator.randomGenerator.generate(), "testorg", false), OrganizationRef(UUIDGenerator.randomGenerator.generate(), "withoutDom", false))
  val admin = AdminUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false)
  val devices = List(DeviceRef(UUIDGenerator.randomGenerator.generate(), "device1", activated = true, disabled = false), DeviceRef(UUIDGenerator.randomGenerator.generate(), "device2", activated = true, disabled = false))
  val user = UserRef(UUIDGenerator.randomGenerator.generate(), Some("user"), Some("user"), "user", "user@user.com", HashedAlgorithm.toHex("test"), true, true, false)
  //clientID: UUID, tenantID: UUID, serviceName: String, action: String, servicePerms: List[String]
  val acls = Set(ACLEntry("service1", UUIDGenerator.randomGenerator.generate(), "get", List("a", "b", "c")), ACLEntry("service1", UUIDGenerator.randomGenerator.generate(), "get", List("d", "e", "f")))

  implicit class TupleACL(acl: (UUID, UUID, String, String, List[String])) {
    def toACLEntry = ACLEntry(acl._3, acl._2, acl._4, acl._5)
  }

  override def beforeAll(configMap: ConfigMap) {
    db withDynSession {
      managementDao.create
      organizations.foreach(managementDao.saveOrganizationRef)
      devices.foreach(managementDao.saveDeviceRef)
      managementDao.saveAdminUser(admin)
      managementDao.saveUserRef(user)
      domains.foreach(managementDao.saveDomain(_, organizations.head.id))
      acls.foreach(p => managementDao.saveACL(devices.head.id, p))
    }
  }

  override def afterAll(configMap: ConfigMap) {
    db withDynSession {

      managementDao.drop
    }
  }

  it should "get organization by id" in {
    db withDynSession {
      managementDao.getOrganizationRef(organizations.head.id) should be(Some(organizations.head))
      managementDao.getOrganizationRef(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "throw DuplicateIDEntity for duplicate organizations" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {
        managementDao.saveOrganizationRef(organizations.head)
      }
    }
  }

  it should "get organization with domains" in {
    db.withDynSession {
      val org = managementDao.getOrganization(organizations.head.id)
      org should be(Some(organizations.head.toOrganization(domains.toSet, Set())))
    }
  }

  it should "get organization with out domains" in {
    db.withDynSession {
      val org = managementDao.getOrganization(organizations.last.id)
      org should be(Some((organizations.last.toOrganization(Set(), Set()))))
    }
  }

  it should "get device by id " in {
    db withDynSession {
      managementDao.getDeviceRef(devices.head.id) should be(Some(devices.head))
    }
  }

  it should "get device by id with perms" in {
    db withDynSession {
      managementDao.getDevice(devices.head.id) should be(Some(devices.head.toDevice(acls)))
    }
  }

  it should "get device withoutperm by id " in {
    db withDynSession {
      managementDao.getDevice(devices.last.id) should be(Some(devices.last.toDevice(Set())))
    }
  }

  it should "get device by deviceId " in {
    db withDynSession {

      managementDao.getDeviceRefByDeviceID(devices.head.deviceID) should be(Some(devices.head))
    }
  }

  it should "get device withoutperm by deviceId " in {
    db withDynSession {

      //managementDao.getDeviceByDeviceID(deviceWithoutPerm.deviceID) should be(Some(deviceWithoutPerm))
    }
  }

  it should "throw DuplicateIDEntity for duplicate devices" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {

        managementDao.saveDeviceRef(devices.head)
      }
    }
  }

  it should "update device by id " in {
    db withDynSession {
      val dev = devices.head.copy(activated = false, disabled = true, deviceID = "new deviceID")
      Some(managementDao.updateDeviceRef(dev)) should be(managementDao.getDeviceRef(dev.id))
    }
  }

  it should "get admin by id" in {
    db withDynSession {

      managementDao.getAdminUser(admin.id) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by id" in {
    db withDynSession {

      //managementDao.getAdminByID(adminWithoutOrg.id) should be(Some(adminWithoutOrg))
    }
  }

  it should "get admin by email" in {
    db withDynSession {

      managementDao.getAdminUserByEmail(admin.email) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by email" in {
    db withDynSession {

      //managementDao.getAdminByEmail(adminWithoutOrg.email) should be(Some(adminWithoutOrg))
    }
  }

  it should "get admin by username" in {
    db withDynSession {

      managementDao.getAdminUserByUsername(admin.username) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by username" in {
    db withDynSession {

      // managementDao.getAdminByUsername(adminWithoutOrg.username) should be(Some(adminWithoutOrg))
    }
  }

  it should "throw DuplicateIDEntity for duplicate admins" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {
        managementDao.saveAdminUser(admin)
      }
    }
  }

  it should "get user by id" in {
    db withDynSession {
      managementDao.getUserRef(user.id) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by id" in {
    db withDynSession {
      //managementDao.getUser(userWithoutPerm.id) should be(Some(userWithoutPerm))
    }
  }

  it should "get user by email" in {
    db withDynSession {
      managementDao.getUserRefByEmail(user.email) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by email" in {
    db withDynSession {
      //managementDao.getUserByEmail(userWithoutPerm.email) should be(Some(userWithoutPerm))
    }
  }

  it should "get user by username" in {
    db withDynSession {
      managementDao.getUserRefByUsername(user.username) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by username" in {
    db withDynSession {
      //managementDao.getUserByUsername(userWithoutPerm.username) should be(Some(userWithoutPerm))
    }
  }

  it should "throw DuplicateIDEntity for duplicate users" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {
        managementDao.saveUserRef(user)
      }
    }
  }
}
