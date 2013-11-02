package com.turkcellteknoloji.iotdb

import java.util.UUID
import com.fasterxml.uuid.impl.UUIDUtil
import org.apache.commons.codec.binary.Base64

/**
 * Created by capacman on 10/31/13.
 */
package object domain {
  val base64val = new Base64(true)

  final class RichUUID(val self: UUID) {

    def asString = self.toString.filter(_ != '-')

    def asByteArray = UUIDUtil.asByteArray(self)

    def base64 = base64val.encodeToString(asByteArray)
  }

  final class RichString(val self: String) {
    def base64 = Base64.encodeBase64URLSafeString(self.getBytes("UTF-8"))
  }

  implicit def UUID2RichUUID(uuid: UUID) = new RichUUID(uuid)

  implicit def String2RichString(s: String) = new RichString(s)
}
