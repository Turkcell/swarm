package io.swarm.security.shiro

import io.swarm.infrastructure.persistence.slick.{SlickPersistenceSessionComponent, SlickProfileComponent}
import scala.slick.jdbc.JdbcBackend
import io.swarm.domain.persistence.slick.ClientResourceDaoComponent
import scala.slick.driver.{HsqldbDriver, JdbcProfile}

/**
 * Created by Anil Chalil on 11/19/13.
 */
trait HSQLInMemoryClientResourceDaoComponent extends SlickProfileComponent with JdbcBackend with ClientResourceDaoComponent {

  val profile: JdbcProfile = HsqldbDriver
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")
  val clientResourceDao = new ClientResourceDao {}

}
