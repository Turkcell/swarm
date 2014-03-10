package io.swarm.security.shiro

import io.swarm.management.dao.ManagementDaoComponent
import scala.slick.driver.{HsqldbDriver, JdbcProfile}
import io.swarm.management.impl.ManagementDaoJDBC
import scala.slick.jdbc.JdbcBackend._
import io.swarm.infrastructure.persistence.slick.SlickDbProvider


trait HSQLInMemoryManagementDaoComponent extends ManagementDaoComponent with SlickDbProvider {
  val profile: JdbcProfile = HsqldbDriver
  val managementDao = new ManagementDaoJDBC(profile)
  lazy val db = Database.forURL("jdbc:hsqldb:mem:mymemdb", driver = "org.hsqldb.jdbc.JDBCDriver", user = "sa", password = "sa")
}
