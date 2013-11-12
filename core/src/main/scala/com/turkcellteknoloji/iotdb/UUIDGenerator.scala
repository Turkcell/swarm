package com.turkcellteknoloji.iotdb

import com.sun.corba.se.spi.legacy.connection.GetEndPointInfoAgainException
import com.fasterxml.uuid.{EthernetAddress, Generators}
import java.security.SecureRandom
import java.util.Random

/**
 * Created by capacman on 11/11/13.
 */
object UUIDGenerator {
  val secretGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface())
}
