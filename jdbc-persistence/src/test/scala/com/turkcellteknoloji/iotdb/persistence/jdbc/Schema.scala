package com.turkcellteknoloji.iotdb.persistence.jdbc

import com.fasterxml.uuid.Generators
import com.turkcellteknoloji.iotdb.domain
import com.turkcellteknoloji.iotdb.domain.User
import org.junit.runner.RunWith
import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.junit.JUnitRunner
import org.scalatest.matchers.ShouldMatchers
import scala.slick.driver.{ExtendedProfile, HsqldbDriver}
import scala.slick.session.Database

/**
 * Created by capacman on 10/26/13.
 */


trait HSQLInMemoryDB {
  val profile: ExtendedProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver")
}

@RunWith(classOf[JUnitRunner])
class SchemaTest extends FlatSpec with ShouldMatchers with BeforeAndAfterAll with MetadataComponent with Profile with HSQLInMemoryDB {

  import Database.threadLocalSession

  val sGenerator = Generators.randomBasedGenerator()
  val dbGenerator = Generators.randomBasedGenerator()
  val userGenerator = Generators.randomBasedGenerator()

  val tmpUser = User(userGenerator.generate(), "anil", "halil", "user1", "user@user.com")
  val databases = List(domain.Database(dbGenerator.generate(), "db1", tmpUser), domain.Database(dbGenerator.generate(), "db2", tmpUser))
  val series = List(
    domain.Series(sGenerator.generate(), "key1", Some("first series"), Set("tag1"), Map("attr1" -> "val1")),
    domain.Series(sGenerator.generate(), "key2", Some("second series"), Set("tag1","tag2", "tag3"), Map("attr2" -> "val2", "attr3" -> "val3"))
  )

  override def beforeAll(configMap: Map[String, Any]) {
    db withSession {
      create
      databases.foreach(saveDatabase)
      series.foreach(saveSeries(_, databases.head.id))
    }
  }

  override def afterAll(configMap: Map[String, Any]) {
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

  "schema " should " get series with key" in {
    db withSession {
      getSeriesByKey(series.head.key, databases.head.id) should be(Some(series.head))
      getSeriesByKey(series.head.key, databases(1).id) should be(None)
    }
  }

  "schema " should " get series by tags" in {
    db withSession {
      getSeriesByTags(Set("tag1"), databases.head.id) should have size (2)
      getSeriesByTags(Set("tag1","tag2"), databases.head.id) should have size (1)
      getSeriesByTags(Set("tag1","tag2","tag3"), databases.head.id) should have size (1)
    }
  }

  "schema " should " get series by attributes" in {
    db withSession {
      getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size (1)
      getSeriesByAttributes(Map("attr2" -> "val2"), databases.head.id) should have size (1)
      getSeriesByAttributes(Map("attr1" -> "val1","attr2" -> "val2"), databases.head.id) should have size (0)
    }
  }

  "schema " should " get series by intersection" in {
    db withSession {
      getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size (1)
      getSeriesIntersection(Set(),Set(),Set("tag1"),Map("attr1" -> "val1"),databases.head.id) should have size (1)
      getSeriesIntersection(Set(),Set(),Set("tag1"),Map(),databases.head.id) should have size (2)
      getSeriesIntersection(Set(),Set("key1"),Set("tag1"),Map(),databases.head.id) should have size (1)
      getSeriesIntersection(Set(),Set("key2"),Set("tag1"),Map(),databases.head.id) should have size (1)
      getSeriesIntersection(Set(),Set("key1"),Set("tag2"),Map(),databases.head.id) should have size (0)
    }
  }
}
