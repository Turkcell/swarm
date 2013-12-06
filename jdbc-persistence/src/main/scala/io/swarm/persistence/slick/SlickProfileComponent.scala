package io.swarm.persistence.slick

import scala.slick.driver.JdbcProfile

/**
 * Created by capacman on 10/25/13.
 */

trait SlickProfileComponent {
  val profile: JdbcProfile
}
