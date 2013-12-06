package io.swarm.persistence.slick

import io.swarm.persistence.{PersistenceSessionComponent, PersistenceSession}
import scala.slick.jdbc.JdbcBackend


trait SlickPersistenceSessionComponent extends PersistenceSessionComponent {
  this: JdbcBackend =>
  val db: Database
  val persistenceSession: PersistenceSession = new PersistenceSession {
    def withSession[T](body: => T): T = db withDynSession (body)

    def withTransaction[T](body: => T): T = db withDynTransaction (body)
  }
}