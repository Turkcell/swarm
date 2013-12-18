package io.swarm.infrastructure.persistence

/**
 * Created by Anil Chalil on 12/7/13.
 */
trait OptimisticLockingException {

}

object OptimisticLockingException {
  def apply(message: String) = new RuntimeException(message) with OptimisticLockingException
}
