package io.swarm.infrastructure.persistence.slick

import scala.slick.jdbc.JdbcBackend
import io.swarm.infrastructure.persistence.{PersistenceSessionComponent, PersistenceSession}


trait SlickPersistenceSessionComponent extends PersistenceSessionComponent {
  this: JdbcBackend =>
  val db: Database
  val persistenceSession: PersistenceSession = new PersistenceSession {
    def withSession[T](body: => T): T = db withDynSession (body)

    def withTransaction[T](body: => T): T = db withDynTransaction (body)
  }
}