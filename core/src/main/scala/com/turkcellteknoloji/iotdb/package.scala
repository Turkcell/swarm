/*
 * Copyright 2013 Turkcell Teknoloji Inc. and individual
 * contributors by the 'Created by' comments.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.turkcellteknoloji

import org.apache.commons.codec.binary.Base64
import java.util.UUID
import com.fasterxml.uuid.impl.UUIDUtil
import org.apache.commons.codec.digest.DigestUtils
import java.nio.ByteBuffer

/**
 * Created by Anil Chalil
 */
package object iotdb {
  val base64val = new Base64(true)

  // https://issues.scala-lang.org/browse/SI-5954 should be in companion RichUUID
  val kClockOffset = 0x01b21dd213814000L
  val kClockMultiplierL = 10000L


  final class RichUUID(val self: UUID) {

    def asString = self.toString.filter(_ != '-')

    def asByteArray = UUIDUtil.asByteArray(self)

    def base64 = base64val.encodeToString(asByteArray)

    def timeStampInMillis = if (self == null) 0L else (self.timestamp() - kClockOffset) / kClockMultiplierL

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
