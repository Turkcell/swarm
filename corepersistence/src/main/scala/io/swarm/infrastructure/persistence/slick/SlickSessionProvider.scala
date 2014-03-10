package io.swarm.infrastructure.persistence.slick

import scala.slick.jdbc.JdbcBackend._

/**
 * Created by capacman on 10/26/13.
 */

trait SlickDbProvider {
  val db: Database
}
