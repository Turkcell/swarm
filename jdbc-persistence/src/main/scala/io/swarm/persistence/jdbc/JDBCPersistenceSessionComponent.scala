package io.swarm.persistence.jdbc

import io.swarm.persistence.{PersistenceSessionComponent, PersistenceSession}


trait JDBCPersistenceSessionComponent extends PersistenceSessionComponent {
  this: DatabaseProvider =>
  val persistenceSession: PersistenceSession = new PersistenceSession {
    def withSession[T](body: => T): T = db withSession (body)

    def withTransaction[T](body: => T): T = db withTransaction (body)
  }
}