/**
 * Created by Anil Chalil on 1/3/14.
 */
class PersistenceTests {
  /*
    "schema " should " get series with id" in {
      db withDynSession {

        val sr1 = managementResourceDao.getSeries(series.head.id)
        val sr2 = managementResourceDao.getSeries(series(1).id)
        sr1 should be(Some(series.head))
        sr2 should be(Some(series(1)))
      }
    }

    it should " get series with key" in {
      db withDynSession {

        managementResourceDao.getSeriesByKey(series.head.key, databases.head.id) should be(Some(series.head))
        managementResourceDao.getSeriesByKey(series.head.key, databases(1).id) should be(None)
      }
    }

    it should " get series by tags" in {
      db withDynSession {

        managementResourceDao.getSeriesByTags(Set("tag1"), databases.head.id) should have size 2
        managementResourceDao.getSeriesByTags(Set("tag1", "tag2"), databases.head.id) should have size 1
        managementResourceDao.getSeriesByTags(Set("tag1", "tag2", "tag3"), databases.head.id) should have size 1
      }
    }

    it should " get series by attributes" in {
      db withDynSession {

        managementResourceDao.getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
        managementResourceDao.getSeriesByAttributes(Map("attr2" -> "val2"), databases.head.id) should have size 1
        managementResourceDao.getSeriesByAttributes(Map("attr1" -> "val1", "attr2" -> "val2"), databases.head.id) should have size 0
      }
    }

    it should " get series by intersection" in {
      db withDynSession {

        managementResourceDao.getSeriesByAttributes(Map("attr1" -> "val1"), databases.head.id) should have size 1
        managementResourceDao.getSeriesIntersection(Set(), Set(), Set("tag1"), Map("attr1" -> "val1"), databases.head.id) should have size 1
        managementResourceDao.getSeriesIntersection(Set(), Set(), Set("tag1"), Map(), databases.head.id) should have size 2
        managementResourceDao.getSeriesIntersection(Set(), Set("key1"), Set("tag1"), Map(), databases.head.id) should have size 1
        managementResourceDao.getSeriesIntersection(Set(), Set("key2"), Set("tag1"), Map(), databases.head.id) should have size 1
        managementResourceDao.getSeriesIntersection(Set(), Set("key1"), Set("tag2"), Map(), databases.head.id) should have size 0
      }
    }

    it should "throw DuplicateIDEntity for duplicate series" in {
      intercept[DuplicateIDEntity] {
        db withDynSession {

          managementResourceDao.saveSeries(series.head, databases.head.id)
        }
      }
    }

    it should "get database by id" in {
      db withDynSession {

        managementResourceDao.getDatabase(databases.head.id) should be(Some(databases.head))
        managementResourceDao.getDatabase(UUIDGenerator.randomGenerator.generate()) should be(None)
      }
    }

    it should "throw DuplicateIDEntity for duplicate database" in {
      intercept[DuplicateIDEntity] {
        db withDynSession {

          managementResourceDao.saveDatabase(databases.head, organization.id)
        }
      }
    }
  */
}
