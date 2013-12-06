package io.swarm.persistence

import java.util.UUID

/**
 * Created by capacman on 10/26/13.
 */
package object slick {

  private[slick] final class RichUUID(val self: UUID) {
    def asString = {
      self.toString.filter(_ != '-')
    }
  }

  private[slick] implicit def UUID2RichUUID(uuid: UUID) = {
    new RichUUID(uuid)
  }

}
