package io.swarm.infrastructure.persistence

/**
 * Created by Anil Chalil on 12/3/13.
 */
trait PersistenceSession {
  def withSession[T](body: => T): T

  def withTransaction[T](body: => T): T
}
