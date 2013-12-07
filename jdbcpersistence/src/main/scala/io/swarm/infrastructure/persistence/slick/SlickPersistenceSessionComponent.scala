package io.swarm.infrastructure.persistence.slick

import scala.slick.jdbc.JdbcBackend.Database
import io.swarm.infrastructure.persistence.{PersistenceSessionComponent, PersistenceSession}


trait SlickPersistenceSessionComponent extends PersistenceSessionComponent {
  val db: Database
  val persistenceSession: PersistenceSession = new PersistenceSession {
    def withSession[T](body: => T): T = db withDynSession (body)

    def withTransaction[T](body: => T): T = db withDynTransaction (body)
  }
}