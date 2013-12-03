package io.swarm.persistence.jdbc

import scala.slick.session.Database

/**
 * Created by Anil Chalil on 12/3/13.
 */
trait DatabaseProvider {
  val db:Database
}
