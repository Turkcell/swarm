package io.swarm.persistence.slick

import io.swarm.domain._
import org.junit.runner.RunWith
import org.scalatest.{ConfigMap, BeforeAndAfterAllConfigMap, ShouldMatchers, FlatSpec}
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.{JdbcProfile, HsqldbDriver}
import io.swarm.{UUIDGenerator, domain}
import io.swarm.security.HashedAlgorithm
import io.swarm.domain.persistence.slick.ClientResourceDaoComponent
import scala.slick.backend.DatabaseComponent
import scala.slick.jdbc.JdbcBackend

/**
 * Created by capacman on 10/26/13.
 */

trait HSQLInMemoryClientResourceDaoComponent extends SlickProfileComponent with JdbcBackend with ClientResourceDaoComponent {

  val profile: JdbcProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")
  val clientResourceDao = new ClientResourceDao {}

}

@RunWith(classOf[JUnitRunner])
class SchemaTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAllConfigMap with ClientResourceDaoComponent with HSQLInMemoryClientResourceDaoComponent {


  val databases = List(domain.Database(UUIDGenerator.randomGenerator.generate(), "db1", DatabaseMetadata(0), 0), domain.Database(UUIDGenerator.randomGenerator.generate(), "db2", DatabaseMetadata(0), 0))
  val organization = Organization(UUIDGenerator.randomGenerator.generate(), "testorg", databases.toSet, 0)
  val tmpUser = AdminUser(UUIDGenerator.randomGenerator.generate(), Some("test"), Some("test"), "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set(organization), 0)
  val devices = List(Device(UUIDGenerator.randomGenerator.generate(), "device1", databases.head.databaseInfo, activated = true, disabled = false, Set("perm1", "perm2"), 0), Device(UUIDGenerator.randomGenerator.generate(), "device2", databases.head.databaseInfo, activated = true, disabled = false, Set("perm1", "perm2"), 0))


  val series = List(
    domain.Series(UUIDGenerator.randomGenerator.generate(), "key1", Some("first series"), Set("tag1"), Map("attr1" -> "val1"), SeriesType.Long),
    domain.Series(UUIDGenerator.randomGenerator.generate(), "key2", Some("second series"), Set("tag1", "tag2", "tag3"), Map("attr2" -> "val2", "attr3" -> "val3"), SeriesType.Float))

  override def beforeAll(configMap: ConfigMap) {
    db withSession {
      implicit session: Session =>
        clientResourceDao.create
        clientResourceDao.saveOrganization(organization)
        databases.foreach(clientResourceDao.saveDatabase(_, organization.id))
        series.foreach(clientResourceDao.saveSeries(_, databases.head.id))
        devices.foreach(clientResourceDao.saveDevice)
        clientResourceDao.saveAdminUser(tmpUser)
        clientResourceDao.addAdminToOrganization(tmpUser.id,organization.id)
    }
  }

  override def afterAll(configMap: ConfigMap) {
    db withSession {
      implicit session: Session =>
        clientResourceDao.drop
    }
  }

  "schema " should " get series with id" in {
    db withSession {
      implicit session: Session =>
        val sr1 = clientResourceDao.getSeries(series.head.id)
        val sr2 = clientResourceDao.getSeries(series(1).id)
        sr1 should be(Some(series.head))
        sr2 should be(Some(series(1)))
    }
  }

  it should " get series with key" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getSeriesByKey(series.head.key, databases.head.id) should be(Some(series.head))
        clientResourceDao.getSeriesByKey(series.head.key, databases(1).id) should be(None)
    }
  }

  it should " get series by tags" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getSeriesByTags(Set("tag1"), databases.head.id) should have size 2
        clientResourceDao.getSeriesByTags(Set("tag1", "tag2"), databases.head.id) should have size 1
        clientResourceDao.getSeriesByTags(Set("tag1", "tag2", "tag3"), databases.head.id) should have size 1
    }
  }

  it should " get series by attributes" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
        clientResourceDao.getSeriesByAttributes(Map("attr2" -> "val2"), databases.head.id) should have size 1
        clientResourceDao.getSeriesByAttributes(Map("attr1" -> "val1", "attr2" -> "val2"), databases.head.id) should have size 0
    }
  }

  it should " get series by intersection" in {
    db withSession {
      implicit session: Session =>
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
      db withSession {
        implicit session: Session =>
          clientResourceDao.saveSeries(series.head, databases.head.id)
      }
    }
  }

  it should "get database by id" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getDatabase(databases.head.id) should be(Some(databases.head))
        clientResourceDao.getDatabase(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "throw DuplicateIDEntity for duplicate database" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session: Session =>
          clientResourceDao.saveDatabase(databases.head, organization.id)
      }
    }
  }

  it should "get organization by id" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getOrganizationByID(organization.id) should be(Some(organization))
        clientResourceDao.getOrganizationByID(UUIDGenerator.randomGenerator.generate()) should be(None)
    }
  }

  it should "throw DuplicateIDEntity for duplicate organizations" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session: Session =>
          clientResourceDao.saveOrganization(organization)
      }
    }
  }

  it should "get device by id " in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getDeviceByID(devices.head.id) should be(Some(devices.head))
    }
  }

  it should "get device by deviceId " in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getDeviceByDeviceID(devices.head.deviceID) should be(Some(devices.head))
    }
  }

  it should "throw DuplicateIDEntity for duplicate devices" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session: Session =>
          clientResourceDao.saveDevice(devices.head)
      }
    }
  }

  it should "get admin by id" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getAdminByID(tmpUser.id) should be(Some(tmpUser))
    }
  }

  it should "get admin by email" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getAdminByEmail(tmpUser.email) should be(Some(tmpUser))
    }
  }

  it should "get admin by username" in {
    db withSession {
      implicit session: Session =>
        clientResourceDao.getAdminByUsername(tmpUser.username) should be(Some(tmpUser))
    }
  }

  it should "throw DuplicateIDEntity for duplicate admins" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        implicit session: Session =>
          clientResourceDao.saveAdminUser(tmpUser)
      }
    }
  }
}
