package io.swarm.persistence.jdbc

import com.fasterxml.uuid.Generators
import io.swarm.domain._
import org.junit.runner.RunWith
import org.scalatest.{ConfigMap, BeforeAndAfterAllConfigMap, ShouldMatchers, FlatSpec}
import org.scalatest.junit.JUnitRunner
import scala.slick.driver.{ExtendedProfile, HsqldbDriver}
import scala.slick.session.Database
import io.swarm.{UUIDGenerator, domain}
import io.swarm.security.HashedAlgorithm

/**
 * Created by capacman on 10/26/13.
 */

trait HSQLInMemoryDB {
  val profile: ExtendedProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver")
}

@RunWith(classOf[JUnitRunner])
class SchemaTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAllConfigMap with MetadataComponent with Profile with HSQLInMemoryDB {

  import Database.threadLocalSession

  val uuidGenerator = Generators.timeBasedGenerator()
  val databases = List(domain.Database(uuidGenerator.generate(), "db1", DatabaseMetadata(0)), domain.Database(uuidGenerator.generate(), "db2", DatabaseMetadata(0)))
  val organization = Organization(UUIDGenerator.secretGenerator.generate(), "testorg", databases.toSet)
  val tmpUser = AdminUser(UUIDGenerator.secretGenerator.generate(), "test", "test", "test", "test@test.com", HashedAlgorithm.toHex("test"), true, true, false, Set(organization))


  val series = List(
    domain.Series(uuidGenerator.generate(), "key1", Some("first series"), Set("tag1"), Map("attr1" -> "val1"), SeriesType.Long),
    domain.Series(uuidGenerator.generate(), "key2", Some("second series"), Set("tag1", "tag2", "tag3"), Map("attr2" -> "val2", "attr3" -> "val3"), SeriesType.Float))

  override def beforeAll(configMap: ConfigMap) {
    db withSession {
      create
      saveOrganization(organization)
      databases.foreach(saveDatabase(_, organization))
      series.foreach(saveSeries(_, databases.head.id))
    }
  }

  override def afterAll(configMap: ConfigMap) {
    db withSession {
      drop
    }
  }

  "schema " should " get series with id" in {
    db withSession {
      val sr1 = getSeries(series.head.id)
      val sr2 = getSeries(series(1).id)
      sr1 should be(Some(series.head))
      sr2 should be(Some(series(1)))
    }
  }

  it should " get series with key" in {
    db withSession {
      getSeriesByKey(series.head.key, databases.head.id) should be(Some(series.head))
      getSeriesByKey(series.head.key, databases(1).id) should be(None)
    }
  }

  it should " get series by tags" in {
    db withSession {
      getSeriesByTags(Set("tag1"), databases.head.id) should have size 2
      getSeriesByTags(Set("tag1", "tag2"), databases.head.id) should have size 1
      getSeriesByTags(Set("tag1", "tag2", "tag3"), databases.head.id) should have size 1
    }
  }

  it should " get series by attributes" in {
    db withSession {
      getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
      getSeriesByAttributes(Map("attr2" -> "val2"), databases.head.id) should have size 1
      getSeriesByAttributes(Map("attr1" -> "val1", "attr2" -> "val2"), databases.head.id) should have size 0
    }
  }

  it should " get series by intersection" in {
    db withSession {
      getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
      getSeriesIntersection(Set(), Set(), Set("tag1"), Map("attr1" -> "val1"), databases.head.id) should have size 1
      getSeriesIntersection(Set(), Set(), Set("tag1"), Map(), databases.head.id) should have size 2
      getSeriesIntersection(Set(), Set("key1"), Set("tag1"), Map(), databases.head.id) should have size 1
      getSeriesIntersection(Set(), Set("key2"), Set("tag1"), Map(), databases.head.id) should have size 1
      getSeriesIntersection(Set(), Set("key1"), Set("tag2"), Map(), databases.head.id) should have size 0
    }
  }

  it should "throw DublicateIDEntity for duplicate series" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        saveSeries(series.head, databases.head.id)
      }
    }
  }

  it should "get database by id" in {
    db withSession {
      getDatabase(databases.head.id) should be(Some(databases.head))
      getDatabase(UUIDGenerator.secretGenerator.generate()) should be(None)
    }
  }

  it should "throw DublicateIDEntity for duplicate database" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        saveDatabase(databases.head, organization)
      }
    }
  }

  it should "get organization by id" in {
    db withSession {
      getOrganizationByID(organization.id) should be(Some(organization))
      getOrganizationByID(UUIDGenerator.secretGenerator.generate()) should be(None)
    }
  }

  it should "throw DublicateIDEntity for duplicate organizations" in {
    intercept[DuplicateIDEntity] {
      db withSession {
        saveOrganization(organization)
      }
    }
  }
}
