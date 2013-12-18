package io.swarm.infrastructure.persistence

import io.swarm.domain._
import org.junit.runner.RunWith
import org.scalatest.{ConfigMap, BeforeAndAfterAllConfigMap, ShouldMatchers, FlatSpec}
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.{JdbcProfile, HsqldbDriver}
import io.swarm.{UUIDGenerator, domain}
import io.swarm.security.HashedAlgorithm
import io.swarm.domain.persistence.slick.ClientResourceDaoComponent
import io.swarm.infrastructure.persistence.slick.SlickProfileComponent
import scala.slick.jdbc.JdbcBackend.Database

/**
 * Created by capacman on 10/26/13.
 */

trait HSQLInMemoryClientResourceDaoComponent extends SlickProfileComponent with ClientResourceDaoComponent {

  val profile: JdbcProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")
  val clientResourceDao = new ClientResourceDao {}

}

@RunWith(classOf[JUnitRunner])
class SchemaTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAllConfigMap  with HSQLInMemoryClientResourceDaoComponent {


  val databases = List(domain.Database(UUIDGenerator.randomGenerator.generate(), "db1", DatabaseMetadata(0), 0), domain.Database(UUIDGenerator.randomGenerator.generate(), "db2", DatabaseMetadata(0), 0))
  val organization = Organization(UUIDGenerator.randomGenerator.generate(), "testorg", databases.toSet, 0)
  val organizationWithoutDB = Organization(UUIDGenerator.randomGenerator.generate(), "withoutdb", Set(), 0)
  val admin = AdminUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set(organization), 0)
  val adminWithoutOrg = AdminUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "adminWithoutOrg", "adminWithoutOrg@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set(), 0)
  val devices = List(Device(UUIDGenerator.randomGenerator.generate(), "device1", databases.head.databaseInfo, activated = true, disabled = false, Set("perm1", "perm2"), 0), Device(UUIDGenerator.randomGenerator.generate(), "device2", databases.head.databaseInfo, activated = true, disabled = false, Set("perm1", "perm2"), 0))
  val deviceWithoutPerm = Device(UUIDGenerator.randomGenerator.generate(), "device3", databases.head.databaseInfo, activated = true, disabled = false, Set(), 0)
  val user = DatabaseUser(UUIDGenerator.randomGenerator.generate(), Some("user"), Some("user"), "user", "user@user.com", HashedAlgorithm.toHex("test"), true, true, false, Set("perm1", "perm2"), 0)
  val userWithoutPerm = DatabaseUser(UUIDGenerator.randomGenerator.generate(), Some("user"), Some("user"), "user2", "user2@user.com", HashedAlgorithm.toHex("test"), true, true, false, Set(), 0)

  val series = List(
    domain.Series(UUIDGenerator.randomGenerator.generate(), "key1", Some("first series"), Set("tag1"), Map("attr1" -> "val1"), SeriesType.Long),
    domain.Series(UUIDGenerator.randomGenerator.generate(), "key2", Some("second series"), Set("tag1", "tag2", "tag3"), Map("attr2" -> "val2", "attr3" -> "val3"), SeriesType.Float))

  override def beforeAll(configMap: ConfigMap) {
    db withDynSession {
      clientResourceDao.create
      clientResourceDao.saveOrganization(organization)
      clientResourceDao.saveOrganization(organizationWithoutDB)
      databases.foreach(clientResourceDao.saveDatabase(_, organization.id))
      series.foreach(clientResourceDao.saveSeries(_, databases.head.id))
      devices.foreach(clientResourceDao.saveDevice)
      clientResourceDao.saveDevice(deviceWithoutPerm)
      clientResourceDao.saveAdminUser(admin)
      clientResourceDao.addAdminToOrganization(admin.id, organization.id)
      clientResourceDao.saveAdminUser(adminWithoutOrg)
      clientResourceDao.saveUser(user)
      clientResourceDao.saveUser(userWithoutPerm)
    }
  }

  override def afterAll(configMap: ConfigMap) {
    db withDynSession {

      clientResourceDao.drop
    }
  }

  "schema " should " get series with id" in {
    db withDynSession {

      val sr1 = clientResourceDao.getSeries(series.head.id)
      val sr2 = clientResourceDao.getSeries(series(1).id)
      sr1 should be(Some(series.head))
      sr2 should be(Some(series(1)))
    }
  }

  it should " get series with key" in {
    db withDynSession {

      clientResourceDao.getSeriesByKey(series.head.key, databases.head.id) should be(Some(series.head))
      clientResourceDao.getSeriesByKey(series.head.key, databases(1).id) should be(None)
    }
  }

  it should " get series by tags" in {
    db withDynSession {

      clientResourceDao.getSeriesByTags(Set("tag1"), databases.head.id) should have size 2
      clientResourceDao.getSeriesByTags(Set("tag1", "tag2"), databases.head.id) should have size 1
      clientResourceDao.getSeriesByTags(Set("tag1", "tag2", "tag3"), databases.head.id) should have size 1
    }
  }

  it should " get series by attributes" in {
    db withDynSession {

      clientResourceDao.getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
      clientResourceDao.getSeriesByAttributes(Map("attr2" -> "val2"), databases.head.id) should have size 1
      clientResourceDao.getSeriesByAttributes(Map("attr1" -> "val1", "attr2" -> "val2"), databases.head.id) should have size 0
    }
  }

  it should " get series by intersection" in {
    db withDynSession {

      clientResourceDao.getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
      clientResourceDao.getSeriesIntersection(Set(), Set(), Set("tag1"), Map("attr1" -> "val1"), databases.head.id) should have size 1
      clientResourceDao.getSeriesIntersection(Set(), Set(), Set("tag1"), Map(), databases.head.id) should have size 2
      clientResourceDao.getSeriesIntersection(Set(), Set("key1"), Set("tag1"), Map(), databases.head.id) should have size 1
      clientResourceDao.getSeriesIntersection(Set(), Set("key2"), Set("tag1"), Map(), databases.head.id) should have size 1
      clientResourceDao.getSeriesIntersection(Set(), Set("key1"), Set("tag2"), Map(), databases.head.id) should have size 0
    }
  }

  it should "throw DuplicateIDEntity for duplicate series" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {

        clientResourceDao.saveSeries(series.head, databases.head.id)
      }
    }
  }

  it should "get database by id" in {
    db withDynSession {

      clientResourceDao.getDatabase(databases.head.id) should be(Some(databases.head))
      clientResourceDao.getDatabase(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "throw DuplicateIDEntity for duplicate database" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {

        clientResourceDao.saveDatabase(databases.head, organization.id)
      }
    }
  }

  it should "get organization by id" in {
    db withDynSession {

      clientResourceDao.getOrganizationByID(organization.id) should be(Some(organization))
      clientResourceDao.getOrganizationByID(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "get organization without db by id" in {
    db withDynSession {

      clientResourceDao.getOrganizationByID(organizationWithoutDB.id) should be(Some(organizationWithoutDB))
    }
  }

  it should "throw DuplicateIDEntity for duplicate organizations" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {

        clientResourceDao.saveOrganization(organization)
      }
    }
  }

  it should "get device by id " in {
    db withDynSession {

      clientResourceDao.getDeviceByID(devices.head.id) should be(Some(devices.head))
    }
  }

  it should "get device withoutperm by id " in {
    db withDynSession {

      clientResourceDao.getDeviceByID(deviceWithoutPerm.id) should be(Some(deviceWithoutPerm))
    }
  }

  it should "get device by deviceId " in {
    db withDynSession {

      clientResourceDao.getDeviceByDeviceID(devices.head.deviceID) should be(Some(devices.head))
    }
  }

  it should "get device withoutperm by deviceId " in {
    db withDynSession {

      clientResourceDao.getDeviceByDeviceID(deviceWithoutPerm.deviceID) should be(Some(deviceWithoutPerm))
    }
  }

  it should "throw DuplicateIDEntity for duplicate devices" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {

        clientResourceDao.saveDevice(devices.head)
      }
    }
  }

  it should "update device by id " in {
    db withDynSession {
      val dev = devices.head.copy(activated = false, permissions = Set("newPerm"), disabled = true, deviceID = "new deviceID")
      Some(clientResourceDao.updateDevice(dev)) should be(clientResourceDao.getDeviceByID(dev.id))
    }
  }

  it should "get admin by id" in {
    db withDynSession {

      clientResourceDao.getAdminByID(admin.id) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by id" in {
    db withDynSession {

      clientResourceDao.getAdminByID(adminWithoutOrg.id) should be(Some(adminWithoutOrg))
    }
  }

  it should "get admin by email" in {
    db withDynSession {

      clientResourceDao.getAdminByEmail(admin.email) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by email" in {
    db withDynSession {

      clientResourceDao.getAdminByEmail(adminWithoutOrg.email) should be(Some(adminWithoutOrg))
    }
  }

  it should "get admin by username" in {
    db withDynSession {

      clientResourceDao.getAdminByUsername(admin.username) should be(Some(admin))
    }
  }

  it should "get adminwithoutorg by username" in {
    db withDynSession {

      clientResourceDao.getAdminByUsername(adminWithoutOrg.username) should be(Some(adminWithoutOrg))
    }
  }

  it should "throw DuplicateIDEntity for duplicate admins" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {
        clientResourceDao.saveAdminUser(admin)
      }
    }
  }

  it should "get user by id" in {
    db withDynSession {
      clientResourceDao.getUserByID(user.id) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by id" in {
    db withDynSession {
      clientResourceDao.getUserByID(userWithoutPerm.id) should be(Some(userWithoutPerm))
    }
  }

  it should "get user by email" in {
    db withDynSession {
      clientResourceDao.getUserByEmail(user.email) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by email" in {
    db withDynSession {
      clientResourceDao.getUserByEmail(userWithoutPerm.email) should be(Some(userWithoutPerm))
    }
  }

  it should "get user by username" in {
    db withDynSession {
      clientResourceDao.getUserByUsername(user.username) should be(Some(user))
    }
  }

  it should "get userWithoutPerm by username" in {
    db withDynSession {
      clientResourceDao.getUserByUsername(userWithoutPerm.username) should be(Some(userWithoutPerm))
    }
  }

  it should "throw DuplicateIDEntity for duplicate users" in {
    intercept[DuplicateIDEntity] {
      db withDynSession {
        clientResourceDao.saveUser(user)
      }
    }
  }
}
