package io.swarm
package security

import org.scalatest._
import io.swarm.UUIDGenerator

/**
 * Created by Anil Chalil on 12/4/13.
 */
class SecureTimeStampTests extends FlatSpec with ShouldMatchers {

  "secretGenerator" should "give timestamp in milliseconds" in {
    val timeStamp = UUIDGenerator.secretGenerator.generate().timeStampInMillis
    (System.currentTimeMillis()-timeStamp) should be < 20L
  }
}
