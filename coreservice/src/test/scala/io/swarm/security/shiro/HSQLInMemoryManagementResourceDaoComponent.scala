package io.swarm.security.shiro

import io.swarm.infrastructure.persistence.slick.{SlickPersistenceSessionComponent, SlickProfileComponent}
import scala.slick.jdbc.JdbcBackend.Database
import scala.slick.driver.{HsqldbDriver, JdbcProfile}
import io.swarm.management.impl.ManagementResourceDaoComponent

/**
 * Created by Anil Chalil on 11/19/13.
 */
trait HSQLInMemoryManagementResourceDaoComponent extends SlickProfileComponent  with ManagementResourceDaoComponent {

  val profile: JdbcProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")
  val clientResourceDao = new ManagementResourceDao {}

}
