package com.turkcellteknoloji.iotdb

import java.util.UUID
import com.fasterxml.uuid.impl.UUIDUtil
import org.apache.commons.codec.binary.Base64
import org.apache.commons.codec.digest.DigestUtils
import java.nio.ByteBuffer

/**
 * Created by capacman on 10/31/13.
 */
package object domain {
  val base64val = new Base64(true)

  object RichUUID {
    val kClockOffset = 0x01b21dd213814000L
    val kClockMultiplierL = 10000L
  }

  final class RichUUID(val self: UUID) {

    def asString = self.toString.filter(_ != '-')

    def asByteArray = UUIDUtil.asByteArray(self)

    def base64 = base64val.encodeToString(asByteArray)

    def timeStampInMillis = if (self == null) 0L else (self.timestamp() - RichUUID.kClockOffset) / RichUUID.kClockMultiplierL

  }

  final class RichString(val self: String) {
    def base64 = Base64.encodeBase64URLSafeString(self.getBytes("UTF-8"))

    def sha = DigestUtils.sha(self)

    def decodeBase64 = Base64.decodeBase64(self)
  }

  final class RichByteBuffer(val self: ByteBuffer) {
    def base64URLSafeString = Base64.encodeBase64URLSafeString(self.array())
  }

  implicit def byteBuffer2RichByteBuffer(byteBuffer: ByteBuffer) = new RichByteBuffer(byteBuffer)

  implicit def UUID2RichUUID(uuid: UUID) = new RichUUID(uuid)

  implicit def String2RichString(s: String) = new RichString(s)
}
